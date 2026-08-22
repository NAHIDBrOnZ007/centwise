package com.centwise.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun SettingsScreen(
    onAppearanceClick: () -> Unit = {},
    onCurrencyClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onBudgetsClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onSubscriptionsClick: () -> Unit = {},
    onSmartRulesClick: () -> Unit = {},
    onFAQClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Text(
                text = "Settings",
                style = CentwiseTypography.LargeTitle,
                color = textPrimary
            )
        }

        // Section 1: Personalization (Matching Screenshot ios ui 1.jpeg)
        item {
            Column {
                Text(
                    text = "Personalization",
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                    color = textSecondary,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                ) {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        iconColor = Color(0xFFFF9500),
                        title = "Appearance",
                        subtitle = "Theme, accent color, dark mode",
                        onClick = onAppearanceClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.Paid,
                        iconColor = Color(0xFF34C759),
                        title = "Currency",
                        subtitle = "Default currency for totals and new entries",
                        onClick = onCurrencyClick,
                        showDivider = false,
                        isDark = isDark
                    )
                }
            }
        }

        // Section 2: Data Management (Matching Screenshot ios ui 1.jpeg)
        item {
            Column {
                Text(
                    text = "Data Management",
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                    color = textSecondary,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                ) {
                    SettingsRow(
                        icon = Icons.Default.GridView,
                        iconColor = Color(0xFFAF52DE),
                        title = "Categories",
                        subtitle = "Manage expense and income categories",
                        onClick = onCategoriesClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.PieChart,
                        iconColor = Color(0xFF34C759),
                        title = "Budgets",
                        subtitle = "Set spending limits by category",
                        onClick = onBudgetsClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.AccountBalance,
                        iconColor = Color(0xFF007AFF),
                        title = "Accounts",
                        subtitle = "Manage bank accounts and cards",
                        onClick = onAccountsClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.Autorenew,
                        iconColor = Color(0xFF5856D6),
                        title = "Subscriptions",
                        subtitle = "Track recurring payments",
                        onClick = onSubscriptionsClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.AutoAwesome,
                        iconColor = Color(0xFFFF2D55),
                        title = "Smart Rules",
                        subtitle = "Auto-categorize transactions",
                        onClick = onSmartRulesClick,
                        showDivider = false,
                        isDark = isDark
                    )
                }
            }
        }

        // Section 3: Support & About
        item {
            Column {
                Text(
                    text = "Support & About",
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                    color = textSecondary,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                ) {
                    AppLockRow(isDark = isDark)

                    SettingsRow(
                        icon = Icons.Default.HelpOutline,
                        iconColor = Color(0xFF007AFF),
                        title = "FAQ",
                        subtitle = "Common questions and answers",
                        onClick = onFAQClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconColor = Color(0xFF8E8E93),
                        title = "About Centwise",
                        subtitle = "Version, privacy, and credits",
                        onClick = onAboutClick,
                        showDivider = false,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLockRow(isDark: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var lockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled) }

    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (lockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "App Lock",
                tint = Color(0xFF34C759),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App Lock",
                    style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = AppLockManager.lockTypeLabel(context),
                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1
                )
            }

            Switch(
                checked = lockEnabled,
                onCheckedChange = { enabled ->
                    lockEnabled = enabled
                    AppLockManager.setLockEnabled(context, enabled)
                },
                colors = SwitchDefaults.colors(checkedTrackColor = accent)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 54.dp),
            color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
            thickness = 1.dp
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bare Icon without background box (Screenshot ios ui 1.jpeg)
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(16.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 54.dp),
                color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000),
                thickness = 1.dp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
