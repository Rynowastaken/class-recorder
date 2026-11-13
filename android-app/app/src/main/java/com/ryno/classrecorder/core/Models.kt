package com.ryno.classrecorder.core

import org.json.JSONArray
import org.json.JSONObject

data class CourseSlot(
    val name: String,
    val teacher: String = "",
    val room: String = "",
    val color: String = "#6750A4"
)

data class TimeSlotRow(
    val slot: String,
    val time: String,
    val days: List<CourseSlot?>
)

data class UnifiedSchedule(
    val schoolName: String,
    val sourceUrl: String,
    val syncedAt: Long,
    val slots: List<TimeSlotRow>
)

data class CurrentClassMatch(
    val kind: String,
    val slot: String,
    val time: String,
    val weekdayIndex: Int,
    val course: CourseSlot
)

data class RecordingRecord(
    val id: String,
    val filePath: String,
    val courseName: String,
    val teacher: String,
    val room: String,
    val slot: String,
    val startedAt: Long,
    val durationMs: Long,
    val status: String,
    val notes: String,
    val tags: List<String>
)

data class McpStatus(
    val running: Boolean,
    val port: Int,
    val recentCalls: List<String>
)

object JsonModels {
    fun parseSchedule(obj: JSONObject?): UnifiedSchedule? {
        if (obj == null) return null
        val slotsArr = obj.optJSONArray("slots") ?: return null
        val slots = mutableListOf<TimeSlotRow>()
        for (i in 0 until slotsArr.length()) {
            val row = slotsArr.optJSONObject(i) ?: continue
            val daysArr = row.optJSONArray("days") ?: JSONArray()
            val days = mutableListOf<CourseSlot?>()
            for (d in 0 until daysArr.length()) {
                val c = daysArr.optJSONObject(d)
                days.add(
                    if (c == null) null
                    else CourseSlot(
                        name = c.optString("name"),
                        teacher = c.optString("teacher"),
                        room = c.optString("room"),
                        color = c.optString("color", "#6750A4")
                    )
                )
            }
            slots.add(
                TimeSlotRow(
                    slot = row.optString("slot"),
                    time = row.optString("time"),
                    days = days
                )
            )
        }
        return UnifiedSchedule(
            schoolName = obj.optString("schoolName", "NUTC"),
            sourceUrl = obj.optString("sourceUrl"),
            syncedAt = obj.optLong("syncedAt"),
            slots = slots
        )
    }

    fun parseCurrentClass(obj: JSONObject?): CurrentClassMatch? {
        if (obj == null) return null
        val c = obj.optJSONObject("course") ?: return null
        return CurrentClassMatch(
            kind = obj.optString("kind", "active"),
            slot = obj.optString("slot"),
            time = obj.optString("time"),
            weekdayIndex = obj.optInt("weekdayIndex"),
            course = CourseSlot(
                name = c.optString("name"),
                teacher = c.optString("teacher"),
                room = c.optString("room"),
                color = c.optString("color", "#6750A4")
            )
        )
    }

    fun parseRecording(obj: JSONObject): RecordingRecord {
        val tagsArr = obj.optJSONArray("tags") ?: JSONArray()
        val tags = (0 until tagsArr.length()).mapNotNull { tagsArr.optString(it).takeIf { s -> s.isNotEmpty() } }
        return RecordingRecord(
            id = obj.optString("id"),
            filePath = obj.optString("filePath"),
            courseName = obj.optString("courseName", "Uncategorized"),
            teacher = obj.optString("teacher"),
            room = obj.optString("room"),
            slot = obj.optString("slot"),
            startedAt = obj.optLong("startedAt"),
            durationMs = obj.optLong("durationMs"),
            status = obj.optString("status"),
            notes = obj.optString("notes"),
            tags = tags
        )
    }

    fun parseRecordings(arr: JSONArray?): List<RecordingRecord> {
        if (arr == null) return emptyList()
        val out = mutableListOf<RecordingRecord>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { out.add(parseRecording(it)) }
        }
        return out
    }

    fun parseMcpStatus(obj: JSONObject?): McpStatus {
        if (obj == null) return McpStatus(false, 8765, emptyList())
        val callsArr = obj.optJSONArray("recentCalls") ?: JSONArray()
        val calls = (0 until callsArr.length()).mapNotNull { callsArr.optString(it) }
        return McpStatus(
            running = obj.optBoolean("running", false),
            port = obj.optInt("port", 8765),
            recentCalls = calls
        )
    }
}
