package com.ryno.classrecorder.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityHashTest {
    @Test
    fun blankKeysNormalizeAndNeverReturnNegativeIndex() {
        val expected = DshColors.identityIndex("Uncategorized")
        assertEquals(expected, DshColors.identityIndex(""))
        assertEquals(expected, DshColors.identityIndex("   "))
        val samples = listOf("程式設計與應用 (一)", "民法", "大學之道", "體育", "服務與學習", "C++/Java")
        for (s in samples) {
            val idx = DshColors.identityIndex(s, 8)
            assertTrue("idx in 0..7 for $s", idx in 0..7)
            assertEquals(idx, DshColors.identityIndex(s, 8))
        }
    }
}
