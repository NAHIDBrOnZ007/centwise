package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

@Composable
fun TransactionRow(
    transaction: TransactionItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
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
    val providerColor = CentwiseColors.providerColor(transaction.paymentMethod)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium))
            .background(cardBg)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(providerColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = providerColor,
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
                    color = textSecondary
                )
                Text(
                    text = " • ${transaction.paymentMethod}",
                    style = CentwiseTypography.Caption,
                    color = providerColor
                )
            }
        }

        // Amount Text
        Text(
            text = "$amountPrefix${CurrencyFormatter.formatBDT(transaction.amount, showSign = false)}",
            style = CentwiseTypography.AmountMedium,
            color = amountColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionRowPreview() {
    TransactionRow(
        transaction = TransactionItem(
            title = "Foodpanda BD",
            amount = 650.0,
            type = TransactionType.EXPENSE,
            category = "Food & Dining",
            paymentMethod = "bKash"
        )
    )
}
