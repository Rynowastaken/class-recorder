package com.ryno.classrecorder.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattersTest {
    @Test
    fun normalizesTimeRangesToEnDash() {
        assertEquals("08:10–09:00", UiFormatters.formatTimeRange("08:10~09:00"))
        assertEquals("13:25–14:15", UiFormatters.formatTimeRange("13:25 - 14:15"))
        assertEquals("14:20–15:10", UiFormatters.formatTimeRange("14:20-15:10"))
        assertEquals("", UiFormatters.formatTimeRange("   "))
        assertEquals("19:00", UiFormatters.formatTimeRange("19:00"))
    }

    @Test
    fun previewsFilenameWithAllSupportedTokensAndTypos() {
        val out = UiFormatters.previewFilename(
            template = "{class} {date} - {lession number} ({room})",
            dateFormat = "yyyy-MM-dd",
            courseName = "民法",
            room = "3806"
        )
        assertEquals("民法 2026-11-18 - 1 (3806).m4a", out)
    }

    @Test
    fun formatsDurationTabular() {
        assertEquals("00:05", UiFormatters.formatDuration(5_200L))
        assertEquals("48:12", UiFormatters.formatDuration((48 * 60 + 12) * 1000L))
        assertEquals("1:02:03", UiFormatters.formatDuration((3600 + 120 + 3) * 1000L))
    }
}
