package com.centwise.core.design.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
    OUTLINE
}

@Composable
fun CentwiseButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    isFullWidth: Boolean = false,
    enabled: Boolean = true,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )

    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.78f else if (!enabled) 0.38f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_alpha"
    )

    val bgColor = when (variant) {
        ButtonVariant.PRIMARY -> accent
        ButtonVariant.SECONDARY -> if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
        ButtonVariant.DESTRUCTIVE -> CentwiseColors.ExpenseRed
        ButtonVariant.OUTLINE -> Color.Transparent
    }

    val fgColor = when (variant) {
        ButtonVariant.PRIMARY, ButtonVariant.DESTRUCTIVE -> Color.White
        ButtonVariant.SECONDARY -> if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
        ButtonVariant.OUTLINE -> accent
    }

    val border = if (variant == ButtonVariant.OUTLINE) {
        BorderStroke(1.5.dp, accent)
    } else null

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = border,
        modifier = modifier
            .then(if (isFullWidth) Modifier.fillMaxWidth() else Modifier)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = pressAlpha
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    if (AppearancePrefs.hapticsEnabled) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    }
                    onClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fgColor,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = CentwiseTypography.Headline,
                fontWeight = FontWeight.SemiBold,
                color = fgColor,
                fontSize = 15.sp
            )
        }
    }
}
