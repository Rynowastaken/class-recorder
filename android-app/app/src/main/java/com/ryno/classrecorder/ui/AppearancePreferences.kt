package com.ryno.classrecorder.ui

import android.content.Context
import com.ryno.classrecorder.ui.theme.AppAccent

enum class DshThemeMode { SYSTEM, LIGHT, DARK }

object AppearancePreferences {
    private const val PREFS_NAME = "dsh_appearance"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_ID = "accent_id"
    private const val KEY_LAST_PRESET_ID = "last_preset_id"

    /**
     * Absolute path of the user's chosen background image, or null for the
     * built-in Mist gradient.
     *
     * The file is COPIED into app storage when picked (see CustomBackground),
     * so this is a stable path we own rather than a content:// URI that can be
     * revoked when the picker's permission grant expires.
     */
    private const val KEY_BACKGROUND_PATH = "background_path"

    /**
     * Wallpaper blur strength, 0f..1f, where 0 is sharp and 1 is heavily frosted.
     *
     * Stored as a normalised strength rather than a pixel radius so the same
     * setting looks identical on a 720p phone and a 1440p one - the radius is
     * derived from the stored image's size at load time.
     */
    private const val KEY_BACKGROUND_BLUR = "background_blur"

    /** Default strength: the middle of the range, which reads as frosted. */
    const val DEFAULT_BACKGROUND_BLUR = 0.45f

    fun loadBackgroundBlur(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getFloat(KEY_BACKGROUND_BLUR, DEFAULT_BACKGROUND_BLUR)
        return raw.coerceIn(0f, 1f)
    }

    fun saveBackgroundBlur(context: Context, strength: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_BACKGROUND_BLUR, strength.coerceIn(0f, 1f))
            .apply()
    }

    private val presetIds: Set<String> = AppAccent.presets.map { it.id }.toSet()
    private val validAccentIds: Set<String> = presetIds + AppAccent.WALLPAPER_ID

    fun parseThemeMode(raw: String?): DshThemeMode {
        val cleaned = raw?.trim()?.uppercase().orEmpty()
        return DshThemeMode.entries.firstOrNull { it.name == cleaned } ?: DshThemeMode.SYSTEM
    }

    fun parseAccentId(raw: String?): String {
        val cleaned = raw?.trim()?.lowercase().orEmpty()
        return if (cleaned in validAccentIds) cleaned else AppAccent.Mist.id
    }

    fun load(context: Context): Pair<DshThemeMode, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = parseThemeMode(prefs.getString(KEY_THEME_MODE, null))
        val accentId = parseAccentId(prefs.getString(KEY_ACCENT_ID, null))
        return mode to accentId
    }

    fun loadLastPresetId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LAST_PRESET_ID, null)?.trim()?.lowercase().orEmpty()
        return if (raw in presetIds) raw else AppAccent.Mist.id
    }

    fun saveThemeMode(context: Context, mode: DshThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun saveAccentId(context: Context, accentId: String) {
        val valid = parseAccentId(accentId)
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCENT_ID, valid)
        if (valid in presetIds) {
            editor.putString(KEY_LAST_PRESET_ID, valid)
        }
        editor.apply()
    }

    /** The chosen background image path, or null when the Mist gradient is in use. */
    fun loadBackgroundPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_BACKGROUND_PATH, null)?.takeIf { it.isNotBlank() }
        // A file the user (or a cleaner app) deleted must not leave the app on a
        // black backdrop, so verify it is still readable before reporting it.
        if (path != null && !java.io.File(path).canRead()) {
            saveBackgroundPath(context, null)
            return null
        }
        return path
    }

    fun saveBackgroundPath(context: Context, path: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .apply { if (path == null) remove(KEY_BACKGROUND_PATH) else putString(KEY_BACKGROUND_PATH, path) }
            .apply()
    }
}
