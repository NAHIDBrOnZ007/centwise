package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun FilterPillRow(
    selectedPeriod: String = "This Month",
    selectedType: String = "Type",
    selectedCategory: String = "Category",
    onPeriodClick: () -> Unit = {},
    onTypeClick: () -> Unit = {},
    onCategoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Period Pill
        FilterCapsulePill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CalendarToday,
            text = selectedPeriod,
            isActive = selectedPeriod != "All Time",
            onClick = onPeriodClick,
            isDark = isDark
        )

        // 2. Type Pill
        FilterCapsulePill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Menu,
            text = selectedType,
            isActive = selectedType != "Type",
            onClick = onTypeClick,
            isDark = isDark
        )

        // 3. Category Pill
        FilterCapsulePill(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Tune,
            text = selectedCategory,
            isActive = selectedCategory != "Category",
            onClick = onCategoryClick,
            isDark = isDark
        )
    }
}

@Composable
fun FilterCapsulePill(
    icon: ImageVector,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val accent = com.centwise.features.settings.AccentOptions.byName(com.centwise.features.settings.AppearancePrefs.accentName).color
    val bg = if (isActive) {
        accent
    } else {
        if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg
    }
    val contentColor = if (isActive) {
        Color.White
    } else {
        if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            style = CentwiseTypography.Caption.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 11.sp
            ),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            color = contentColor
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FilterPillRowPreview() {
    FilterPillRow()
}
