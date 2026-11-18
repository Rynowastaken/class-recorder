package com.ryno.classrecorder.ui

import android.content.Context
import com.ryno.classrecorder.ui.theme.AppAccent

enum class DshThemeMode { SYSTEM, LIGHT, DARK }

object AppearancePreferences {
    private const val PREFS_NAME = "dsh_appearance"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_ACCENT_ID = "accent_id"
    private const val KEY_LAST_PRESET_ID = "last_preset_id"

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
}
