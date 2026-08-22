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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.BudgetItem
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import java.util.Calendar

@Composable
fun BudgetDetailScreen(
    budget: BudgetItem,
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = FakeTransactionRepository.shared
    val transactions by repository.transactions.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val trackBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg

    val pct = if (budget.allocatedAmount > 0) {
        (budget.spentAmount / budget.allocatedAmount).toFloat()
    } else 0f
    val isOverBudget = budget.spentAmount > budget.allocatedAmount

    val categoryTransactions = transactions.filter {
        it.category.equals(budget.categoryName, ignoreCase = true) &&
                it.type == com.centwise.data.models.TransactionType.EXPENSE
    }

    val daysLeft = run {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        daysInMonth - today + 1
    }
    val remaining = maxOf(budget.allocatedAmount - budget.spentAmount, 0.0)
    val dailyAllowance = remaining / maxOf(daysLeft, 1)

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
            Text(
                text = budget.categoryName,
                style = CentwiseTypography.Headline,
                color = textPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showEditSheet = true }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = textPrimary)
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = CentwiseColors.ExpenseRed
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Progress Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isOverBudget) "Over budget" else "On track",
                        style = CentwiseTypography.Caption,
                        color = if (isOverBudget) CentwiseColors.ExpenseRed else CentwiseColors.IncomeGreen
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = CurrencyFormatter.formatBDT(budget.spentAmount),
                            style = CentwiseTypography.HeroAmount,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "of ${CurrencyFormatter.formatBDT(budget.allocatedAmount, compact = true)}",
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
                                .fillMaxWidth(pct.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusCapsule))
                                .background(if (isOverBudget) CentwiseColors.ExpenseRed else accent)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("${(pct * 100).toInt()}% used", style = CentwiseTypography.Caption, color = textSecondary)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "${CurrencyFormatter.formatBDT(remaining, compact = true)} remaining",
                            style = CentwiseTypography.Caption,
                            color = if (isOverBudget) CentwiseColors.ExpenseRed else CentwiseColors.IncomeGreen
                        )
                    }
                }
            }

            // Daily Allowance Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(16.dp)
                ) {
                    DetailStat(
                        title = "Daily Allowance",
                        value = CurrencyFormatter.formatBDT(dailyAllowance, compact = true),
                        modifier = Modifier.weight(1f)
                    )
                    DetailStat(
                        title = "Days Left",
                        value = "$daysLeft",
                        modifier = Modifier.weight(1f)
                    )
                    DetailStat(
                        title = "Spent",
                        value = CurrencyFormatter.formatBDT(budget.spentAmount, compact = true),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Transactions
            item {
                Text("Spending in this Category", style = CentwiseTypography.Headline, color = textPrimary)
            }

            if (categoryTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No spending recorded yet this period",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(categoryTransactions.take(10)) { transaction ->
                    TransactionRow(transaction = transaction, isDark = isDark)
                }
            }
        }
    }

    if (showEditSheet) {
        AddEditBudgetSheet(
            editingBudget = budget,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                FakeTransactionRepository.shared.updateBudget(updated)
                showEditSheet = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Budget?") },
            text = { Text("This will permanently delete this budget. Transactions are not affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        FakeTransactionRepository.shared.deleteBudget(budget.id)
                        showDeleteDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Delete", color = CentwiseColors.ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailStat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, style = CentwiseTypography.AmountMedium, color = CentwiseColors.AccentBlue, maxLines = 1)
        Text(title, style = CentwiseTypography.Caption, color = CentwiseColors.LightTextSecondary)
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetDetailScreenPreview() {
    BudgetDetailScreen(
        budget = BudgetItem(
            categoryName = "Food & Dining",
            allocatedAmount = 12000.0,
            spentAmount = 8450.0,
            progress = 0.7f
        )
    )
}
