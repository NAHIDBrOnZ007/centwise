package com.centwise.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.core.profile.UserPrefs

@Composable
fun SettingsScreen(
    onAppearanceClick: () -> Unit = {},
    onCurrencyClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onBudgetsClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onSubscriptionsClick: () -> Unit = {},
    onSmartRulesClick: () -> Unit = {},
    onReviewQueueClick: () -> Unit = {},
    onDataManagementClick: () -> Unit = {},
    onFAQClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val reviewQueueItems by com.centwise.data.repository.ReviewQueueRepository.shared.items.collectAsState()
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    var currentUserName by remember { mutableStateOf(UserPrefs.getUserName(context)) }
    var currentUserAvatar by remember { mutableStateOf(UserPrefs.getUserAvatar(context)) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    if (showEditProfileSheet) {
        EditProfileSheet(
            currentName = currentUserName,
            currentAvatar = currentUserAvatar,
            onDismiss = { showEditProfileSheet = false },
            onSave = { newName, newAvatar ->
                UserPrefs.setUserName(context, newName)
                UserPrefs.setUserAvatar(context, newAvatar)
                currentUserName = newName
                currentUserAvatar = newAvatar
                showEditProfileSheet = false
            },
            isDark = isDark
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
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

        // Profile Header Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .iosBounceClick {
                        showEditProfileSheet = true
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                        .border(2.dp, accent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = UserPrefs.getAvatarResId(currentUserAvatar)),
                        contentDescription = "User Avatar",
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUserName,
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Tap to change avatar & name",
                        style = CentwiseTypography.Caption,
                        color = accent
                    )
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Section 1: Personalization (Exact matching iOS SettingsScreen)
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
                        iconColor = accent,
                        title = "Appearance",
                        subtitle = "Theme and accent color",
                        onClick = onAppearanceClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.Paid,
                        iconColor = accent,
                        title = "Currency",
                        subtitle = "Currency for totals and new entries",
                        onClick = onCurrencyClick,
                        showDivider = false,
                        isDark = isDark
                    )
                }
            }
        }

        // Section 2: Money & Automation (Exact matching iOS SettingsScreen)
        item {
            Column {
                Text(
                    text = "Money & Automation",
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
                        icon = Icons.Default.AccountBalance,
                        iconColor = accent,
                        title = "Accounts",
                        subtitle = "Bank, card, mobile wallet, and Cash accounts",
                        onClick = onAccountsClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.GridView,
                        iconColor = accent,
                        title = "Categories",
                        subtitle = "Expense and income categories",
                        onClick = onCategoriesClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.PieChart,
                        iconColor = accent,
                        title = "Budgets",
                        subtitle = "Category spending limits",
                        onClick = onBudgetsClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.Subscriptions,
                        iconColor = accent,
                        title = "Subscriptions",
                        subtitle = "Recurring payments",
                        onClick = onSubscriptionsClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.AutoAwesome,
                        iconColor = accent,
                        title = "Smart Rules",
                        subtitle = "Automatic transaction categorization",
                        onClick = onSmartRulesClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.MarkEmailRead,
                        iconColor = accent,
                        title = "Review Queue",
                        subtitle = if (reviewQueueItems.isNotEmpty()) "${reviewQueueItems.size} pending SMS messages" else "Resolve ambiguous financial messages",
                        onClick = onReviewQueueClick,
                        showDivider = false,
                        isDark = isDark
                    )
                }
            }
        }

        // Section 3: Privacy & Data (Exact matching iOS SettingsScreen)
        item {
            Column {
                Text(
                    text = "Privacy & Data",
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
                    AppLockRow(accent = accent, isDark = isDark)

                    SettingsRow(
                        icon = Icons.Default.Storage,
                        iconColor = accent,
                        title = "Data Management",
                        subtitle = "Export, demo data, and reset",
                        onClick = onDataManagementClick,
                        showDivider = false,
                        isDark = isDark
                    )
                }
            }
        }

        // Section 4: Support (Exact matching iOS SettingsScreen)
        item {
            Column {
                Text(
                    text = "Support",
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
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconColor = accent,
                        title = "Frequently Asked Questions",
                        subtitle = "Help using Centwise",
                        onClick = onFAQClick,
                        showDivider = true,
                        isDark = isDark
                    )
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconColor = accent,
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
private fun AppLockRow(accent: Color, isDark: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var lockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled) }

    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

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
                tint = accent,
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
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = Color.White
                )
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
                .iosBounceClick { onClick() }
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
