package com.ryno.classrecorder.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream

/**
 * The user's custom background image.
 *
 * Picking a picture hands back a one-shot `content://` URI that can be revoked
 * as soon as the picker process goes away, so the image is COPIED into
 * `filesDir/background/` immediately and the app only ever loads its own file.
 *
 * The copy is downscaled and re-encoded once, at pick time, for two reasons:
 * a 12 MP camera photo would be a 48 MB bitmap on the heap every frame, and
 * decoding a fresh large JPEG on each launch is a visible stall behind the
 * splash. 1440px on the long edge is above any phone width this app targets.
 */
object CustomBackground {
    private const val DIR = "background"
    private const val FILE = "custom_background.jpg"
    private const val MAX_EDGE = 1440
    private const val JPEG_QUALITY = 88

    private fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    private fun file(context: Context): File = File(dir(context), FILE)

    /**
     * Copy [uri] into app storage, replacing any existing background.
     * Returns the stored path, or null when the image could not be decoded.
     */
    fun save(context: Context, uri: Uri): String? {
        val source = context.contentResolver.openInputStream(uri) ?: return null
        // Decode bounds first so a huge image is never fully allocated.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        source.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var scale = 1
        while (bounds.outWidth / scale > MAX_EDGE || bounds.outHeight / scale > MAX_EDGE) {
            scale *= 2
        }

        val target = context.contentResolver.openInputStream(uri) ?: return null
        val decoded = target.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply { inSampleSize = scale }
            )
        } ?: return null

        // Honour the EXIF orientation. A photo taken in portrait is stored
        // landscape with an orientation tag; ignoring it draws the user's
        // wallpaper on its side.
        val oriented = applyExifOrientation(context, uri, decoded)
        if (oriented !== decoded) decoded.recycle()

        val out = file(context)
        return try {
            FileOutputStream(out).use { stream ->
                oriented.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
            }
            oriented.recycle()
            out.absolutePath
        } catch (t: Throwable) {
            oriented.recycle()
            null
        }
    }

    private fun applyExifOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching 0
            stream.use {
                when (ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        }.getOrDefault(0)
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * The stored image, already blurred, or null when none is set.
     *
     * The blur is baked in HERE rather than applied per-frame with a RenderEffect.
     * Two reasons: it costs nothing to draw afterwards (a blurred wallpaper is
     * sampled every frame, by the backdrop and again by every glass card), and it
     * survives being composited into the recorded GraphicsLayer that the glass
     * samples — a renderEffect on a sibling layer does not end up inside that
     * layer, which is why the wallpaper still looked sharp.
     *
     * The blur is a stack blur done once at load: roughly dsh-Go's 24dp sigma,
     * scaled to the stored image so it looks the same on any density.
     */
    fun load(context: Context, blurStrength: Float): ImageBitmap? {
        val path = AppearancePreferences.loadBackgroundPath(context) ?: return null
        val bytes = runCatching { File(path).readBytes() }.getOrNull() ?: return null
        val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
            .getOrNull() ?: return null
        val radius = radiusFor(blurStrength, bitmap.width, bitmap.height)
        val blurred = if (radius > 0) {
            runCatching { blur(bitmap, radius) }.getOrNull() ?: bitmap
        } else {
            bitmap
        }
        if (blurred !== bitmap) bitmap.recycle()
        return blurred.asImageBitmap()
    }

    /**
     * Convert a 0f..1f strength into a pixel radius for THIS image.
     *
     * Scaling by the image's long edge is what makes the slider feel consistent
     * across photos: a 600px image and a 1440px image at the same strength look
     * equally frosted, instead of the small one turning to mush.
     */
    fun radiusFor(strength: Float, width: Int, height: Int): Int {
        val s = strength.coerceIn(0f, 1f)
        if (s <= 0.01f) return 0
        val longEdge = maxOf(width, height).coerceAtLeast(1)
        // 0.03 of the long edge at full strength: ~43px on a 1440px image,
        // which is a heavy frost, and nothing at all at zero.
        return (longEdge * 0.03f * s).toInt().coerceAtLeast(1)
    }

    // The radius now comes from radiusFor(), driven by the user's slider.

    /**
     * A box blur, three passes, which approximates a gaussian closely enough for
     * a background wash and runs in O(n) per pass. RenderScript is deprecated and
     * android.graphics has no public blur, so this is the portable option.
     */
    private fun blur(source: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return source
        var current = source.copy(Bitmap.Config.ARGB_8888, true) ?: return source
        current = boxBlurPass(current, radius)
        return current
    }

    private fun boxBlurPass(input: Bitmap, radius: Int): Bitmap {
        val w = input.width
        val h = input.height
        val pixels = IntArray(w * h)
        input.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        val window = radius * 2 + 1

        // Horizontal pass
        val row = IntArray(w)
        for (y in 0 until h) {
            val base = y * w
            var rSum = 0; var gSum = 0; var bSum = 0
            for (i in -radius..radius) {
                val p = pixels[base + i.coerceIn(0, w - 1)]
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (x in 0 until w) {
                row[x] = ((rSum / window) shl 16) or ((gSum / window) shl 8) or (bSum / window) or -0x1000000
                val add = pixels[base + (x + radius + 1).coerceIn(0, w - 1)]
                val sub = pixels[base + (x - radius).coerceIn(0, w - 1)]
                rSum += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                gSum += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                bSum += (add and 0xFF) - (sub and 0xFF)
            }
            System.arraycopy(row, 0, out, base, w)
        }

        // Vertical pass
        val col = IntArray(h)
        for (x in 0 until w) {
            var rSum = 0; var gSum = 0; var bSum = 0
            for (i in -radius..radius) {
                val p = out[i.coerceIn(0, h - 1) * w + x]
                rSum += (p shr 16) and 0xFF
                gSum += (p shr 8) and 0xFF
                bSum += p and 0xFF
            }
            for (y in 0 until h) {
                col[y] = ((rSum / window) shl 16) or ((gSum / window) shl 8) or (bSum / window) or -0x1000000
                val add = out[(y + radius + 1).coerceIn(0, h - 1) * w + x]
                val sub = out[(y - radius).coerceIn(0, h - 1) * w + x]
                rSum += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                gSum += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                bSum += (add and 0xFF) - (sub and 0xFF)
            }
            for (y in 0 until h) {
                pixels[y * w + x] = col[y]
            }
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, w, 0, 0, w, h)
        input.recycle()
        return result
    }

    /** Delete the stored image and clear the preference. */
    fun clear(context: Context) {
        runCatching { file(context).delete() }
        AppearancePreferences.saveBackgroundPath(context, null)
    }
}
