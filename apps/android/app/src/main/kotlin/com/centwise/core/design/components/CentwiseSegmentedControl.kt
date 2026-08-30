package com.centwise.core.design.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography

/**
 * Idiomatic Jetpack Compose Segmented Control with physics-based spring slide animation,
 * capsule pill rounding, and guaranteed crisp white text on active segments.
 */
@Composable
fun <T> CentwiseSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    accent: Color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val selectedIndex = items.indexOfFirst {
        it == selectedItem || (it is String && selectedItem is String && it.equals(selectedItem, ignoreCase = true))
    }.coerceAtLeast(0)

    val containerBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0C000000)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(containerBg)
            .padding(2.5.dp)
    ) {
        val segmentWidth = if (items.isNotEmpty()) maxWidth / items.size else maxWidth
        val animatedOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pill_offset"
        )

        // Fully Rounded Capsule Pill Sliding Indicator
        Box(
            modifier = Modifier
                .offset(x = animatedOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accent)
        )

        // Segment Labels
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(item) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                        color = if (isSelected) Color.White else if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// Backward-compatibility alias
@Composable
fun <T> IosSegmentedControl(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    accent: Color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    CentwiseSegmentedControl(
        items = items,
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        itemLabel = itemLabel,
        modifier = modifier,
        accent = accent,
        isDark = isDark
    )
}
