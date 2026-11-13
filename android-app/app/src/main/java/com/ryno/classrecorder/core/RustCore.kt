package com.ryno.classrecorder.core

interface RecordingBridge {
    fun startRecordingFromAgent(courseJson: String): String
    fun stopRecordingFromAgent(): String
}

object RustCore {
    init {
        System.loadLibrary("class_recorder_core")
    }

    external fun initCore(filesDir: String, recordingsDir: String, bridge: RecordingBridge): String
    external fun syncSchedule(sourceUrl: String): String
    external fun getScheduleJson(): String
    external fun getCurrentClassJson(): String
    external fun prepareRecording(courseOverrideJson: String): String
    external fun completeRecording(id: String, durationMs: Long): String
    external fun listRecordings(courseFilter: String): String
    external fun updateRecordingNotes(id: String, notes: String, tagsJson: String): String
    external fun deleteRecording(id: String): String
    external fun startMcpServer(port: Int, authToken: String): String
    external fun stopMcpServer(): String
    external fun getMcpStatus(): String
}
