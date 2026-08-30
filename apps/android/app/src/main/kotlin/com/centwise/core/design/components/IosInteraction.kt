package com.centwise.core.design.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.centwise.features.settings.AppearancePrefs

/**
 * Universal iOS-style bounce press interaction with tactile spring physics and instant haptics.
 * Replicates SwiftUI ButtonStyle (CentwisePressStyle) and native iOS touch depth across the entire app.
 */
fun Modifier.iosBounceClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.96f,
    pressAlpha: Float = 0.78f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 500f
        ),
        label = "ios_bounce_scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressAlpha else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ios_bounce_alpha"
    )

    this
        .graphicsLayer {
            this.scaleX = animatedScale
            this.scaleY = animatedScale
            this.alpha = animatedAlpha
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                if (AppearancePrefs.hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
                onClick()
            }
        )
}
