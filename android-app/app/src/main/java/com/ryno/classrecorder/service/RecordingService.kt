package com.ryno.classrecorder.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ryno.classrecorder.MainActivity
import com.ryno.classrecorder.core.JsonModels
import com.ryno.classrecorder.core.RecordingRecord
import com.ryno.classrecorder.core.RustCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class RecorderState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,
    val activeRecording: RecordingRecord? = null
)

class RecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var recorder: MediaRecorder? = null
    private var tickerJob: Job? = null
    private var startEpochMs: Long = 0L
    private var accumulatedMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val courseJson = intent.getStringExtra(EXTRA_COURSE_JSON) ?: "{}"
                startInternal(courseJson)
            }
            ACTION_STOP -> stopInternal()
            ACTION_TOGGLE_PAUSE -> togglePauseInternal()
        }
        return START_NOT_STICKY
    }

    private fun startInternal(courseJson: String) {
        if (_state.value.isRecording) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prepRaw = RustCore.prepareRecording(courseJson)
        val prepObj = JSONObject(prepRaw)
        if (!prepObj.optBoolean("ok", false)) return
        val rec = JsonModels.parseRecording(prepObj.getJSONObject("data"))

        createNotificationChannel()
        val notification = buildNotification(rec.courseName, isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(rec.filePath)
            mr.prepare()
            mr.start()

            recorder = mr
            accumulatedMs = 0L
            startEpochMs = System.currentTimeMillis()
            _state.value = RecorderState(
                isRecording = true,
                isPaused = false,
                elapsedMs = 0L,
                amplitude = 0f,
                activeRecording = rec
            )
            startTicker()
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            RustCore.deleteRecording(rec.id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun togglePauseInternal() {
        val current = _state.value
        if (!current.isRecording) return
        try {
            if (current.isPaused) {
                recorder?.resume()
                startEpochMs = System.currentTimeMillis()
                _state.value = current.copy(isPaused = false)
            } else {
                recorder?.pause()
                accumulatedMs += System.currentTimeMillis() - startEpochMs
                _state.value = current.copy(isPaused = true, amplitude = 0f)
            }
            val courseName = current.activeRecording?.courseName ?: "Class Recording"
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildNotification(courseName, _state.value.isPaused))
        } catch (_: Exception) {
        }
    }

    private fun stopInternal() {
        val current = _state.value
        if (!current.isRecording) return

        tickerJob?.cancel()
        val totalMs = if (current.isPaused) {
            accumulatedMs
        } else {
            accumulatedMs + (System.currentTimeMillis() - startEpochMs)
        }

        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
        }

        current.activeRecording?.let { rec ->
            RustCore.completeRecording(rec.id, totalMs)
        }

        _state.value = RecorderState()
        onRecordingFinished?.invoke()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                delay(100)
                val cur = _state.value
                if (!cur.isRecording) break
                if (!cur.isPaused) {
                    val elapsed = accumulatedMs + (System.currentTimeMillis() - startEpochMs)
                    val maxAmp = try {
                        (recorder?.maxAmplitude ?: 0) / 32767f
                    } catch (_: Exception) {
                        0f
                    }
                    _state.value = cur.copy(
                        elapsedMs = elapsed,
                        amplitude = maxAmp.coerceIn(0f, 1f)
                    )
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Class Recording",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(courseName: String, isPaused: Boolean): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RecordingService::class.java).apply { action = ACTION_TOGGLE_PAUSE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Paused: $courseName" else "Recording: $courseName")
            .setContentText("Saving to $courseName folder")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, if (isPaused) "Resume" else "Pause", pauseIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        recorder?.release()
        recorder = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "class_recorder_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.ryno.classrecorder.ACTION_START"
        const val ACTION_STOP = "com.ryno.classrecorder.ACTION_STOP"
        const val ACTION_TOGGLE_PAUSE = "com.ryno.classrecorder.ACTION_TOGGLE_PAUSE"
        const val EXTRA_COURSE_JSON = "course_json"

        private val _state = MutableStateFlow(RecorderState())
        val state = _state.asStateFlow()

        var onRecordingFinished: (() -> Unit)? = null

        fun start(context: Context, courseJson: String = "{}") {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_COURSE_JSON, courseJson)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun togglePause(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_TOGGLE_PAUSE
            }
            context.startService(intent)
        }
    }
}
