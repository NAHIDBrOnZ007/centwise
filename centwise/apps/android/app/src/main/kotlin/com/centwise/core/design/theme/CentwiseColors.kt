package com.centwise.core.design.theme

import androidx.compose.ui.graphics.Color

object CentwiseColors {
    // Core Semantic Colors
    val AccentMauve = Color(0xFFB55D75)
    val AccentBlue = Color(0xFF007AFF)

    // Transaction & Stat Status Colors
    val IncomeGreen = Color(0xFF34C759)
    val ExpenseRed = Color(0xFFFF3B30)
    val SavedTeal = Color(0xFF30B0C7)

    // Light Theme Palette
    val LightBackground = Color(0xFFFFFFFF)
    val LightSurface = Color(0xFFF8F9FA)
    val LightCardBorder = Color(0x0A000000)
    val LightTextPrimary = Color(0xFF000000)
    val LightTextSecondary = Color(0xFF8E8E93)
    val LightSearchBg = Color(0xFFEFEFF4)
    val LightTabBarBg = Color(0xE0FFFFFF)
    val LightTabActivePill = Color(0x24B55D75)

    // Dark & AMOLED Palette
    val DarkBackground = Color(0xFF000000)
    val DarkSurface = Color(0xFF1C1C1E)
    val DarkCardBorder = Color(0x14FFFFFF)
    val DarkTextPrimary = Color(0xFFFFFFFF)
    val DarkTextSecondary = Color(0xFF8E8E93)
    val DarkSearchBg = Color(0xFF1C1C1E)
    val DarkTabBarBg = Color(0xE01C1C1E)
    val DarkTabActivePill = Color(0x26FFFFFF)

    // Bangladesh MFS & Bank Brand Palette
    val BKashPink = Color(0xFFE2136E)
    val NagadOrange = Color(0xFFF7941D)
    val RocketPurple = Color(0xFF8C3494)
    val UpayBlue = Color(0xFF00A3E0)
    val CityBankRed = Color(0xFFD71920)
    val BracBankBlue = Color(0xFF005A9C)
    val IslamiBankGreen = Color(0xFF008752)
    val StandardCharteredGreen = Color(0xFF00A3E0)

    fun providerColor(providerName: String): Color {
        return when (providerName.lowercase()) {
            "bkash" -> BKashPink
            "nagad" -> NagadOrange
            "rocket" -> RocketPurple
            "upay" -> UpayBlue
            "city bank" -> CityBankRed
            "brac bank" -> BracBankBlue
            "islami bank" -> IslamiBankGreen
            else -> AccentBlue
        }
    }
}
