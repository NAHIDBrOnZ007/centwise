package com.centwise.core.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AppearancePrefs

/**
 * Top Navigation Bar for Modal Sheets featuring tactile spring-press animations,
 * pill capsule shapes, and instant interactive haptic feedback matching native iOS modals.
 */
@Composable
fun ModalSheetTopBar(
    title: String,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveEnabled: Boolean = true,
    cancelLabel: String = "Cancel",
    saveLabel: String = "Save",
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current

    // 1. Cancel Button Interactive Press Animation
    val cancelInteraction = remember { MutableInteractionSource() }
    val isCancelPressed by cancelInteraction.collectIsPressedAsState()
    val cancelScale by animateFloatAsState(
        targetValue = if (isCancelPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 500f
        ),
        label = "cancel_scale"
    )
    val cancelAlpha by animateFloatAsState(
        targetValue = if (isCancelPressed) 0.72f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cancel_alpha"
    )
    val cancelBg by animateColorAsState(
        targetValue = when {
            isCancelPressed -> if (isDark) Color(0x38FFFFFF) else Color(0x22000000)
            isDark -> Color(0x1FFFFFFF)
            else -> Color(0x12000000)
        },
        animationSpec = spring(),
        label = "cancel_bg"
    )

    // 2. Save Button Interactive Press Animation
    val saveInteraction = remember { MutableInteractionSource() }
    val isSavePressed by saveInteraction.collectIsPressedAsState()
    val saveScale by animateFloatAsState(
        targetValue = if (isSavePressed && saveEnabled) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 500f
        ),
        label = "save_scale"
    )
    val saveAlpha by animateFloatAsState(
        targetValue = when {
            !saveEnabled -> 0.40f
            isSavePressed -> 0.80f
            else -> 1.0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "save_alpha"
    )
    val saveBg by animateColorAsState(
        targetValue = when {
            !saveEnabled -> accent.copy(alpha = 0.35f)
            isSavePressed -> accent.copy(alpha = 0.85f)
            else -> accent
        },
        animationSpec = spring(),
        label = "save_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel Capsule Pill Button with Tactile Physics & Haptics
        Surface(
            shape = CircleShape,
            color = cancelBg,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = cancelScale
                    scaleY = cancelScale
                    alpha = cancelAlpha
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = cancelInteraction,
                    indication = null,
                    onClick = {
                        if (AppearancePrefs.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onCancel()
                    }
                )
        ) {
            Text(
                text = cancelLabel,
                style = CentwiseTypography.Headline.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
            )
        }

        // Sheet Title
        Text(
            text = title,
            style = CentwiseTypography.Headline,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary
        )

        // Save / Action Capsule Pill Button with Tactile Physics & Haptics
        Surface(
            shape = CircleShape,
            color = saveBg,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = saveScale
                    scaleY = saveScale
                    alpha = saveAlpha
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = saveInteraction,
                    indication = null,
                    enabled = saveEnabled,
                    onClick = {
                        if (AppearancePrefs.hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        onSave()
                    }
                )
        ) {
            Text(
                text = saveLabel,
                style = CentwiseTypography.Headline.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (saveEnabled) Color.White else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
            )
        }
    }
}
