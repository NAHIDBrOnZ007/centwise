package com.centwise.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
fun AppearanceScreen(
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
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
            Text(
                text = "Appearance",
                style = CentwiseTypography.Headline,
                color = textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Theme Mode", style = CentwiseTypography.Headline, color = textPrimary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x24FFFFFF) else Color(0x0A000000))
                        .padding(4.dp)
                ) {
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        val isSelected = AppearancePrefs.themeMode == mode ||
                                (mode == ThemeMode.DARK && AppearancePrefs.themeMode == ThemeMode.AMOLED)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) accent else Color.Transparent)
                                .iosBounceClick { AppearancePrefs.setThemeMode(context, mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.displayName,
                                style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) Color.White else textSecondary
                            )
                        }
                    }
                }

                if (AppearancePrefs.themeMode == ThemeMode.DARK || AppearancePrefs.themeMode == ThemeMode.AMOLED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AMOLED Black", style = CentwiseTypography.Body, color = textPrimary)
                            Text(
                                "Pure black background for OLED displays",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }
                        Switch(
                            checked = AppearancePrefs.themeMode == ThemeMode.AMOLED,
                            onCheckedChange = { enabled ->
                                AppearancePrefs.setThemeMode(
                                    context,
                                    if (enabled) ThemeMode.AMOLED else ThemeMode.DARK
                                )
                            },
                            colors = SwitchDefaults.colors(checkedTrackColor = accent)
                        )
                    }
                }

                Text(
                    "System follows your device settings automatically.",
                    style = CentwiseTypography.Caption,
                    color = textSecondary
                )
            }

            // Accent Palette Sections
            AccentCategorySection(
                title = "Bangladesh Providers",
                options = AccentOptions.bangladeshOptions,
                selectedName = AppearancePrefs.accentName,
                onSelect = { AppearancePrefs.setAccent(context, it) },
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            AccentCategorySection(
                title = "Aesthetic Vibes",
                options = AccentOptions.aestheticOptions,
                selectedName = AppearancePrefs.accentName,
                onSelect = { AppearancePrefs.setAccent(context, it) },
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            AccentCategorySection(
                title = "Modern & Minimal",
                options = AccentOptions.modernOptions,
                selectedName = AppearancePrefs.accentName,
                onSelect = { AppearancePrefs.setAccent(context, it) },
                cardBg = cardBg,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            // Haptics Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Haptic Feedback", style = CentwiseTypography.Body, color = textPrimary)
                    Text(
                        "Subtle vibration on taps and actions",
                        style = CentwiseTypography.Caption,
                        color = textSecondary
                    )
                }
                Switch(
                    checked = AppearancePrefs.hapticsEnabled,
                    onCheckedChange = { AppearancePrefs.setHaptics(context, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = accent)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AccentCategorySection(
    title: String,
    options: List<AccentOption>,
    selectedName: String,
    onSelect: (String) -> Unit,
    cardBg: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
            color = textPrimary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedName.equals(option.name, ignoreCase = true)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .iosBounceClick { onSelect(option.name) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(option.color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = option.name,
                        style = CentwiseTypography.Caption.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        color = if (isSelected) textPrimary else textSecondary
                    )
                    Text(
                        text = option.subtitle,
                        style = CentwiseTypography.Caption.copy(fontSize = 9.sp),
                        maxLines = 1,
                        color = textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppearanceScreenPreview() {
    AppearanceScreen()
}
