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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onBackClick() }
                    .padding(10.dp)
            )
            Text(text = "Budgets", style = CentwiseTypography.Headline, color = textPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Add",
                style = CentwiseTypography.Body,
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showAddSheet = true }
                    .padding(10.dp)
            )
        }

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

            items(budgets) { budget ->
                val pct = if (budget.allocatedAmount > 0) {
                    (budget.spentAmount / budget.allocatedAmount).toFloat()
                } else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .clickable { onBudgetClick(budget) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = budget.categoryName,
                            style = CentwiseTypography.Body,
                            color = textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${CurrencyFormatter.formatBDT(budget.spentAmount, compact = true)} / ${CurrencyFormatter.formatBDT(budget.allocatedAmount, compact = true)}",
                            style = CentwiseTypography.AmountSmall,
                            color = textPrimary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
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
