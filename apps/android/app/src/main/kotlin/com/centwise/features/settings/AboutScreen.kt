package com.centwise.features.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.R

@Composable
fun AboutScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBackClick() }
                    .padding(10.dp)
            )
            Text(text = "About", style = CentwiseTypography.Headline, color = textPrimary)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Identity Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Centwise logo",
                    modifier = Modifier.size(84.dp)
                )
                Text("Centwise", style = CentwiseTypography.Title2, color = textPrimary)
                Text("Version 1.0", style = CentwiseTypography.Caption, color = textSecondary)
                Text(
                    "Bangladesh-focused expense tracker that turns bank and MFS SMS into insights automatically.",
                    style = CentwiseTypography.Subheadline,
                    color = textSecondary
                )
            }

            // Info Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                AboutRow(icon = Icons.Default.Public, tint = CentwiseColors.AccentBlue, title = "Platform", value = "Android", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = true)
                AboutRow(icon = Icons.Default.Info, tint = CentwiseColors.IncomeGreen, title = "Made for", value = "Bangladesh", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = true)
                AboutRow(icon = Icons.Default.Lock, tint = CentwiseColors.NagadOrange, title = "Data storage", value = "On-device only", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = true)
                AboutRow(icon = Icons.Default.Payments, tint = accent, title = "Currency", value = "Bangladeshi Taka (৳)", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = false)
            }

            // Privacy Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Privacy First", style = CentwiseTypography.Headline, color = textPrimary)
                Text(
                    "Centwise works fully offline. Your SMS messages, transactions, and balances never leave your device unless you create a backup yourself.",
                    style = CentwiseTypography.Subheadline,
                    color = textSecondary
                )
            }

            Text(
                "© 2026 Centwise",
                style = CentwiseTypography.Caption,
                color = textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(title, style = CentwiseTypography.Body, color = textPrimary, modifier = Modifier.weight(1f))
            Text(value, style = CentwiseTypography.Subheadline, color = textSecondary)
        }
        if (showDivider) {
            HorizontalDivider(color = Color(0x0A000000))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}
