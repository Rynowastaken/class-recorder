package com.ryno.classrecorder.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class DshIconsTest {
    @Test
    fun allVectorIconsParseCleanly() {
        val all = listOf(
            DshIcons.Add,
            DshIcons.ArrowBack,
            DshIcons.AutoAwesome,
            DshIcons.CalendarMonth,
            DshIcons.Check,
            DshIcons.Checklist,
            DshIcons.ChevronRight,
            DshIcons.Close,
            DshIcons.ContentCopy,
            DshIcons.DeleteOutline,
            DshIcons.EditNote,
            DshIcons.EventAvailable,
            DshIcons.ExpandLess,
            DshIcons.ExpandMore,
            DshIcons.FolderZip,
            DshIcons.Forward10,
            DshIcons.GraphicEq,
            DshIcons.Hub,
            DshIcons.Link,
            DshIcons.Mic,
            DshIcons.MicOff,
            DshIcons.Pause,
            DshIcons.PlayArrow,
            DshIcons.RadioButtonUnchecked,
            DshIcons.Refresh,
            DshIcons.Replay5,
            DshIcons.RestartAlt,
            DshIcons.Save,
            DshIcons.School,
            DshIcons.Search,
            DshIcons.SearchOff,
            DshIcons.Settings,
            DshIcons.Stop,
            DshIcons.Terminal
        )
        assertTrue(all.size == 34)
        for (vec in all) {
            assertTrue("Empty vector ${vec.name}", vec.root.size > 0)
        }
    }
}
