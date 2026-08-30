package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

object CategoryIconHelper {
    fun iconFor(category: String?, iconKey: String? = null): ImageVector {
        val symbol = (iconKey ?: "").trim().lowercase()
        val cat = (category ?: "").trim().lowercase()

        // 1. Direct SF Symbol matching from Rust / iOS repository
        when (symbol) {
            "fork.knife", "cup.and.saucer", "fork.knife.circle", "fork.knife.circle.fill" -> return Icons.Default.Restaurant
            "car", "car.fill", "car.2", "car.2.fill", "tram", "bus" -> return Icons.Default.DirectionsCar
            "bag", "bag.fill", "bag.circle", "cart", "cart.fill" -> return Icons.Default.ShoppingBag
            "bolt", "bolt.fill", "bolt.circle", "bolt.circle.fill", "lightbulb" -> return Icons.Default.Bolt
            "antenna.radiowaves.left.and.right", "iphone", "phone", "phone.fill", "simcard" -> return Icons.Default.PhoneIphone
            "banknote", "banknote.fill", "dollarsign.circle", "dollarsign" -> return Icons.Default.Payments
            "arrow.left.arrow.right", "arrow.2.squarepath", "repeat" -> return Icons.Default.SwapHoriz
            "cross.case", "cross.case.fill", "cross", "heart", "heart.fill", "pills", "stethoscope" -> return Icons.Default.MedicalServices
            "play.tv", "play.tv.fill", "tv", "tv.fill", "film", "popcorn" -> return Icons.Default.Tv
            "book", "book.fill", "book.closed", "graduationcap", "graduationcap.fill" -> return Icons.Default.School
            "square.grid.2x2", "square.grid.2x2.fill", "ellipsis" -> return Icons.Default.Category
            "house", "house.fill", "building", "building.2" -> return Icons.Default.Home
            "sparkles", "face.smiling" -> return Icons.Default.Spa
            "chart.line.uptrend.xyaxis", "chart.pie", "chart.bar" -> return Icons.AutoMirrored.Filled.TrendingUp
        }

        // 2. Exact keyword matching on Category Name (safe from substring collisions like "healthcare" containing "car")
        return when {
            cat.contains("health") || cat.contains("medical") || cat.contains("doctor") || cat.contains("pharma") || cat.contains("hospital") -> Icons.Default.MedicalServices
            cat.contains("food") || cat.contains("dining") || cat.contains("restaurant") || cat.contains("cafe") || cat.contains("lunch") || cat.contains("dinner") -> Icons.Default.Restaurant
            cat.contains("transport") || cat.contains("ride") || cat.contains("uber") || cat.contains("pathao") || cat.contains("fuel") || cat.contains("vehicle") || cat.split(" ", "-", "_").any { it == "car" } -> Icons.Default.DirectionsCar
            cat.contains("shopping") || cat.contains("daraz") || cat.contains("aarong") || cat.contains("cloth") || cat.contains("grocery") || cat.split(" ", "-", "_").any { it == "bag" } -> Icons.Default.ShoppingBag
            cat.contains("bill") || cat.contains("utilit") || cat.contains("electric") || cat.contains("water") || cat.contains("gas") || cat.contains("desco") || cat.contains("dpdc") || cat.contains("nesco") -> Icons.Default.Bolt
            cat.contains("recharge") || cat.contains("mobile") || cat.contains("phone") || cat.contains("grameenphone") || cat.contains("banglalink") || cat.contains("robi") || cat.contains("airtel") || cat.contains("teletalk") -> Icons.Default.PhoneIphone
            cat.contains("salary") || cat.contains("income") || cat.contains("wage") || cat.contains("deposit") || cat.contains("payroll") || cat.contains("cash") -> Icons.Default.Payments
            cat.contains("transfer") || cat.contains("send money") || cat.contains("cash out") || cat.contains("remittance") -> Icons.Default.SwapHoriz
            cat.contains("entertainment") || cat.contains("movie") || cat.contains("cineplex") || cat.contains("netflix") || cat.contains("spotify") || cat.contains("game") -> Icons.Default.Tv
            cat.contains("education") || cat.contains("school") || cat.contains("college") || cat.contains("university") || cat.contains("tuition") || cat.contains("course") || cat.contains("book") -> Icons.Default.School
            cat.contains("home") || cat.contains("rent") || cat.contains("apartment") -> Icons.Default.Home
            cat.contains("beauty") || cat.contains("salon") || cat.contains("spa") -> Icons.Default.Spa
            cat.contains("invest") || cat.contains("stock") || cat.contains("savings") -> Icons.AutoMirrored.Filled.TrendingUp
            else -> Icons.Default.Category
        }
    }

    fun defaultColorFor(category: String?): Color {
        val cat = (category ?: "").trim().lowercase()
        return when {
            cat.contains("health") || cat.contains("medical") -> Color(0xFFEF4444)
            cat.contains("food") || cat.contains("dining") -> Color(0xFFF97316)
            cat.contains("transport") || cat.contains("ride") || cat.split(" ", "-", "_").any { it == "car" } -> Color(0xFF06B6D4)
            cat.contains("shopping") || cat.contains("cloth") -> Color(0xFFEC4899)
            cat.contains("bill") || cat.contains("utilit") -> Color(0xFFEAB308)
            cat.contains("recharge") || cat.contains("mobile") -> Color(0xFF8B5CF6)
            cat.contains("salary") || cat.contains("income") -> Color(0xFF10B981)
            cat.contains("transfer") -> Color(0xFF3B82F6)
            cat.contains("entertainment") || cat.contains("movie") -> Color(0xFF6366F1)
            cat.contains("education") -> Color(0xFF14B8A6)
            else -> Color(0xFF64748B)
        }
    }
}

@Composable
fun CategoryIconBadge(
    category: String,
    iconKey: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    useThemeAccent: Boolean = true,
    customColor: Color? = null
) {
    val themeAccent = AccentOptions.byName(AppearancePrefs.accentName).color
    val iconColor = customColor ?: if (useThemeAccent) themeAccent else CategoryIconHelper.defaultColorFor(category)
    val badgeBg = iconColor.copy(alpha = 0.14f)
    val icon = CategoryIconHelper.iconFor(category, iconKey)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(badgeBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category,
            tint = iconColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
