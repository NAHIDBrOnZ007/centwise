package com.centwise.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun SpendingSummaryCard(
    monthlyExpense: Double,
    monthlyIncome: Double,
    monthlySaved: Double,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(18.dp)
    ) {
        // Hero: Spent this month
        Text(
            text = "Spent this month",
            style = CentwiseTypography.Subheadline,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = CurrencyFormatter.formatBDT(monthlyExpense, showSign = false),
            style = CentwiseTypography.HeroAmount,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(
            color = if (isDark) Color(0x14FFFFFF) else Color(0x0D000000),
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 3-Column Stats Breakdown with Vertical Colored Stripes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Income
            StatColumn(
                label = "Income",
                amount = monthlyIncome,
                stripeColor = CentwiseColors.IncomeGreen,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            // 2. Expenses
            StatColumn(
                label = "Expenses",
                amount = monthlyExpense,
                stripeColor = CentwiseColors.ExpenseRed,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )

            // 3. Saved
            StatColumn(
                label = "Saved",
                amount = monthlySaved,
                stripeColor = CentwiseColors.SavedTeal,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    amount: Double,
    stripeColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(stripeColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = CurrencyFormatter.formatBDT(amount, showSign = false, compact = true),
                style = CentwiseTypography.AmountSmall,
                color = textPrimary
            )
            Text(
                text = label,
                style = CentwiseTypography.Caption,
                color = textSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpendingSummaryCardPreview() {
    SpendingSummaryCard(
        monthlyExpense = 14250.0,
        monthlyIncome = 85000.0,
        monthlySaved = 70750.0
    )
}
