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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.CategoryIconHelper
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.repository.TransactionRepository
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
    val repository = TransactionRepository.shared
    val budgets by repository.budgets.collectAsState()
    val transactions by repository.transactions.collectAsState()

    val currentBudget = budgets.firstOrNull { it.id == budget.id } ?: budget

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<com.centwise.data.models.TransactionItem?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface
    val trackBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg

    val pct = if (currentBudget.allocatedAmount > 0) {
        (currentBudget.spentAmount / currentBudget.allocatedAmount).toFloat()
    } else 0f
    val isOverBudget = currentBudget.spentAmount > currentBudget.allocatedAmount

    val categoryTransactions = transactions.filter {
        it.category.equals(currentBudget.categoryName, ignoreCase = true) &&
                it.type == com.centwise.data.models.TransactionType.EXPENSE
    }.sortedByDescending { it.timestamp }

    val daysLeft = run {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        daysInMonth - today + 1
    }
    val remaining = maxOf(currentBudget.allocatedAmount - currentBudget.spentAmount, 0.0)
    val dailyAllowance = remaining / maxOf(daysLeft, 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // 1. Navigation Top Bar (Back, Title, Edit Pill Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackClick = onBackClick, isDark = isDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = currentBudget.categoryName,
                style = CentwiseTypography.Headline,
                color = textPrimary,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
            )
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier
                    .clip(CircleShape)
                    .iosBounceClick { showEditSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Edit",
                        style = CentwiseTypography.Headline.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Unified Progress & Daily Allowance Hero Card (Matching iOS BudgetDetailScreen 1:1)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header: Category Icon, Name & Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconHelper.iconFor(currentBudget.categoryName),
                                contentDescription = currentBudget.categoryName,
                                tint = accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentBudget.categoryName,
                                style = CentwiseTypography.Headline.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isDark) Color(0x1FFFFFFF) else Color(0x0F000000)
                            ) {
                                Text(
                                    text = if (isOverBudget) "Over budget" else "On track",
                                    style = CentwiseTypography.Caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                    color = if (isOverBudget) CentwiseColors.ExpenseRed else textSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                    // Spending & Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = CurrencyFormatter.formatBDT(currentBudget.spentAmount),
                            style = CentwiseTypography.LargeTitle.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "of ${CurrencyFormatter.formatBDT(currentBudget.allocatedAmount, compact = true)}",
                            style = CentwiseTypography.Body,
                            color = textSecondary
                        )
                    }

                    // Progress Bar
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
                                .background(if (isOverBudget) CentwiseColors.ExpenseRed else accent)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${(pct * 100).toInt()}% used",
                            style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${CurrencyFormatter.formatBDT(remaining, compact = true)} ${if (isOverBudget) "over budget" else "remaining"}",
                            style = CentwiseTypography.Caption.copy(fontSize = 12.sp),
                            color = if (isOverBudget) CentwiseColors.ExpenseRed else textSecondary
                        )
                    }

                    HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                    // 3-Column Daily Allowance Stats Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailStat(
                            title = "Daily Allowance",
                            value = CurrencyFormatter.formatBDT(dailyAllowance, compact = true),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        )
                        DetailStat(
                            title = "Days Left",
                            value = "$daysLeft",
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        )
                        DetailStat(
                            title = "Spent",
                            value = CurrencyFormatter.formatBDT(currentBudget.spentAmount, compact = true),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Transactions Section Header
            item {
                Text(
                    text = "Spending in this Category (${categoryTransactions.size})",
                    style = CentwiseTypography.Headline,
                    color = textPrimary
                )
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
                items(categoryTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction },
                        showBackground = true,
                        showChevron = true,
                        isDark = isDark
                    )
                }
            }

            // 4. Destructive Action Section (Matching iOS Delete Budget Card)
            item {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .iosBounceClick { showDeleteDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete Budget",
                            style = CentwiseTypography.Headline.copy(fontSize = 15.sp),
                            color = CentwiseColors.ExpenseRed,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        AddEditBudgetSheet(
            editingBudget = currentBudget,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                TransactionRepository.shared.updateBudget(updated)
                showEditSheet = false
            }
        )
    }

    if (selectedTransaction != null) {
        com.centwise.features.transactions.TransactionDetailSheet(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null },
            onEdit = null,
            onDelete = { txId ->
                TransactionRepository.shared.deleteTransaction(txId)
                selectedTransaction = null
            },
            isDark = isDark
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Budget?", style = CentwiseTypography.Headline) },
            text = { Text("This will permanently delete this budget. Transactions are not affected.", style = CentwiseTypography.Body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        TransactionRepository.shared.deleteBudget(budget.id)
                        showDeleteDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Delete", color = CentwiseColors.ExpenseRed, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun DetailStat(title: String, value: String, modifier: Modifier = Modifier) {
    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, style = CentwiseTypography.AmountMedium, color = accent, maxLines = 1)
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
            spentAmount = 8450.0
        )
    )
}
