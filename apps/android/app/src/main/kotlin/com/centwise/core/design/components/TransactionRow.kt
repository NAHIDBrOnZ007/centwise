package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

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
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
    val amountPrefix = if (isIncome) "+ " else "- "

    val rowModifier = if (showBackground) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .iosBounceClick { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    } else {
        modifier
            .fillMaxWidth()
            .iosBounceClick { onClick() }
            .padding(vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clean unboxed category icon tinted with active theme accent (Matching iOS TransactionRow)
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = CategoryIconHelper.iconFor(transaction.category),
                contentDescription = transaction.category,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Category • Date
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.title,
                style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.category,
                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1
                )
                val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                Text(
                    text = " • ${dateFormat.format(transaction.date)}",
                    style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                    color = textSecondary,
                    maxLines = 1
                )
            }
        }

        // Amount Text
        Text(
            text = "$amountPrefix${CurrencyFormatter.formatBDT(transaction.amount, showSign = false)}",
            style = CentwiseTypography.AmountSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            color = amountColor
        )

        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
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
