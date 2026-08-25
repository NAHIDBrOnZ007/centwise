package com.centwise.features.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

// MARK: - Summary Card

@Composable
fun AnalyticsSummaryCard(
    spent: Double,
    income: Double,
    transactionCount: Int,
    topCategoryName: String?,
    periodDays: Int = 30,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val dividerColor = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)

    val dailyAverage = spent / maxOf(periodDays, 1)
    val savingsRate = if (income > 0) maxOf((income - spent) / income, 0.0) else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SPENT THIS PERIOD", style = CentwiseTypography.Caption, color = textSecondary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = CurrencyFormatter.formatBDT(spent),
                style = CentwiseTypography.HeroAmount,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(savingsRate * 100).toInt()}% saved",
                style = CentwiseTypography.Caption,
                color = if (income >= spent) CentwiseColors.IncomeGreen else CentwiseColors.ExpenseRed
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryStat(
                title = "TRANSACTIONS",
                value = "$transactionCount",
                modifier = Modifier.weight(1f),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent
            )
            SummaryStat(
                title = "DAILY AVG",
                value = CurrencyFormatter.formatBDT(dailyAverage, compact = true),
                modifier = Modifier.weight(1f),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent
            )
            SummaryStat(
                title = "TOP CATEGORY",
                value = topCategoryName ?: "—",
                modifier = Modifier.weight(1.2f),
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                accent = accent
            )
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

    val sorted = slices.sortedByDescending { it.value }
    val total = sorted.sumOf { it.value }

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

    val maxValue = maxOf(points.maxOfOrNull { it.value } ?: 0.0, 1.0)

    var appeared by remember { mutableStateOf(false) }
    val barScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "trend"
    )
    LaunchedEffect(Unit) { appeared = true }

    var trendLabel: String? = null
    var trendUp = false
    if (points.size >= 2) {
        val last = points.last().value
        val previous = points[points.size - 2].value
        if (previous > 0) {
            val change = (last - previous) / previous
            if (change > 0.05) {
                trendLabel = "${(change * 100).toInt()}%"
                trendUp = true
            } else if (change < -0.05) {
                trendLabel = "${(kotlin.math.abs(change) * 100).toInt()}%"
                trendUp = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
            .background(cardBg)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Spending Trends",
                style = CentwiseTypography.Headline,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (trendLabel != null) {
                Text(
                    text = if (trendUp) "▲ $trendLabel" else "▼ $trendLabel",
                    style = CentwiseTypography.Caption,
                    color = if (trendUp) CentwiseColors.ExpenseRed else CentwiseColors.IncomeGreen
                )
            }
        }

        if (points.isEmpty()) {
            Text(
                "No spending data for this period",
                style = CentwiseTypography.Subheadline,
                color = textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                points.forEach { point ->
                    val ratio = if (maxValue > 0) (point.value / maxValue).toFloat().coerceIn(0f, 1f) else 0f
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (point.value > 0) CurrencyFormatter.formatBDT(point.value, compact = true) else "-",
                            style = CentwiseTypography.Caption.copy(fontSize = 10.sp),
                            color = if (point.value > 0) textPrimary else textSecondary.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight((ratio * barScale).coerceAtLeast(0.04f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (ratio > 0.01f) accent else Color.Transparent
                                    )
                            )
                        }
                        Text(
                            text = point.label,
                            style = CentwiseTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = textSecondary,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
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
