package com.centwise.core.design.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

enum class CentwiseTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TRANSACTIONS("Transactions", Icons.AutoMirrored.Filled.List),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

/**
 * Idiomatic Jetpack Compose Dynamic Floating Tab Bar.
 * Inspired by PennyWise & Material 3 Expressive navigation:
 * Only the currently active tab expands horizontally to show its title,
 * while inactive tabs remain sleek icon-only buttons.
 */
@Composable
fun FloatingTabBar(
    selectedTab: CentwiseTab,
    onTabSelected: (CentwiseTab) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current
    val barBg = if (isDark) CentwiseColors.DarkTabBarBg else CentwiseColors.LightTabBarBg
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val activePillBg = if (isDark) accent.copy(alpha = 0.22f) else accent.copy(alpha = 0.14f)
    val inactiveColor = if (isDark) {
        CentwiseColors.DarkTextSecondary.copy(alpha = 0.70f)
    } else {
        CentwiseColors.LightTextSecondary.copy(alpha = 0.70f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = barBg,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            border = BorderStroke(
                1.dp,
                if (isDark) Color(0x33FFFFFF) else Color(0x14000000)
            )
        ) {
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CentwiseTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val tabInteraction = remember { MutableInteractionSource() }
                    val isTabPressed by tabInteraction.collectIsPressedAsState()
                    val tabPressScale by animateFloatAsState(
                        targetValue = if (isTabPressed) 0.93f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f),
                        label = "tab_press_scale_${tab.name}"
                    )
                    val itemColor by animateColorAsState(
                        targetValue = if (isSelected) accent else inactiveColor,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_color_${tab.name}"
                    )
                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) activePillBg else Color.Transparent,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tab_pill_bg_${tab.name}"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
                        label = "icon_scale_${tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = tabPressScale
                                scaleY = tabPressScale
                            }
                            .clip(CircleShape)
                            .background(pillBgColor)
                            .clickable(
                                interactionSource = tabInteraction,
                                indication = null
                            ) {
                                if (selectedTab != tab) {
                                    if (AppearancePrefs.hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onTabSelected(tab)
                                }
                            }
                            .padding(
                                horizontal = if (isSelected) 16.dp else 13.dp,
                                vertical = 10.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = itemColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(iconScale)
                            )
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                        expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                                       shrinkHorizontally(spring(stiffness = Spring.StiffnessMediumLow))
                            ) {
                                Text(
                                    text = tab.title,
                                    style = CentwiseTypography.Caption.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = itemColor,
                                    modifier = Modifier.padding(start = 8.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FloatingTabBarPreview() {
    FloatingTabBar(
        selectedTab = CentwiseTab.HOME,
        onTabSelected = {}
    )
}
