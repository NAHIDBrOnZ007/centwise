package com.centwise.features.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.centwise.core.design.theme.CentwiseColors

enum class ThemeMode(val displayName: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Black")
}

enum class AccentCategory(val displayName: String) {
    BANGLADESH("Bangladesh Providers"),
    AESTHETIC("Aesthetic Vibes"),
    MODERN("Modern & Minimal")
}

data class AccentOption(
    val name: String,
    val subtitle: String,
    val color: Color,
    val category: AccentCategory
)

object AccentOptions {
    val bangladeshOptions = listOf(
        AccentOption("Emerald Green", "Signature Centwise", CentwiseColors.PrimaryEmerald, AccentCategory.BANGLADESH),
        AccentOption("bKash Pink", "Vibrant Magenta", CentwiseColors.BKashPink, AccentCategory.BANGLADESH),
        AccentOption("Nagad Orange", "Warm Sunset", CentwiseColors.NagadOrange, AccentCategory.BANGLADESH),
        AccentOption("Rocket Violet", "Royal Purple", CentwiseColors.RocketPurple, AccentCategory.BANGLADESH),
        AccentOption("Upay Blue", "Dynamic Blue", CentwiseColors.UpayBlue, AccentCategory.BANGLADESH),
        AccentOption("Cellfin Green", "Islamic Emerald", CentwiseColors.CellfinGreen, AccentCategory.BANGLADESH),
        AccentOption("City Bank Red", "Signature Crimson", CentwiseColors.CityBankRed, AccentCategory.BANGLADESH),
        AccentOption("EBL Gold", "Eastern Gold", CentwiseColors.EasternBankGold, AccentCategory.BANGLADESH),
        AccentOption("Sapphire Blue", "Deep Cobalt", CentwiseColors.TransferBlue, AccentCategory.BANGLADESH)
    )

    val aestheticOptions = listOf(
        AccentOption("Cyber Cyan", "Electric Neon", CentwiseColors.CyberCyan, AccentCategory.AESTHETIC),
        AccentOption("Electric Indigo", "Aesthetic Iris", CentwiseColors.ElectricIndigo, AccentCategory.AESTHETIC),
        AccentOption("Velvet Lavender", "Pastel Dream", CentwiseColors.AestheticLavender, AccentCategory.AESTHETIC),
        AccentOption("Crimson Ruby", "Luxury Velvet", CentwiseColors.CrimsonRuby, AccentCategory.AESTHETIC),
        AccentOption("Sakura Bloom", "Cherry Blossom", CentwiseColors.SakuraBloom, AccentCategory.AESTHETIC),
        AccentOption("Midnight Amethyst", "Deep Royal Violet", CentwiseColors.MidnightAmethyst, AccentCategory.AESTHETIC),
        AccentOption("Neon Coral", "Sunset Peach Glow", CentwiseColors.NeonCoral, AccentCategory.AESTHETIC),
        AccentOption("Aurora Teal", "Northern Lights", CentwiseColors.AuroraTeal, AccentCategory.AESTHETIC),
        AccentOption("Nordic Sky", "Crisp Azure", CentwiseColors.NordicSky, AccentCategory.AESTHETIC)
    )

    val modernOptions = listOf(
        AccentOption("Sunburst Amber", "Golden Glow", CentwiseColors.SunsetAmber, AccentCategory.MODERN),
        AccentOption("Mint Breeze", "Fresh Neo Mint", CentwiseColors.MintBreeze, AccentCategory.MODERN),
        AccentOption("Matcha Green", "Zen Matcha", CentwiseColors.MatchaGreen, AccentCategory.MODERN),
        AccentOption("Warm Champagne", "Refined Amber", CentwiseColors.WarmChampagne, AccentCategory.MODERN),
        AccentOption("Rose Gold", "Soft Metallic", CentwiseColors.RoseGold, AccentCategory.MODERN),
        AccentOption("Graphite Slate", "Pure Minimalist", CentwiseColors.GraphiteSlate, AccentCategory.MODERN),
        AccentOption("Obsidian Noir", "Deep Slate Noir", CentwiseColors.ObsidianCharcoal, AccentCategory.MODERN)
    )

    val all: List<AccentOption> = bangladeshOptions + aestheticOptions + modernOptions

    fun byName(name: String): AccentOption {
        val trimmed = name.trim()
        return all.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            ?: all.firstOrNull { it.name.contains(trimmed, ignoreCase = true) }
            ?: all.first()
    }
}

object AppearancePrefs {
    private const val PREFS_NAME = "centwise_appearance"

    var themeMode: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var accentName: String by mutableStateOf("Emerald Green")
        private set
    var hapticsEnabled: Boolean by mutableStateOf(true)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        themeMode = ThemeMode.entries.firstOrNull { it.name == prefs.getString("themeMode", null) }
            ?: ThemeMode.SYSTEM
        accentName = prefs.getString("accentName", null) ?: "Emerald Green"
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
