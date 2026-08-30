package com.centwise.features.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.components.EmptyStateView
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.repository.TransactionRepository
import com.centwise.data.models.BudgetItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs

@Composable
fun BudgetListScreen(
    onBackClick: () -> Unit = {},
    onBudgetClick: (BudgetItem) -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = TransactionRepository.shared
    val budgets by repository.budgets.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val trackBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg

    val totalBudget = budgets.sumOf { it.allocatedAmount }
    val totalSpent = budgets.sumOf { it.spentAmount }
    val totalPct = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Budgets",
                style = CentwiseTypography.Headline,
                color = textPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .iosBounceClick { showAddSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Add",
                        style = CentwiseTypography.Headline.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        if (budgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    title = "No Budgets Yet",
                    description = "Create a budget to track your spending by category.",
                    buttonText = "Create Budget",
                    onButtonClick = { showAddSheet = true },
                    isDark = isDark
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Overview Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Monthly Total Budget", style = CentwiseTypography.Subheadline, color = textSecondary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = CurrencyFormatter.formatBDT(totalSpent),
                                style = CentwiseTypography.HeroAmount,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "of ${CurrencyFormatter.formatBDT(totalBudget, compact = true)}",
                                style = CentwiseTypography.Body,
                                color = textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusCapsule))
                                .background(trackBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(totalPct.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusCapsule))
                                    .background(if (totalSpent > totalBudget) CentwiseColors.ExpenseRed else accent)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${(totalPct * 100).toInt()}% Used",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "${CurrencyFormatter.formatBDT(maxOf(totalBudget - totalSpent, 0.0), compact = true)} Remaining",
                                style = CentwiseTypography.Caption,
                                color = CentwiseColors.IncomeGreen
                            )
                        }
                    }
                }

                item {
                    Text("Category Budgets", style = CentwiseTypography.Headline, color = textPrimary)
                }

                items(budgets, key = { it.id }) { budget ->
                    val pct = if (budget.allocatedAmount > 0) {
                        (budget.spentAmount / budget.allocatedAmount).toFloat()
                    } else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                            .background(cardBg)
                            .iosBounceClick { onBudgetClick(budget) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Clean unboxed category icon matching iOS budgetCard
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.iconFor(budget.categoryName),
                                    contentDescription = budget.categoryName,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Text(
                                text = budget.categoryName,
                                style = CentwiseTypography.Headline.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                                color = textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${CurrencyFormatter.formatBDT(budget.spentAmount, compact = true)} / ${CurrencyFormatter.formatBDT(budget.allocatedAmount, compact = true)}",
                                style = CentwiseTypography.Caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                color = textSecondary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = textSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Progress Bar matching iOS budgetCard
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusCapsule))
                                .background(trackBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pct.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusCapsule))
                                    .background(if (budget.spentAmount > budget.allocatedAmount) CentwiseColors.ExpenseRed else accent)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${(pct * 100).toInt()}% used",
                                style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                                color = textSecondary
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            val isOver = budget.spentAmount > budget.allocatedAmount
                            val diff = if (isOver) budget.spentAmount - budget.allocatedAmount else budget.allocatedAmount - budget.spentAmount
                            Text(
                                "${CurrencyFormatter.formatBDT(diff, compact = true)} ${if (isOver) "over budget" else "remaining"}",
                                style = CentwiseTypography.Caption.copy(fontSize = 11.sp),
                                color = if (isOver) CentwiseColors.ExpenseRed else textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditBudgetSheet(
            onDismiss = { showAddSheet = false },
            onSave = { budget ->
                TransactionRepository.shared.addBudget(budget)
                showAddSheet = false
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetListScreenPreview() {
    BudgetListScreen()
}
