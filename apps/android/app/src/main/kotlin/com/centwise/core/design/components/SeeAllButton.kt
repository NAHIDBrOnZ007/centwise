package com.centwise.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.LanguagePrefs

/**
 * Unified pill-style action button used in section headers across screens
 * (e.g. "See all" on HomeScreen and GroupScreen).
 */
@Composable
fun SeeAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = if (LanguagePrefs.isBengali) "সব দেখুন" else "See all",
    accentColor: Color = AccentOptions.byName(AppearancePrefs.accentName).color
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = CircleShape,
        color = accentColor.copy(alpha = 0.12f),
        modifier = modifier
            .clip(CircleShape)
            .iosBounceClick {
                onClick()
                if (AppearancePrefs.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SeeAllButtonPreview() {
    SeeAllButton(onClick = {})
}
