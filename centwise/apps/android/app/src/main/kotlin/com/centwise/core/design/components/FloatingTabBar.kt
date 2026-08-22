package com.centwise.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography

enum class CentwiseTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TRANSACTIONS("Transactions", Icons.Default.List),
    ANALYTICS("Analytics", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun FloatingTabBar(
    selectedTab: CentwiseTab,
    onTabSelected: (CentwiseTab) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val barBg = if (isDark) CentwiseColors.DarkTabBarBg else CentwiseColors.LightTabBarBg
    val accent = com.centwise.features.settings.AccentOptions.byName(com.centwise.features.settings.AppearancePrefs.accentName).color
    val activeColor = accent
    val activePillBg = if (isDark) accent.copy(alpha = 0.20f) else accent.copy(alpha = 0.12f)
    val inactiveColor = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isDark) 16.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.12f)
                )
                .clip(CircleShape)
                .background(barBg)
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CentwiseTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else inactiveColor,
                    animationSpec = spring(),
                    label = "tab_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (isSelected) activePillBg else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = itemColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.title,
                            style = CentwiseTypography.Caption.copy(fontSize = 10.sp),
                            color = itemColor
                        )
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
