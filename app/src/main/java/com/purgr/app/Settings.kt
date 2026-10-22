package com.purgr.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** Accent colours: [light]/[dark] is the accent in each theme. DYNAMIC follows the wallpaper (Android 12+). */
enum class Accent(val label: String, val light: Long, val dark: Long) {
    OCEAN("Ocean", 0xFF2A66CC, 0xFF8AB4FF),
    LAVENDER("Lavender", 0xFF6150C8, 0xFFAFA2FF),
    ROSE("Rose", 0xFFC0436A, 0xFFFF8FB0),
    CORAL("Coral", 0xFFC8503C, 0xFFFF8A75),
    HONEY("Honey", 0xFFA86F00, 0xFFF2C14E),
    SAGE("Sage", 0xFF2F7D5B, 0xFF7FD1A8),
    GRAPHITE("Graphite", 0xFF2B2B2E, 0xFFE6E6E8),
    DYNAMIC("Wallpaper", 0xFF6750A4, 0xFFD0BCFF),
}

object Settings {
    private lateinit var prefs: SharedPreferences

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var accent by mutableStateOf(Accent.OCEAN)
        private set

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("purgr_settings", Context.MODE_PRIVATE)
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString("themeMode", null)!!) }.getOrDefault(themeMode)
        accent = runCatching { Accent.valueOf(prefs.getString("accent", null)!!) }.getOrDefault(accent)
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString("themeMode", mode.name).apply()
    }

    fun updateAccent(value: Accent) {
        accent = value
        prefs.edit().putString("accent", value.name).apply()
    }
}
