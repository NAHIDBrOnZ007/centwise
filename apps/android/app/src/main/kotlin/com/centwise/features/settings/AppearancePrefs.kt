package com.centwise.features.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Black")
}

data class AccentOption(
    val name: String,
    val color: Color
)

object AccentOptions {
    val all = listOf(
        AccentOption("Emerald", Color(0xFF00A86B)),
        AccentOption("bKash Pink", Color(0xFFE2136E)),
        AccentOption("Nagad Orange", Color(0xFFF7941D)),
        AccentOption("Rocket Violet", Color(0xFF8C3494)),
        AccentOption("Sapphire Blue", Color(0xFF007AFF))
    )

    fun byName(name: String): AccentOption = all.firstOrNull { it.name == name } ?: all.first()
}

object AppearancePrefs {
    private const val PREFS_NAME = "centwise_appearance"

    var themeMode: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var accentName: String by mutableStateOf("Emerald")
        private set
    var hapticsEnabled: Boolean by mutableStateOf(true)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        themeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString("themeMode", null) }
            ?: ThemeMode.SYSTEM
        accentName = prefs.getString("accentName", null) ?: "Emerald"
        hapticsEnabled = prefs.getBoolean("hapticsEnabled", true)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("themeMode", mode.name).apply()
    }

    fun setAccent(context: Context, name: String) {
        accentName = name
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("accentName", name).apply()
    }

    fun setHaptics(context: Context, enabled: Boolean) {
        hapticsEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("hapticsEnabled", enabled).apply()
    }
}
