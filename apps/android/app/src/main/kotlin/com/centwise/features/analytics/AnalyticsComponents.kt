package com.centwise.features.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import kotlin.math.min

data class CategorySlice(val name: String, val value: Double, val color: Color)
data class TrendPoint(val label: String, val value: Double)

@Composable
fun AnalyticsSummaryCard(
    spent: Double,
    income: Double,
    transactionCount: Int,
    topCategoryName: String?,
    periodDays: Int = 30,
    isDark: Boolean = isSystemInDarkTheme()
) {
    var animateValues by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateValues = true }
    val animatedSpent by animateFloatAsState(
        targetValue = if (animateValues) spent.toFloat() else 0f,
        animationSpec = tween(450),
        label = "analytics spent"
    )
    val animatedIncome by animateFloatAsState(
        targetValue = if (animateValues) income.toFloat() else 0f,
        animationSpec = tween(450),
        label = "analytics income"
    )
    val animatedTransactionCount by animateIntAsState(
        targetValue = if (animateValues) transactionCount else 0,
        animationSpec = tween(450),
        label = "analytics transaction count"
    )
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    val net = animatedIncome - animatedSpent
    val dailyAverage = animatedSpent / maxOf(periodDays, 1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Spent this period + big amount
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "SPENT THIS PERIOD",
                style = CentwiseTypography.Caption,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                fontSize = 11.sp
            )

            Text(
                text = CurrencyFormatter.formatBDT(animatedSpent.toDouble()),
                style = CentwiseTypography.HeroAmount.copy(fontSize = 32.sp),
                color = textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        // 3 Columns: Income, Expenses, Net
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("INCOME", style = CentwiseTypography.Caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text(CurrencyFormatter.formatBDT(animatedIncome.toDouble(), compact = true), style = CentwiseTypography.Headline, fontWeight = FontWeight.Bold, color = CentwiseColors.IncomeGreen, fontSize = 15.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("EXPENSES", style = CentwiseTypography.Caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text(CurrencyFormatter.formatBDT(animatedSpent.toDouble(), compact = true), style = CentwiseTypography.Headline, fontWeight = FontWeight.Bold, color = CentwiseColors.ExpenseRed, fontSize = 15.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("NET", style = CentwiseTypography.Caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text("${if (net >= 0) "+" else ""}${CurrencyFormatter.formatBDT(net.toDouble(), compact = true)}", style = CentwiseTypography.Headline, fontWeight = FontWeight.Bold, color = if (net >= 0) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed, fontSize = 15.sp)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        // 3 Columns: Transactions, Daily Avg, Top Category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TRANSACTIONS", style = CentwiseTypography.Caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text("$animatedTransactionCount", style = CentwiseTypography.Headline, fontWeight = FontWeight.SemiBold, color = textPrimary, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("DAILY AVG", style = CentwiseTypography.Caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text(CurrencyFormatter.formatBDT(dailyAverage.toDouble(), compact = true), style = CentwiseTypography.Headline, fontWeight = FontWeight.SemiBold, color = textPrimary, fontSize = 14.sp)
            }
            Column(modifier = Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("TOP CATEGORY", style = CentwiseTypography.Caption, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                Text(topCategoryName ?: "Others", style = CentwiseTypography.Headline, fontWeight = FontWeight.SemiBold, color = textPrimary, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SummaryStat(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, style = CentwiseTypography.Caption, color = textSecondary, maxLines = 1)
        }
        Text(
            value,
            style = CentwiseTypography.AmountSmall,
            color = textPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// MARK: - Category Pie (Donut) Chart

@Composable
fun CategoryPieChart(
    slices: List<CategorySlice>,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    val sorted = remember(slices) { slices.sortedByDescending { it.value } }
    val total = remember(sorted) { sorted.sumOf { it.value } }

    var appeared by remember { mutableStateOf(false) }
    val sweepScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "pie"
    )
    LaunchedEffect(Unit) { appeared = true }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Category Breakdown", style = CentwiseTypography.Headline, color = textPrimary)

        if (total <= 0) {
            Text(
                "No category data available",
                style = CentwiseTypography.Subheadline,
                color = textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val thickness = size.minDimension * 0.26f
                        val diameter = size.minDimension - thickness
                        val arcSize = Size(diameter, diameter)
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                        sorted.forEach { slice ->
                            val sweep = (slice.value / total * 360.0).toFloat() * sweepScale
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = thickness)
                            )
                            startAngle += sweep
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            CurrencyFormatter.formatBDT(total, compact = true),
                            style = CentwiseTypography.AmountMedium,
                            color = textPrimary
                        )
                        Text("Total", style = CentwiseTypography.Caption, color = textSecondary)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sorted.take(5).forEach { slice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                slice.name,
                                style = CentwiseTypography.Caption,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${(slice.value / total * 100).toInt()}%",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }
                    }

                    if (sorted.size > 5) {
                        Text("+ ${sorted.size - 5} more", style = CentwiseTypography.Caption, color = textSecondary)
                    }
                }
            }
        }
    }
}

// MARK: - Spending Trends Chart

@Composable
fun SpendingTrendsChart(
    points: List<TrendPoint>,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val gridLineColor = if (isDark) Color(0x14FFFFFF) else Color(0x0F000000)

    val rawMax = remember(points) { points.maxOfOrNull { it.value } ?: 0.0 }
    val niceMax = remember(rawMax) { calculateNiceMax(rawMax) }

    var appeared by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "trend"
    )
    LaunchedEffect(Unit) { appeared = true }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Trend direction calculation matching iOS 1:1
    val (trendLabel, trendUp) = remember(points) {
        var label: String? = null
        var up = false
        if (points.size >= 2) {
            val last = points.last().value
            val previous = points[points.size - 2].value
            if (previous > 0) {
                val change = (last - previous) / previous
                if (change > 0.05) {
                    label = "${(change * 100).toInt()}%"
                    up = true
                } else if (change < -0.05) {
                    label = "${(kotlin.math.abs(change) * 100).toInt()}%"
                    up = false
                }
            }
        }
        Pair(label, up)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header matching iOS 1:1
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spending Trends",
                style = CentwiseTypography.Headline,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )

            if (selectedIndex != null && selectedIndex!! in points.indices) {
                val selected = points[selectedIndex!!]
                Text(
                    text = "${selected.label}: ${CurrencyFormatter.formatBDT(selected.value)}",
                    style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = accent
                )
            } else if (trendLabel != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (trendUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (trendUp) CentwiseColors.ExpenseRed else CentwiseColors.IncomeGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = trendLabel,
                        style = CentwiseTypography.Caption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = if (trendUp) CentwiseColors.ExpenseRed else CentwiseColors.IncomeGreen
                    )
                }
            }
        }

        if (points.isEmpty()) {
            Text(
                text = "No spending data for this period",
                style = CentwiseTypography.Subheadline,
                color = textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = CentwiseSpacing.md),
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Plot area and X-Axis
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Plot area with 3 horizontal grid lines and vertical bars
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                    ) {
                        // Horizontal grid lines matching iOS AxisMarks / AxisGridLine
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(gridLineColor)
                                )
                            }
                        }

                        // Vertical bars matching iOS BarMark (accent colored, cornerRadius 6, no gray box)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            points.forEachIndexed { index, point ->
                                val ratio = if (niceMax > 0) (point.value / niceMax).toFloat().coerceIn(0f, 1f) else 0f
                                val barHeight = 96.dp * (ratio * barScale)
                                val barWidth = when {
                                    points.size <= 3 -> 24.dp
                                    points.size <= 6 -> 18.dp
                                    else -> 14.dp
                                }
                                val isSelected = selectedIndex == index

                                Box(
                                    modifier = Modifier
                                        .width(barWidth)
                                        .height(if (point.value > 0) barHeight.coerceAtLeast(4.dp) else 0.dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (isSelected) accent.copy(alpha = 0.8f) else accent)
                                        .clickable {
                                            selectedIndex = if (selectedIndex == index) null else index
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // X-Axis Labels (matching iOS AxisValueLabel)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        points.forEachIndexed { index, point ->
                            val labelWidth = when {
                                points.size <= 3 -> 40.dp
                                points.size <= 6 -> 32.dp
                                else -> 24.dp
                            }
                            val isSelected = selectedIndex == index
                            Text(
                                text = point.label,
                                style = CentwiseTypography.Caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) accent else textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(labelWidth)
                                    .clickable {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Y-Axis Labels aligned with the 3 grid lines (matching iOS AxisValueLabel)
                Column(
                    modifier = Modifier
                        .height(96.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = CurrencyFormatter.formatBDT(niceMax, compact = true),
                        style = CentwiseTypography.Caption.copy(fontSize = 10.sp),
                        color = textSecondary
                    )
                    Text(
                        text = CurrencyFormatter.formatBDT(niceMax / 2.0, compact = true),
                        style = CentwiseTypography.Caption.copy(fontSize = 10.sp),
                        color = textSecondary
                    )
                    Text(
                        text = "0",
                        style = CentwiseTypography.Caption.copy(fontSize = 10.sp),
                        color = textSecondary
                    )
                }
            }
        }
    }
}

private fun calculateNiceMax(rawMax: Double): Double {
    if (rawMax <= 0.0) return 1000.0
    val exponent = kotlin.math.floor(kotlin.math.log10(rawMax))
    val power = Math.pow(10.0, exponent)
    val fraction = rawMax / power
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceFraction * power
}

@Preview(showBackground = true)
@Composable
fun AnalyticsComponentsPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnalyticsSummaryCard(spent = 24500.0, income = 65000.0, transactionCount = 42, topCategoryName = "Food & Dining")
        CategoryPieChart(
            slices = listOf(
                CategorySlice("Food", 8000.0, Color(0xFFF97316)),
                CategorySlice("Transport", 5000.0, Color(0xFF06B6D4)),
                CategorySlice("Shopping", 3000.0, Color(0xFFEC4899))
            )
        )
        SpendingTrendsChart(
            points = listOf(
                TrendPoint("Mar", 18000.0),
                TrendPoint("Apr", 22000.0),
                TrendPoint("May", 19500.0)
            )
        )
    }
}
