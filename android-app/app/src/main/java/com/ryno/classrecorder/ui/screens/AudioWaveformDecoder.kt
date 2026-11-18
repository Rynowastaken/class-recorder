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
                decodeSeekWindowsContinuous(
                    extractor = extractor,
                    codec = codec,
                    info = info,
                    durationUs = durationUs,
                    buckets = buckets
                )
            } else {
                decodeFullStream(
                    extractor = extractor,
                    codec = codec,
                    info = info,
                    durationUs = durationUs,
                    buckets = buckets
                )
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

    /**
     * Feeds 2 AAC access units per seek window continuously without calling [MediaCodec.flush]
     * (packet 0 primes the MDCT overlap; packet 1 yields the clean window amplitude), avoiding
     * 72× binder flush round-trips on hardware/C2 decoders.
     */
    private fun decodeSeekWindowsContinuous(
        extractor: MediaExtractor,
        codec: MediaCodec,
        info: MediaCodec.BufferInfo,
        durationUs: Long,
        buckets: FloatArray
    ) {
        val bucketCount = buckets.size
        val cleanBuckets = FloatArray(bucketCount)
        val fallbackBuckets = FloatArray(bucketCount)
        var bucketIdx = 0
        var packetInBucket = 0
        var inputDone = false
        var outputDone = false
        var safetyLoops = 0

        while (!outputDone && safetyLoops++ < 2_000) {
            var waitedInputOnce = false
            while (!inputDone) {
                val timeoutUs = if (!waitedInputOnce) {
                    waitedInputOnce = true
                    500L
                } else {
                    0L
                }
                val inIdx = codec.dequeueInputBuffer(timeoutUs)
                if (inIdx < 0) break
                val inBuf = codec.getInputBuffer(inIdx)

                var queued = false
                while (bucketIdx < bucketCount && !queued) {
                    if (packetInBucket == 0) {
                        val targetUs = (durationUs * bucketIdx.toLong()) / bucketCount.toLong()
                        extractor.seekTo(targetUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                    }
                    val read = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                    if (read < 0) {
                        if (packetInBucket == 0) {
                            bucketIdx = bucketCount
                        } else {
                            packetInBucket = 0
                            bucketIdx++
                        }
                    } else {
                        val syntheticPts = (bucketIdx * 200_000L) + (packetInBucket * 80_000L)
                        codec.queueInputBuffer(inIdx, 0, read, syntheticPts, 0)
                        extractor.advance()
                        packetInBucket++
                        if (packetInBucket >= 2) {
                            packetInBucket = 0
                            bucketIdx++
                        }
                        queued = true
                    }
                }

                if (!queued) {
                    codec.queueInputBuffer(
                        inIdx,
                        0,
                        0,
                        bucketCount * 200_000L,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    )
                    inputDone = true
                }
            }

            var outIdx = codec.dequeueOutputBuffer(info, 1_500L)
            while (outIdx >= 0) {
                val outBuf = codec.getOutputBuffer(outIdx)
                if (outBuf != null && info.size >= 2) {
                    val peak = measurePcm16Peak(outBuf, info.offset, info.size)
                    val pts = info.presentationTimeUs.coerceAtLeast(0L)
                    val b = ((pts + 40_000L) / 200_000L).toInt().coerceIn(0, bucketCount - 1)
                    fallbackBuckets[b] = max(fallbackBuckets[b], peak)
                    if (pts - (b * 200_000L) >= 45_000L) {
                        cleanBuckets[b] = max(cleanBuckets[b], peak)
                    }
                }
                codec.releaseOutputBuffer(outIdx, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true
                    break
                }
                outIdx = codec.dequeueOutputBuffer(info, 0L)
            }
        }

        for (b in 0 until bucketCount) {
            buckets[b] = if (cleanBuckets[b] > 0f) cleanBuckets[b] else fallbackBuckets[b]
        }
    }

    private fun decodeFullStream(
        extractor: MediaExtractor,
        codec: MediaCodec,
        info: MediaCodec.BufferInfo,
        durationUs: Long,
        buckets: FloatArray
    ) {
        val bucketCount = buckets.size
        var inputDone = false
        var outputDone = false
        var decodedFrames = 0
        var safetyLoops = 0

        while (!outputDone && safetyLoops++ < 4_000) {
            var waitedInputOnce = false
            while (!inputDone) {
                val timeoutUs = if (!waitedInputOnce) {
                    waitedInputOnce = true
                    500L
                } else {
                    0L
                }
                val inIdx = codec.dequeueInputBuffer(timeoutUs)
                if (inIdx < 0) break
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

            var outIdx = codec.dequeueOutputBuffer(info, 1_500L)
            while (outIdx >= 0) {
                val outBuf = codec.getOutputBuffer(outIdx)
                if (outBuf != null && info.size >= 2) {
                    val peak = measurePcm16Peak(outBuf, info.offset, info.size)
                    val bucket = if (durationUs > 0L) {
                        ((info.presentationTimeUs.coerceIn(0L, durationUs) * bucketCount) / (durationUs + 1L))
                            .toInt()
                            .coerceIn(0, bucketCount - 1)
                    } else {
                        decodedFrames % bucketCount
                    }
                    buckets[bucket] = max(buckets[bucket], peak)
                    decodedFrames++
                }
                codec.releaseOutputBuffer(outIdx, false)
                if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true
                    break
                }
                outIdx = codec.dequeueOutputBuffer(info, 0L)
            }
        }
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
