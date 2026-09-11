package com.centwise.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun LanguagePickerScreen(
    onBackClick: () -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val currentLang = LanguagePrefs.selectedLanguage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = if (LanguagePrefs.isBengali) "ভাষা নির্বাচন" else "Language",
                style = CentwiseTypography.Title2,
                color = textPrimary
            )
        }

        Text(
            text = if (LanguagePrefs.isBengali) "আপনার পছন্দের ভাষা নির্বাচন করুন" else "Choose your preferred language for the app",
            style = CentwiseTypography.Caption,
            color = textSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Options List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                .background(cardBg)
        ) {
            AppLanguage.entries.forEachIndexed { index, lang ->
                val isSelected = lang == currentLang

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isSelected) {
                                LanguagePrefs.setLanguage(context, lang)
                                if (AppearancePrefs.hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = lang.flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lang.titleNative,
                            style = CentwiseTypography.Headline.copy(fontSize = 16.sp),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lang.titleEnglish,
                            style = CentwiseTypography.Caption,
                            color = textSecondary
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (index < AppLanguage.entries.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 54.dp)
                            .height(0.6.dp)
                            .background(if (isDark) Color(0x14FFFFFF) else Color(0x0F000000))
                    )
                }
            }
        }
    }
}
