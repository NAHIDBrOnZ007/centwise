package com.centwise.features.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.CategoryOption
import com.centwise.data.models.TransactionType
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

private val timePeriods = listOf("7D", "30D", "90D", "1 Year")

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val transactionCount by viewModel.transactionCount.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val fieldBg = if (isDark) Color(0x1FFFFFFF) else Color(0x0A000000)
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    val periodDays = when (selectedPeriod) {
        "This Month" -> 30
        "Last Month" -> 30
        "3 Months" -> 90
        "6 Months" -> 180
        else -> 365
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Analytics & Trends",
                style = CentwiseTypography.LargeTitle,
                color = textPrimary
            )
        }

        // 1. Period Filter Pills (Horizontal Scroll)
        item {
            val periods = listOf("This Month", "Last Month", "3 Months", "6 Months", "All Time")
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(periods) { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) accent else if (isDark) Color(0xFF242426) else Color(0xFFEEEEEE))
                            .clickable { viewModel.setPeriod(period) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = period,
                            style = CentwiseTypography.Caption,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Type Filter Pills (All, Debit, Credit)
        item {
            val types = listOf("All", "Debit", "Credit")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                types.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) accent else if (isDark) Color(0xFF242426) else Color(0xFFEEEEEE))
                            .clickable { viewModel.setTypeFilter(type) }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = type,
                            style = CentwiseTypography.Caption,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Period Summary Hero Card (with Accent Highlights)
        item {
            AnalyticsSummaryCard(
                spent = totalExpense,
                income = totalIncome,
                transactionCount = transactionCount,
                topCategoryName = categoryBreakdown.firstOrNull()?.category,
                periodDays = periodDays,
                isDark = isDark
            )
        }

        // 3. Cashflow Progress Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash Flow Overview",
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )
                    Text(
                        text = selectedPeriod,
                        style = CentwiseTypography.Caption,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ratio Bar
                val total = totalIncome + totalExpense
                val incomeRatio = if (total > 0) (totalIncome / total).toFloat() else 0.5f
                val expenseRatio = if (total > 0) (totalExpense / total).toFloat() else 0.5f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CentwiseColors.IncomeGreen))
                            Text("Total Inflow", style = CentwiseTypography.Caption, color = textSecondary)
                        }
                        Text(
                            text = CurrencyFormatter.formatBDT(totalIncome, compact = true),
                            style = CentwiseTypography.AmountMedium,
                            color = CentwiseColors.IncomeGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CentwiseColors.ExpenseRed))
                            Text("Total Outflow", style = CentwiseTypography.Caption, color = textSecondary)
                        }
                        Text(
                            text = CurrencyFormatter.formatBDT(totalExpense, compact = true),
                            style = CentwiseTypography.AmountMedium,
                            color = CentwiseColors.ExpenseRed
                        )
                    }
                }
            }
        }

        // 4. Category Pie Chart
        item {
            CategoryPieChart(
                slices = categoryBreakdown.mapIndexed { index, item ->
                    CategorySlice(
                        name = item.category,
                        value = item.totalAmount,
                        color = CategoryOption.defaults
                            .firstOrNull { it.name.equals(item.category, ignoreCase = true) }?.color
                            ?: CategorySliceColors.palette[index % CategorySliceColors.palette.size]
                    )
                }
            )
        }

        // 5. Spending Trends (Last 6 Months with Dynamic Accent Bars)
        item {
            val txs by FakeTransactionRepository.shared.transactions.collectAsState()
            SpendingTrendsChart(points = monthlyTrendPoints(txs), isDark = isDark)
        }

        // 6. Spending by Category (Rich with Icons & Progress Bars)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                    .background(cardBg)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Spending by Category",
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )

                if (categoryBreakdown.isEmpty()) {
                    Text(
                        "No category expenses recorded for this period",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary
                    )
                } else {
                    categoryBreakdown.forEachIndexed { index, item ->
                        val catColor = CategoryOption.defaults
                            .firstOrNull { it.name.equals(item.category, ignoreCase = true) }?.color
                            ?: CategorySliceColors.palette[index % CategorySliceColors.palette.size]

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(catColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = item.category,
                                        style = CentwiseTypography.Body,
                                        color = textPrimary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = CurrencyFormatter.formatBDT(item.totalAmount, compact = true),
                                        style = CentwiseTypography.AmountSmall,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "${(item.percentage * 100).toInt()}%",
                                        style = CentwiseTypography.Caption,
                                        color = textSecondary
                                    )
                                }
                            }

                            // Progress Track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(fieldBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(item.percentage.toFloat().coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(catColor)
                                )
                            }
                        }

                        if (index < categoryBreakdown.size - 1) {
                            HorizontalDivider(color = dividerColor)
                        }
                    }
                }
            }
        }

        // 7. Top Spending Merchants (with Accent Rank Badges)
        item {
            if (topMerchants.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Top Merchants",
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )

                    topMerchants.forEachIndexed { index, merchantItem ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                                        color = accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column {
                                    Text(
                                        text = merchantItem.merchantName,
                                        style = CentwiseTypography.Body,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "${merchantItem.transactionCount} transaction${if (merchantItem.transactionCount == 1) "" else "s"}",
                                        style = CentwiseTypography.Caption,
                                        color = textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = CurrencyFormatter.formatBDT(merchantItem.totalAmount, compact = true),
                                style = CentwiseTypography.AmountSmall,
                                color = textPrimary
                            )
                        }

                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnalyticsScreenPreview() {
    AnalyticsScreen()
}

private object CategorySliceColors {
    val palette = listOf(
        Color(0xFFF97316),
        Color(0xFF06B6D4),
        Color(0xFFEC4899),
        Color(0xFFEAB308),
        Color(0xFF8B5CF6),
        Color(0xFF10B981),
        Color(0xFF007AFF),
        Color(0xFFEF4444)
    )
}

private fun monthlyTrendPoints(
    transactions: List<com.centwise.data.models.TransactionItem>
): List<TrendPoint> {
    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val now = java.util.Calendar.getInstance()

    return (5 downTo 0).map { monthsBack ->
        val monthDate = (now.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.MONTH, -monthsBack)
        }

        val total = transactions
            .filter { transaction ->
                if (transaction.type != TransactionType.EXPENSE) return@filter false
                val txCalendar = java.util.Calendar.getInstance().apply {
                    timeInMillis = transaction.timestamp
                }
                txCalendar.get(java.util.Calendar.MONTH) == monthDate.get(java.util.Calendar.MONTH) &&
                        txCalendar.get(java.util.Calendar.YEAR) == monthDate.get(java.util.Calendar.YEAR)
            }
            .sumOf { it.amount }

        TrendPoint(label = monthLabels[monthDate.get(java.util.Calendar.MONTH)], value = total)
    }
}
