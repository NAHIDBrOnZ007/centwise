package com.centwise.core.design.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography

/**
 * Idiomatic Jetpack Compose Empty State View matching iOS Centwise empty state layout 1:1.
 */
@Composable
fun EmptyStateView(
    title: String = "No transactions yet",
    description: String = "Add a transaction or configure SMS capture to get started.",
    icon: ImageVector = Icons.Default.Inbox,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(44.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            style = CentwiseTypography.Headline,
            color = textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = description,
            style = CentwiseTypography.Subheadline,
            color = textSecondary,
            textAlign = TextAlign.Center
        )

        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            CentwiseButton(
                title = buttonText,
                icon = Icons.Default.Add,
                onClick = onButtonClick,
                isDark = isDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateViewPreview() {
    EmptyStateView(
        title = "No Transactions Yet",
        description = "Add a transaction or configure SMS capture to get started."
    )
}
