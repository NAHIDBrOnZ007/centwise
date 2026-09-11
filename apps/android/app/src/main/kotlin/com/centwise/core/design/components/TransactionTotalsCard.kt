package com.centwise.core.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun TransactionTotalsCard(
    income: Double,
    expense: Double,
    net: Double,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var animateValues by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateValues = true }
    val animatedIncome by animateFloatAsState(
        targetValue = if (animateValues) income.toFloat() else 0f,
        animationSpec = tween(450),
        label = "transaction income"
    )
    val animatedExpense by animateFloatAsState(
        targetValue = if (animateValues) expense.toFloat() else 0f,
        animationSpec = tween(450),
        label = "transaction expense"
    )
    val animatedNet by animateFloatAsState(
        targetValue = if (animateValues) net.toFloat() else 0f,
        animationSpec = tween(450),
        label = "transaction net"
    )
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(16.dp)),
        color = cardBg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Income Column
            TotalItem(
                label = "↓ Income",
                value = CurrencyFormatter.format(animatedIncome.toDouble()),
                color = CentwiseColors.IncomeGreen,
                textSecondary = textSecondary
            )

            // 2. Expenses Column
            TotalItem(
                label = "↑ Expenses",
                value = CurrencyFormatter.format(animatedExpense.toDouble()),
                color = CentwiseColors.ExpenseRed,
                textSecondary = textSecondary
            )

            // 3. Net Column
            TotalItem(
                label = if (animatedNet >= 0) "✓ Net" else "✗ Net",
                value = (if (animatedNet >= 0) "+" else "") + CurrencyFormatter.format(animatedNet.toDouble()),
                color = if (animatedNet >= 0) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun TotalItem(
    label: String,
    value: String,
    color: Color,
    textSecondary: Color
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = CentwiseTypography.Caption,
            color = textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = CentwiseTypography.Headline,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}
