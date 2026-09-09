package com.centwise.core.design.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

/**
 * Universal modifier that clears focus and dismisses the software keyboard when
 * tapping outside of any active text field or input control.
 *
 * Child clickable views (buttons, chips, tabs) consume their own tap events, so
 * their actions fire unimpeded, while taps on empty space, backgrounds, margins,
 * or card paddings automatically hide the keyboard.
 */
fun Modifier.clearFocusOnTapOutside(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    pointerInput(Unit) {
        detectTapGestures(
            onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
    }
}

/**
 * Automatically dismisses the software keyboard as soon as vertical scrolling begins.
 */
@Composable
fun DismissKeyboardOnScroll(scrollState: ScrollState) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}

/**
 * Automatically dismisses the software keyboard as soon as list scrolling begins.
 */
@Composable
fun DismissKeyboardOnScroll(listState: LazyListState) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
}
