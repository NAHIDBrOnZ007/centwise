package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

@Composable
fun TransactionRow(
    transaction: TransactionItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    showBackground: Boolean = false,
    showChevron: Boolean = true,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
    val amountPrefix = if (isIncome) "+ " else "- "

    val typeIcon = when (transaction.type) {
        TransactionType.INCOME -> Icons.Default.ArrowDownward
        TransactionType.EXPENSE -> Icons.Default.ArrowUpward
        TransactionType.CREDIT -> Icons.Default.CreditCard
        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
    }
    val rowModifier = if (showBackground) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val accent = com.centwise.features.settings.AccentOptions.byName(com.centwise.features.settings.AppearancePrefs.accentName).color

        // Icon Badge (Theme accent style matching iOS)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Category / Method
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.category,
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    maxLines = 1
                )
                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                Text(
                    text = " • ${dateFormat.format(transaction.date)}",
                    style = CentwiseTypography.Caption,
                    color = textSecondary,
                    maxLines = 1
                )
            }
        }

        // Amount Text
        Text(
            text = "$amountPrefix${CurrencyFormatter.formatBDT(transaction.amount, showSign = false)}",
            style = CentwiseTypography.AmountMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = amountColor
        )

        if (showChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionRowPreview() {
    TransactionRow(
        transaction = TransactionItem(
            //noinspection SpellCheckingInspection
            title = "Foodpanda BD",
            amount = 650.0,
            type = TransactionType.EXPENSE,
            category = "Food & Dining",
            //noinspection SpellCheckingInspection
            paymentMethod = "bKash"
        )
    )
}
