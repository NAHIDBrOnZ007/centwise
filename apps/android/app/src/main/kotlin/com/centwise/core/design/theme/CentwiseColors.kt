package com.centwise.core.design.theme

import androidx.compose.ui.graphics.Color

object CentwiseColors {
    // MARK: - Brand & Accents
    val PrimaryEmerald = Color(0xFF00A86B)
    val PrimaryEmeraldDark = Color(0xFF008052)
    val PrimaryEmeraldLight = Color(0xFF33CC8C)

    // MARK: - Bangladesh MFS & Bank Brand Colors
    val BKashPink = Color(0xFFE2136E)
    val NagadOrange = Color(0xFFF7941D)
    val RocketPurple = Color(0xFF8C3494)
    val UpayBlue = Color(0xFF007ACC)
    val CellfinGreen = Color(0xFF00994D)
    val CityBankRed = Color(0xFFD91A26)
    val BracBankBlue = Color(0xFF0D4099)
    val EasternBankGold = Color(0xFFCC991A)

    // MARK: - Modern Aesthetic Accents
    val CyberCyan = Color(0xFF06B6D4)
    val ElectricIndigo = Color(0xFF6366F1)
    val AestheticLavender = Color(0xFFA855F7)
    val CrimsonRuby = Color(0xFFE11D48)
    val SunsetAmber = Color(0xFFF59E0B)
    val MintBreeze = Color(0xFF10B981)
    val RoseGold = Color(0xFFFB7185)
    val GraphiteSlate = Color(0xFF64748B)
    val NordicSky = Color(0xFF1488E0)
    val SakuraBloom = Color(0xFFF472B6)
    val MidnightAmethyst = Color(0xFF7C3AED)
    val NeonCoral = Color(0xFFFD7266)
    val AuroraTeal = Color(0xFF14B8A6)
    val MatchaGreen = Color(0xFF84CC16)
    val WarmChampagne = Color(0xFFD97706)
    val ObsidianCharcoal = Color(0xFF334155)

    // MARK: - Financial Semantics
    val IncomeGreen = Color(0xFF22C55E)
    val ExpenseRed = Color(0xFFEF4444)
    val TransferBlue = Color(0xFF3B82F6)
    val RefundAmber = Color(0xFFF59E0B)
    val SavedTeal = Color(0xFF14B8A6)
    val AccentBlue = Color(0xFF007AFF)

    // MARK: - Light Theme Palette
    val LightBackground = Color(0xFFF5F7FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceSecondary = Color(0xFFECEFF1)
    val LightCardBorder = Color(0x0F000000)
    val LightTextPrimary = Color(0xFF000000)
    val LightTextSecondary = Color(0xFF8E8E93)
    val LightSearchBg = Color(0xFFEFEFF4)
    val LightTabBarBg = Color(0xFFFFFFFF)
    val LightTabActivePill = Color(0x2400A86B)

    // MARK: - Dark & AMOLED Palette
    val DarkBackground = Color(0xFF0D1117)
    val DarkSurface = Color(0xFF161F28)
    val DarkSurfaceSecondary = Color(0xFF212B36)
    val DarkCardBorder = Color(0x14FFFFFF)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFF8E8E93)
    val DarkSearchBg = Color(0xFF1C2430)
    val DarkTabBarBg = Color(0xFF161F28)
    val DarkTabActivePill = Color(0x26FFFFFF)

    val AmoledBackground = Color(0xFF000000)
    val AmoledSurface = Color(0xFF121212)

    fun providerColor(providerName: String): Color = when (providerName.lowercase()) {
        "bkash" -> BKashPink
        "nagad" -> NagadOrange
        "rocket" -> RocketPurple
        "upay" -> UpayBlue
        "city bank", "city" -> CityBankRed
        "brac bank", "brac" -> BracBankBlue
        "ebl", "eastern" -> EasternBankGold
        "cellfin" -> CellfinGreen
        else -> TransferBlue
    }
}
