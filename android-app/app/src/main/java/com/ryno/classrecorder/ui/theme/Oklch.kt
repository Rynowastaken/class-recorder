package com.ryno.classrecorder.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * OKLCH color math (Björn Ottosson 2020), ported from DSH Go's oklch.dart.
 *
 * Keeping lightness [l] fixed while shifting hue [h] and scaling chroma [c]
 * preserves perceived brightness and WCAG contrast across all accent presets.
 */
@Immutable
data class Oklch(
    val l: Double,
    val c: Double,
    val h: Double
) {
    fun toColor(alpha: Float = 1f): Color {
        val clampedL = l.coerceIn(0.0, 1.0)
        var curC = max(0.0, c)
        var rgb = linearRgb(clampedL, curC, h)
        if (!inGamut(rgb)) {
            var lo = 0.0
            var hi = curC
            repeat(24) {
                val mid = (lo + hi) * 0.5
                if (inGamut(linearRgb(clampedL, mid, h))) {
                    lo = mid
                } else {
                    hi = mid
                }
            }
            curC = lo
            rgb = linearRgb(clampedL, curC, h)
        }
        fun channel(v: Double): Int = (toGamma(v.coerceIn(0.0, 1.0)) * 255.0).roundToInt().coerceIn(0, 255)
        val aInt = (alpha.coerceIn(0f, 1f) * 255f).roundToInt().coerceIn(0, 255)
        return Color(
            red = channel(rgb.first),
            green = channel(rgb.second),
            blue = channel(rgb.third),
            alpha = aInt
        )
    }

    companion object {
        fun fromColor(color: Color): Oklch {
            val r = toLinear(color.red.toDouble())
            val g = toLinear(color.green.toDouble())
            val b = toLinear(color.blue.toDouble())
            val lp = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
            val mp = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
            val sp = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
            val l = 0.2104542553 * lp + 0.7936177850 * mp - 0.0040720468 * sp
            val a = 1.9779984951 * lp - 2.4285922050 * mp + 0.4505937099 * sp
            val bb = 0.0259040371 * lp + 0.7827717662 * mp - 0.8086757660 * sp
            val c = sqrt(a * a + bb * bb)
            val rawH = atan2(bb, a) * 180.0 / Math.PI
            val h = ((rawH % 360.0) + 360.0) % 360.0
            return Oklch(l, c, h)
        }

        private fun linearRgb(l: Double, c: Double, h: Double): Triple<Double, Double, Double> {
            val rad = h * Math.PI / 180.0
            val a = c * cos(rad)
            val b = c * sin(rad)
            val lp = l + 0.3963377774 * a + 0.2158037573 * b
            val mp = l - 0.1055613458 * a - 0.0638541728 * b
            val sp = l - 0.0894841775 * a - 1.2914855480 * b
            val l3 = lp * lp * lp
            val m3 = mp * mp * mp
            val s3 = sp * sp * sp
            return Triple(
                4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
                -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
                -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3
            )
        }

        private fun inGamut(rgb: Triple<Double, Double, Double>): Boolean {
            val eps = 1e-4
            fun ok(v: Double) = v >= -eps && v <= 1.0 + eps
            return ok(rgb.first) && ok(rgb.second) && ok(rgb.third)
        }

        private fun cbrt(v: Double): Double =
            if (v < 0.0) -(-v).pow(1.0 / 3.0) else v.pow(1.0 / 3.0)

        internal fun toLinear(v: Double): Double =
            if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)

        private fun toGamma(v: Double): Double =
            if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
    }
}

/** Shifts [color] by [hueShift] degrees and multiplies chroma by [chromaScale], preserving lightness and alpha. */
fun retint(color: Color, hueShift: Double, chromaScale: Double): Color {
    if (hueShift == 0.0 && chromaScale == 1.0) return color
    val o = Oklch.fromColor(color)
    val nextHue = (((o.h + hueShift) % 360.0) + 360.0) % 360.0
    return o.copy(c = o.c * chromaScale, h = nextHue).toColor(alpha = color.alpha)
}

/** Relative sRGB luminance (WCAG 2.x), pure math so JVM unit tests need no Android platform stubs. */
fun relativeLuminance(color: Color): Double {
    val r = Oklch.toLinear(color.red.toDouble())
    val g = Oklch.toLinear(color.green.toDouble())
    val b = Oklch.toLinear(color.blue.toDouble())
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** WCAG 2.x contrast ratio between [a] and [b]. */
fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val hi = max(la, lb)
    val lo = min(la, lb)
    return (hi + 0.05) / (lo + 0.05)
}
