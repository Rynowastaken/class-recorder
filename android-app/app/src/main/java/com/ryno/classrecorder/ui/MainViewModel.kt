package com.ryno.classrecorder.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ryno.classrecorder.core.CourseSlot
import com.ryno.classrecorder.core.CurrentClassMatch
import com.ryno.classrecorder.core.JsonModels
import com.ryno.classrecorder.core.McpStatus
import com.ryno.classrecorder.core.RecordingBridge
import com.ryno.classrecorder.core.RecordingRecord
import com.ryno.classrecorder.core.RustCore
import com.ryno.classrecorder.core.UnifiedSchedule
import com.ryno.classrecorder.service.RecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

data class MainUiState(
    val schedule: UnifiedSchedule? = null,
    val currentClass: CurrentClassMatch? = null,
    val selectedCourseOverride: CourseSlot? = null,
    val recordings: List<RecordingRecord> = emptyList(),
    val mcpStatus: McpStatus = McpStatus(false, 8765, emptyList()),
    val lanIp: String = "127.0.0.1",
    val scheduleSourceInput: String = "https://uni-campus-app.pages.dev/s/qVaaFS5A",
    val isSyncing: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application), RecordingBridge {

    private val _ui = MutableStateFlow(MainUiState())
    val ui = _ui.asStateFlow()

    val recorderState = RecordingService.state

    init {
        val ctx = getApplication<Application>()
        val recordingsDir = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "Recordings")
        RustCore.initCore(ctx.filesDir.absolutePath, recordingsDir.absolutePath, this)

        RecordingService.onRecordingFinished = {
            refreshAll()
        }

        // Start MCP server automatically on launch so agents can connect immediately
        RustCore.startMcpServer(8765, "")

        refreshAll()

        // Periodically refresh MCP status & current class match
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(4000)
                refreshMcpAndCurrentClass()
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val schedObj = JSONObject(RustCore.getScheduleJson())
            val sched = if (schedObj.optBoolean("ok")) {
                JsonModels.parseSchedule(schedObj.optJSONObject("data"))
            } else null

            val curObj = JSONObject(RustCore.getCurrentClassJson())
            val cur = if (curObj.optBoolean("ok")) {
                JsonModels.parseCurrentClass(curObj.optJSONObject("data"))
            } else null

            val recObj = JSONObject(RustCore.listRecordings(""))
            val recs = if (recObj.optBoolean("ok")) {
                JsonModels.parseRecordings(recObj.optJSONArray("data"))
            } else emptyList()

            val mcpObj = JSONObject(RustCore.getMcpStatus())
            val mcp = if (mcpObj.optBoolean("ok")) {
                JsonModels.parseMcpStatus(mcpObj.optJSONObject("data"))
            } else McpStatus(false, 8765, emptyList())

            _ui.value = _ui.value.copy(
                schedule = sched,
                currentClass = cur,
                recordings = recs,
                mcpStatus = mcp,
                lanIp = detectLanIp(),
                scheduleSourceInput = sched?.sourceUrl?.takeIf { it.isNotBlank() }
                    ?: _ui.value.scheduleSourceInput
            )
        }
    }

    private fun refreshMcpAndCurrentClass() {
        val curObj = JSONObject(RustCore.getCurrentClassJson())
        val cur = if (curObj.optBoolean("ok")) {
            JsonModels.parseCurrentClass(curObj.optJSONObject("data"))
        } else _ui.value.currentClass

        val mcpObj = JSONObject(RustCore.getMcpStatus())
        val mcp = if (mcpObj.optBoolean("ok")) {
            JsonModels.parseMcpStatus(mcpObj.optJSONObject("data"))
        } else _ui.value.mcpStatus

        val recObj = JSONObject(RustCore.listRecordings(""))
        val recs = if (recObj.optBoolean("ok")) {
            JsonModels.parseRecordings(recObj.optJSONArray("data"))
        } else _ui.value.recordings

        _ui.value = _ui.value.copy(
            currentClass = cur,
            mcpStatus = mcp,
            recordings = recs,
            lanIp = detectLanIp()
        )
    }

    fun syncSchedule(sourceUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _ui.value = _ui.value.copy(isSyncing = true, statusMessage = null, scheduleSourceInput = sourceUrl)
            val res = JSONObject(RustCore.syncSchedule(sourceUrl))
            if (res.optBoolean("ok")) {
                _ui.value = _ui.value.copy(isSyncing = false, statusMessage = "Schedule synced")
                refreshAll()
            } else {
                _ui.value = _ui.value.copy(
                    isSyncing = false,
                    statusMessage = res.optString("error", "Sync failed")
                )
            }
        }
    }

    fun selectCourseOverride(course: CourseSlot?) {
        _ui.value = _ui.value.copy(selectedCourseOverride = course)
    }

    fun startRecording() {
        val course = _ui.value.selectedCourseOverride
        val json = if (course != null) {
            JSONObject().apply {
                put("courseName", course.name)
                put("teacher", course.teacher)
                put("room", course.room)
            }.toString()
        } else "{}"
        RecordingService.start(getApplication(), json)
    }

    fun startRecordingForCourse(course: CourseSlot, slot: String = "") {
        val json = JSONObject().apply {
            put("courseName", course.name)
            put("teacher", course.teacher)
            put("room", course.room)
            put("slot", slot)
        }.toString()
        _ui.value = _ui.value.copy(selectedCourseOverride = course)
        RecordingService.start(getApplication(), json)
    }

    fun stopRecording() {
        RecordingService.stop(getApplication())
    }

    fun togglePause() {
        RecordingService.togglePause(getApplication())
    }

    fun updateNotes(id: String, notes: String, tags: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val tagsJson = JSONArray(tags).toString()
            RustCore.updateRecordingNotes(id, notes, tagsJson)
            refreshAll()
        }
    }

    fun deleteRecording(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            RustCore.deleteRecording(id)
            refreshAll()
        }
    }

    fun toggleMcpServer(port: Int = 8765, authToken: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            if (_ui.value.mcpStatus.running) {
                RustCore.stopMcpServer()
            } else {
                val res = JSONObject(RustCore.startMcpServer(port, authToken))
                if (!res.optBoolean("ok")) {
                    _ui.value = _ui.value.copy(statusMessage = res.optString("error", "MCP failed"))
                }
            }
            refreshMcpAndCurrentClass()
        }
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(statusMessage = null)
    }

    // Called from Rust MCP threads via JNI
    override fun startRecordingFromAgent(courseJson: String): String {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return JSONObject().put("error", "Microphone permission not granted on device").toString()
        }
        if (RecordingService.state.value.isRecording) {
            return JSONObject().put("error", "A recording is already active").toString()
        }
        RecordingService.start(ctx, courseJson)
        // Wait briefly for RecordingService to prepare and enter recording state
        for (i in 0 until 20) {
            Thread.sleep(100)
            val active = RecordingService.state.value.activeRecording
            if (RecordingService.state.value.isRecording && active != null) {
                return JSONObject().put("recording", JSONObject().apply {
                    put("id", active.id)
                    put("filePath", active.filePath)
                    put("courseName", active.courseName)
                    put("teacher", active.teacher)
                    put("room", active.room)
                    put("slot", active.slot)
                    put("startedAt", active.startedAt)
                    put("durationMs", active.durationMs)
                    put("status", active.status)
                    put("notes", active.notes)
                    put("tags", JSONArray(active.tags))
                }).toString()
            }
        }
        return JSONObject().put("error", "Timed out waiting for RecordingService to start").toString()
    }

    override fun stopRecordingFromAgent(): String {
        val active = RecordingService.state.value.activeRecording
            ?: return JSONObject().put("error", "No active recording").toString()
        RecordingService.stop(getApplication())
        for (i in 0 until 20) {
            Thread.sleep(100)
            if (!RecordingService.state.value.isRecording) {
                val res = JSONObject(RustCore.listRecordings(""))
                val list = JsonModels.parseRecordings(res.optJSONArray("data"))
                val updated = list.firstOrNull { it.id == active.id } ?: active.copy(status = "completed")
                return JSONObject().put("recording", JSONObject().apply {
                    put("id", updated.id)
                    put("filePath", updated.filePath)
                    put("courseName", updated.courseName)
                    put("teacher", updated.teacher)
                    put("room", updated.room)
                    put("slot", updated.slot)
                    put("startedAt", updated.startedAt)
                    put("durationMs", updated.durationMs)
                    put("status", updated.status)
                    put("notes", updated.notes)
                    put("tags", JSONArray(updated.tags))
                }).toString()
            }
        }
        return JSONObject().put("error", "Timed out waiting for RecordingService to stop").toString()
    }

    private fun detectLanIp(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }
}
