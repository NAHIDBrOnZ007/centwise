package com.centwise.features.settings

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "About", style = CentwiseTypography.Headline, color = textPrimary)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Identity Card (Official Centwise Logo matching iOS Image("AppLogo"))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.centwise.R.drawable.app_logo),
                    contentDescription = "Centwise logo",
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )

                Text("Centwise", style = CentwiseTypography.Title2, color = textPrimary, fontWeight = FontWeight.Bold)
                Text("Version 1.0 (1)", style = CentwiseTypography.Caption, color = textSecondary)
                Text(
                    "Bangladesh-focused expense tracker that turns bank and MFS SMS into insights automatically.",
                    style = CentwiseTypography.Subheadline,
                    color = textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Info Card (Exact matching iOS LabeledContent)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                AboutLabeledRow(label = "Platform", value = "Android", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = true, isDark = isDark)
                AboutLabeledRow(label = "Made for", value = "Bangladesh 🇧🇩", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = true, isDark = isDark)
                AboutLabeledRow(label = "Data storage", value = "On-device only", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = true, isDark = isDark)
                AboutLabeledRow(label = "Currency", value = "Bangladeshi Taka (৳)", textPrimary = textPrimary, textSecondary = textSecondary, showDivider = false, isDark = isDark)
            }

            // Privacy Card (Matching iOS Section)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
private fun AboutLabeledRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color,
    showDivider: Boolean,
    isDark: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = CentwiseTypography.Body, color = textPrimary)
            Text(value, style = CentwiseTypography.Subheadline, color = textSecondary, fontWeight = FontWeight.Medium)
        }
        if (showDivider) {
            HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen()
}
