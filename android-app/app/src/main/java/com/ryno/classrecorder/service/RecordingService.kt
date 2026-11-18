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
import android.graphics.drawable.Icon
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ryno.classrecorder.MainActivity
import com.ryno.classrecorder.R
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
import org.json.JSONArray
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
            ACTION_DISCARD -> discardInternal()
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
        // The live notification IS the foreground notification.
        //
        // An earlier revision split them, reasoning that FLAG_NO_CLEAR (forced by
        // startForeground) blocks promotion. That was wrong: HyperOS stopped
        // drawing the island at all once the timer notification was posted
        // separately with notify(). Whatever else is true, the foreground
        // notification is the one HyperOS renders in its island, so it has to be
        // this one.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationIdFor(this),
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(notificationIdFor(this), notification)
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
            val elapsed = if (_state.value.isPaused) accumulatedMs
            else accumulatedMs + (System.currentTimeMillis() - startEpochMs)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notificationIdFor(this), buildNotification(courseName, _state.value.isPaused, elapsed))
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

    private fun discardInternal() {
        val current = _state.value
        if (!current.isRecording) return

        tickerJob?.cancel()
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
        }

        current.activeRecording?.let { rec ->
            RustCore.deleteRecording(rec.id)
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
                delay(33)
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
        // The silent channel that carries the FOREGROUND notification. Without
        // it startForeground() throws CannotPostForegroundServiceNotification.
        val fgsChannel = NotificationChannel(
            FGS_CHANNEL_ID,
            "Recording service",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "Keeps recording alive in the background"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(fgsChannel)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active Recording",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Live recording status and timer"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * The minimal notification that keeps this service in the foreground so it
     * may hold the microphone. It is deliberately quiet and never the one the
     * user acts on: foreground notifications cannot be promoted to Live Updates,
     * so the actionable notification is posted separately in startInternal().
     */
    private fun buildSilentForegroundNotification(): Notification =
        Notification.Builder(this, FGS_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            // Silence comes from the IMPORTANCE_MIN channel; setSilent() is not
            // available on Notification.Builder at this API level.
            .setPriority(Notification.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()

    /**
     * The redacted form the island and lock screen draw: no course name, no
     * notes, just enough to say a recording is running and for how long.
     */
    private fun buildPublicVersion(courseName: String, isPaused: Boolean): Notification {
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setColor(0xFFB3261E.toInt())
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (isPaused) "Recording paused" else "Recording")
            .setOngoing(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setShortCriticalText(if (isPaused) "Paused" else "Recording")
        }
        return builder.build()
    }

    private fun buildNotification(
        courseName: String,
        isPaused: Boolean,
        elapsedMs: Long = 0L
    ): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
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
        val discardIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, RecordingService::class.java).apply { action = ACTION_DISCARD },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shortChip = if (isPaused) "Paused" else formatShortTimer(elapsedMs)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Paused • $courseName" else "Recording • $courseName")
            .setContentText(if (isPaused) "Tap Resume to continue" else "Saving to $courseName/")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(0xFFB3261E.toInt())
            .setColorized(true)
            // PROGRESS, not SERVICE. HyperOS promotes ongoing notifications to a
            // Live Update / island only for categories it recognises as "live",
            // and PROGRESS is the one a recording is. This mirrors the
            // notification a working HyperOS app posts on this same device.
            .setCategory(Notification.CATEGORY_PROGRESS)
            // PRIVATE paired with a publicVersion, matching the shape a working
            // promoted notification on this device uses: the island and lock
            // screen need something to draw, and PRIVATE without a
            // publicVersion leaves them nothing.
            // Reverted to the original configuration: PUBLIC visibility, no
            // publicVersion, no timeout. This is the shape that produced the
            // system pill in the first place. Do not "improve" it without a
            // working test - the PRIVATE/publicVersion/timeout combination lost
            // the pill entirely on HyperOS 3.
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis() - elapsedMs)
            .setUsesChronometer(!isPaused)
            .setContentIntent(openIntent)
            // Actions MUST carry a real icon. HyperOS (and most OEM SystemUI
            // skins) silently drop Notification.Action entries whose icon is
            // null, which is why the focus pill rendered with no controls at
            // all while the base notification declared three.
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(
                        this,
                        if (isPaused) R.drawable.ic_notif_resume else R.drawable.ic_notif_pause
                    ),
                    if (isPaused) "Resume" else "Pause",
                    pauseIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notif_stop),
                    "Stop",
                    stopIntent
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_notif_delete),
                    "Delete",
                    discardIntent
                ).build()
            )

        // ORIGINAL promotion request. This is the exact shape that produced the
        // recording pill on this device: ask via the extra, set the chip, and
        // let the platform decide. The ProgressStyle / canPostPromotedNotifications
        // experiment made the pill disappear, so it is reverted.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val extras = Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
            }
            builder.addExtras(extras)
            builder.setShortCriticalText(shortChip)
        }

        // Focus template, sent unconditionally as it was originally. HyperOS
        // only honours it for allowlisted packages, but sending it costs
        // nothing and this is the configuration that produced the pill.
        builder.addExtras(
            buildHyperOsFocusExtras(
                courseName = courseName,
                isPaused = isPaused,
                elapsedMs = elapsedMs,
                pauseIntent = pauseIntent,
                stopIntent = stopIntent
            )
        )

        // ORIGINAL: force the promoted flag after build. This is what the app
        // did when the pill appeared. Removing it (and the other experiments)
        // made the pill go away, so it is restored.
        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            notification.flags = notification.flags or Notification.FLAG_PROMOTED_ONGOING
        }
        return notification
    }

    private fun buildHyperOsFocusExtras(
        courseName: String,
        isPaused: Boolean,
        elapsedMs: Long,
        pauseIntent: PendingIntent,
        stopIntent: PendingIntent
    ): Bundle {
        val now = System.currentTimeMillis()
        val timerWhen = now - elapsedMs
        val timerType = if (isPaused) 2 else 1
        val title = if (isPaused) "Paused • $courseName" else "Recording • $courseName"
        val content = if (isPaused) "Tap Resume to continue" else formatShortTimer(elapsedMs)

        val picIcon = Icon.createWithResource(this, R.mipmap.ic_launcher_round)
        val picsBundle = Bundle().apply {
            putParcelable("miui.focus.pic_1", picIcon)
            putParcelable("miui.focus.pic_2", picIcon)
        }

        // Ticker: the collapsed pill otherwise truncates the course name to
        // "Uncategorized...". SystemUI reads miui.focus.ticker and scrolls it;
        // tickerAutoClose dismisses after N seconds (0 = stay).
        val tickerIcon = Icon.createWithResource(this, R.mipmap.ic_launcher_round)

        val pauseAction = Notification.Action.Builder(
            Icon.createWithResource(
                this,
                if (isPaused) R.drawable.ic_notif_resume else R.drawable.ic_notif_pause
            ),
            if (isPaused) "Resume" else "Pause",
            pauseIntent
        ).addExtras(
            Bundle().apply {
                putString("icon_name", if (isPaused) "action_restart" else "action_pause")
            }
        ).build()
        val stopAction = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_notif_stop),
            "Stop",
            stopIntent
        ).addExtras(
            Bundle().apply {
                putString("icon_name", "action_done")
            }
        ).build()
        // Per Xiaomi's island documentation (dev.mi.com pId=2131, section 2.1):
        // the Notification.Action objects go DIRECTLY into a bundle under the
        // keys miui.focus.action_1 / miui.focus.action_2, and that bundle is
        // added as the "miui.focus.actions" extra. The JSON param_island
        // references those keys by name.
        //
        // An earlier revision wrapped them in Notification().extras instead,
        // which is the platform's serialized form and NOT what SystemUI reads
        // for this key - which is why the island rendered without controls.
        val actionsBundle = Bundle().apply {
            putParcelable("miui.focus.action_1", pauseAction)
            putParcelable("miui.focus.action_2", stopAction)
        }

        val timerInfo = JSONObject().apply {
            put("timerType", timerType)
            put("timerWhen", timerWhen)
            put("timerSystemCurrent", now)
        }

        val bigIslandArea = JSONObject().apply {
            put(
                "imageTextInfoLeft",
                JSONObject().apply {
                    put("type", 1)
                    put(
                        "picInfo",
                        JSONObject().apply {
                            put("type", 2)
                            put("pic", "voiceWaveSmall")
                            put("loop", !isPaused)
                            put("autoplay", !isPaused)
                        }
                    )
                    put(
                        "textInfo",
                        JSONObject().apply {
                            put("title", courseName)
                            put("content", if (isPaused) "Paused" else "Recording")
                        }
                    )
                }
            )
            put(
                "sameWidthDigitInfo",
                JSONObject().apply {
                    put("timerInfo", timerInfo)
                }
            )
        }

        val smallIslandArea = JSONObject().apply {
            put(
                "picInfo",
                if (!isPaused) {
                    JSONObject().apply {
                        put("type", 2)
                        put("pic", "voiceWaveSmall")
                        put("loop", true)
                        put("autoplay", true)
                    }
                } else {
                    JSONObject().apply {
                        put("type", 1)
                        put("pic", "miui.focus.pic_2")
                    }
                }
            )
        }

        val paramIsland = JSONObject().apply {
            put("islandPriority", 1)
            put("islandTimeout", if (isPaused) 600 else 43200)
            put("islandProperty", 2)
            put("bigIslandArea", bigIslandArea)
            put("smallIslandArea", smallIslandArea)
        }

        val actionsArray = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("type", 0)
                    put("action", "miui.focus.action_1")
                }
            )
            put(
                JSONObject().apply {
                    put("type", 0)
                    put("action", "miui.focus.action_2")
                }
            )
        }

        val animTextInfo = JSONObject().apply {
            put(
                "animIconInfo",
                JSONObject().apply {
                    put("number", 0)
                    put("src", "voiceWaveBig")
                    put("loop", !isPaused)
                    put("type", 1)
                    put("autoplay", !isPaused)
                }
            )
            put("title", title)
            put("content", content)
            put("timerInfo", timerInfo)
        }

        val paramV2 = JSONObject().apply {
            put("protocol", 1)
            put("business", "audio_recording")
            put("notifyId", "${packageName}100")
            put("scene", "recorder")
            put("title", title)
            put("content", content)
            put("timerType", timerType)
            put("timerWhen", timerWhen)
            put("timerSystemCurrent", now)
            put("updatable", true)
            put("enableFloat", false)
            put("showSmallIcon", false)
            put("islandFirstFloat", false)
            put("param_island", paramIsland)
            put("animTextInfo", animTextInfo)
            put("actions", actionsArray)
        }

        val rootParam = JSONObject().apply {
            put("protocol", 1)
            put("scene", "recorder")
            put("title", title)
            put("content", content)
            put("timerType", timerType)
            put("timerWhen", timerWhen)
            put("timerSystemCurrent", now)
            put("updatable", true)
            put("enableFloat", false)
            put("showSmallIcon", false)
            put("param_v2", paramV2)
        }

        val rvBar = buildFocusRemoteViews(R.layout.focus_notification_bar, courseName, isPaused, elapsedMs, pauseIntent, stopIntent)
        val rvBarNight = buildFocusRemoteViews(R.layout.focus_notification_bar_night, courseName, isPaused, elapsedMs, pauseIntent, stopIntent)

        return Bundle().apply {
            putParcelable("miui.focus.rvBar", rvBar)
            putParcelable("miui.focus.rvBarNight", rvBarNight)
            putBundle("miui.focus.pics", picsBundle)
            putBundle("miui.focus.actions", actionsBundle)
            putString("miui.focus.param", rootParam.toString())
            // Scrolling ticker text. Without these the collapsed pill truncates
            // a course name to "Uncategorized..." and there is no way to read
            // which class is being recorded.
            putParcelable("miui.focus.pic_ticker", tickerIcon)
            putParcelable("miui.focus.pic_ticker_dark", tickerIcon)
            putStringArray(
                "miui.focus.ticker",
                arrayOf(if (isPaused) "Paused - $courseName" else "Recording - $courseName")
            )
            putInt("miui.focus.tickerAutoClose", 0)
        }
    }

    private fun buildFocusRemoteViews(
        layoutRes: Int,
        courseName: String,
        isPaused: Boolean,
        elapsedMs: Long,
        pauseIntent: PendingIntent,
        stopIntent: PendingIntent
    ): RemoteViews {
        val rv = RemoteViews(packageName, layoutRes)
        rv.setTextViewText(R.id.focus_title, courseName)
        rv.setTextViewText(R.id.focus_status, if (isPaused) "Paused" else "Recording")
        rv.setChronometer(
            R.id.focus_chronometer,
            SystemClock.elapsedRealtime() - elapsedMs,
            "%s",
            !isPaused
        )
        rv.setTextViewText(R.id.focus_btn_pause, if (isPaused) "Resume" else "Pause")
        rv.setOnClickPendingIntent(R.id.focus_btn_pause, pauseIntent)
        rv.setOnClickPendingIntent(R.id.focus_btn_stop, stopIntent)
        return rv
    }

    /**
     * Whether SystemUI will honour our miui.focus.* extras.
     *
     * HyperOS gates the focus template behind a per-package allowlist that the
     * user cannot grant and only Xiaomi can add. The supported query is the
     * canShowFocus binder call into the statusbar provider; if it says no, the
     * whole payload is discarded by SystemUI and our RemoteViews never render.
     */
    private fun isFocusAllowed(): Boolean = runCatching {
        val uri = android.net.Uri.parse("content://miui.statusbar.notification.public")
        val extras = Bundle().apply { putString("package", packageName) }
        contentResolver.call(uri, "canShowFocus", null, extras)
            ?.getBoolean("canShowFocus", false) == true
    }.getOrDefault(false)

    private fun formatShortTimer(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        val hr = min / 60
        return if (hr > 0) String.format("%d:%02d:%02d", hr, min % 60, sec)
        else String.format("%02d:%02d", min, sec)
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        recorder?.release()
        recorder = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "class_recorder_live_v2"

        /** Promotion requires a timeout; 12h outlives any lecture block. */
        private const val PROMOTED_TIMEOUT_MS = 12L * 60L * 60L * 1000L

        /** Silent channel that keeps the service foreground for mic capture. */
        private const val FGS_CHANNEL_ID = "class_recorder_fgs"
        private const val FGS_NOTIFICATION_ID = 1

        // Notification ids are namespaced per package. A second install of the
        // app (.uitest builds used for side-by-side UI testing) would otherwise
        // publish to the same id and let one install clobber the other's live
        // recording notification.
        fun notificationIdFor(context: android.content.Context): Int =
            1001 + (context.packageName.hashCode() and 0x1FF)
        const val ACTION_START = "com.ryno.classrecorder.ACTION_START"
        const val ACTION_STOP = "com.ryno.classrecorder.ACTION_STOP"
        const val ACTION_DISCARD = "com.ryno.classrecorder.ACTION_DISCARD"
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

        fun discard(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_DISCARD
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
