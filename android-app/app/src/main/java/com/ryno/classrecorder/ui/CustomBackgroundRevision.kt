package com.ryno.classrecorder.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Bumped whenever the custom background is set or cleared.
 *
 * The backdrop is composed once at the app root, so a preference write alone
 * would not repaint it. Reading this state in DshBackdrop makes every screen
 * pick up a new wallpaper immediately instead of after a restart.
 */
object CustomBackgroundRevision {
    var value by mutableIntStateOf(0)
        private set

    fun bump() { value++ }
}

/**
 * The current revision, for use as a composition key.
 *
 * Exposed as a composable-readable property so call sites do not need to know
 * that the backing store is a State object.
 */
val customBackgroundRevision: Int
    get() = CustomBackgroundRevision.value
