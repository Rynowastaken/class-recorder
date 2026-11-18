package com.ryno.classrecorder.ui.screens

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Decodes real PCM amplitude peak buckets from an `.m4a` / audio file on disk
 * using Android's [MediaExtractor] + [MediaCodec] off the main thread, with an
 * in-memory [LruCache] so reopening recent recordings renders in 0 ms.
 */
object AudioWaveformDecoder {

    private val peakCache = object : LruCache<String, List<Float>>(24) {}

    suspend fun extractPeaks(
        filePath: String,
        bucketCount: Int = 72
    ): List<Float> = withContext(Dispatchers.Default) {
        if (bucketCount <= 0) return@withContext emptyList()
        val file = File(filePath)
        val length = file.length()
        if (!file.exists() || length <= 0L) return@withContext emptyList()

        val cacheKey = "${file.absolutePath}:${file.lastModified()}:$length:$bucketCount"
        synchronized(peakCache) {
            peakCache.get(cacheKey)?.let { return@withContext it }
        }

        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        val decoded = try {
            extractor.setDataSource(file.absolutePath)
            var audioTrack = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val tf = extractor.getTrackFormat(i)
                val mime = tf.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    format = tf
                    break
                }
            }
            if (audioTrack < 0 || format == null) return@withContext emptyList()
            extractor.selectTrack(audioTrack)

            val mime = format.getString(MediaFormat.KEY_MIME) ?: return@withContext emptyList()
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
            } else {
                0L
            }

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val buckets = FloatArray(bucketCount)
            val info = MediaCodec.BufferInfo()
            val useWindowSeek = durationUs > 30_000_000L

            if (useWindowSeek) {
                for (b in 0 until bucketCount) {
                    val targetUs = (durationUs * b.toLong()) / bucketCount.toLong()
                    extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    codec.flush()
                    buckets[b] = decodeWindowPeak(extractor, codec, info, maxFrames = 6)
                }
            } else {
                var inputDone = false
                var outputDone = false
                var safetyLoops = 0
                while (!outputDone && safetyLoops++ < 4000) {
                    if (!inputDone) {
                        val inIdx = codec.dequeueInputBuffer(4_000L)
                        if (inIdx >= 0) {
                            val inBuf = codec.getInputBuffer(inIdx)
                            val read = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                            if (read < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                val pts = extractor.sampleTime.coerceAtLeast(0L)
                                codec.queueInputBuffer(inIdx, 0, read, pts, 0)
                                extractor.advance()
                            }
                        }
                    }

                    val outIdx = codec.dequeueOutputBuffer(info, 4_000L)
                    if (outIdx >= 0) {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if (outBuf != null && info.size >= 2) {
                            val peak = measurePcm16Peak(outBuf, info.offset, info.size)
                            val bucket = if (durationUs > 0L) {
                                ((info.presentationTimeUs.coerceIn(0L, durationUs) * bucketCount) / (durationUs + 1L))
                                    .toInt()
                                    .coerceIn(0, bucketCount - 1)
                            } else {
                                safetyLoops % bucketCount
                            }
                            buckets[bucket] = max(buckets[bucket], peak)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }

            val maxPeak = buckets.maxOrNull() ?: 0f
            if (maxPeak <= 0.002f) {
                List(bucketCount) { 0.08f }
            } else {
                val normScale = 1f / maxPeak.coerceAtLeast(0.05f)
                buckets.map { raw ->
                    sqrt((raw * normScale).coerceIn(0f, 1f))
                }
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { extractor.release() }
        }

        if (decoded.isNotEmpty()) {
            synchronized(peakCache) {
                peakCache.put(cacheKey, decoded)
            }
        }
        decoded
    }

    private fun decodeWindowPeak(
        extractor: MediaExtractor,
        codec: MediaCodec,
        info: MediaCodec.BufferInfo,
        maxFrames: Int
    ): Float {
        var peak = 0f
        var decodedFrames = 0
        var inputDone = false
        var loops = 0
        while (decodedFrames < maxFrames && loops++ < 32) {
            if (!inputDone) {
                val inIdx = codec.dequeueInputBuffer(3_000L)
                if (inIdx >= 0) {
                    val inBuf = codec.getInputBuffer(inIdx)
                    val read = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                    if (read < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIdx, 0, read, extractor.sampleTime.coerceAtLeast(0L), 0)
                        extractor.advance()
                    }
                }
            }
            val outIdx = codec.dequeueOutputBuffer(info, 3_000L)
            if (outIdx >= 0) {
                val outBuf = codec.getOutputBuffer(outIdx)
                if (outBuf != null && info.size >= 2) {
                    peak = max(peak, measurePcm16Peak(outBuf, info.offset, info.size))
                    decodedFrames++
                }
                codec.releaseOutputBuffer(outIdx, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
            }
        }
        return peak
    }

    private fun measurePcm16Peak(buffer: ByteBuffer, offset: Int, size: Int): Float {
        val dup = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val end = (offset + size).coerceAtMost(dup.limit())
        dup.position(offset.coerceIn(0, end))
        dup.limit(end)
        val shortBuf = dup.asShortBuffer()
        val total = shortBuf.remaining()
        if (total <= 0) return 0f
        val step = max(1, total / 256)
        var maxSample = 0
        var idx = 0
        while (idx < total) {
            val v = abs(shortBuf.get(idx).toInt())
            if (v > maxSample) maxSample = v
            idx += step
        }
        return (maxSample.toFloat() / 32768f).coerceIn(0f, 1f)
    }
}
