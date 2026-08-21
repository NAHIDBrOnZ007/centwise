package com.centwise.features.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Analytics",
                style = CentwiseTypography.LargeTitle,
                color = textPrimary
            )
        }

        // Cashflow Progress Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Monthly Cashflow",
                    style = CentwiseTypography.Subheadline,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Ratio Bar
                val total = totalIncome + totalExpense
                val incomeRatio = if (total > 0) (totalIncome / total).toFloat() else 0.5f
                val expenseRatio = if (total > 0) (totalExpense / total).toFloat() else 0.5f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(incomeRatio.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(CentwiseColors.IncomeGreen)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .weight(expenseRatio.coerceAtLeast(0.01f))
                            .fillMaxHeight()
                            .background(CentwiseColors.ExpenseRed)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Inflow: ${CurrencyFormatter.formatBDT(totalIncome)}",
                        style = CentwiseTypography.AmountSmall,
                        color = CentwiseColors.IncomeGreen
                    )
                    Text(
                        text = "Outflow: ${CurrencyFormatter.formatBDT(totalExpense)}",
                        style = CentwiseTypography.AmountSmall,
                        color = CentwiseColors.ExpenseRed
                    )
                }
            }
        }

        // Category Breakdown Header
        item {
            Text(
                text = "Spending by Category",
                style = CentwiseTypography.Title2,
                color = textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Category Rows
        items(categoryBreakdown.entries.toList(), key = { it.key }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium))
                    .background(cardBg)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.key,
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )
                Text(
                    text = CurrencyFormatter.formatBDT(entry.value),
                    style = CentwiseTypography.AmountMedium,
                    color = textPrimary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen()
}
