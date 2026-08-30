package com.centwise.features.accounts

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.centwise.core.design.components.TopBarBackButton
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.components.iosBounceClick
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.AccountItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.repository.TransactionRepository
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.transactions.AddEditTransactionSheet
import com.centwise.features.transactions.TransactionDetailSheet

@Composable
fun AccountDetailScreen(
    account: AccountItem,
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = TransactionRepository.shared
    val accounts by repository.accounts.collectAsState()
    val transactions by repository.transactions.collectAsState()

    // Live account reference
    val currentAccount = accounts.firstOrNull { it.id == account.id } ?: account

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color
    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    val tint = providerColor(currentAccount.providerName)
    val accountTransactions = transactions.filter {
        it.paymentMethod.equals(currentAccount.providerName, ignoreCase = true) ||
        it.paymentMethod.equals(currentAccount.name, ignoreCase = true)
    }

    val moneyIn = accountTransactions.filter { it.type == com.centwise.data.models.TransactionType.INCOME }
        .sumOf { it.amount }
    val moneyOut = accountTransactions.filter { it.type == com.centwise.data.models.TransactionType.EXPENSE }
        .sumOf { it.amount }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", color = textPrimary) },
            text = {
                Text(
                    "Are you sure you want to remove ${currentAccount.name}? This will also delete all associated transactions.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteAccount(currentAccount.id)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CentwiseColors.ExpenseRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = textSecondary)
                }
            },
            containerColor = cardBg
        )
    }

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
                text = currentAccount.name,
                style = CentwiseTypography.Headline,
                color = textPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Unified Hero Account & Balance Card (Exact matching iOS AccountDetailScreen)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Clean unboxed provider icon tinted with theme accent (Matching iOS AccountDetailScreen)
                        Box(
                            modifier = Modifier.size(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = providerIcon(currentAccount.providerName),
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentAccount.name, style = CentwiseTypography.Headline, color = textPrimary)
                            Text(
                                text = "${currentAccount.type} • •• ${currentAccount.accountNumber.takeLast(4)}",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                    // Balance Section
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Current Balance", style = CentwiseTypography.Caption, color = textSecondary)
                        Text(
                            text = CurrencyFormatter.formatBDT(currentAccount.balance),
                            style = CentwiseTypography.HeroAmount.copy(fontSize = 28.sp),
                            color = if (currentAccount.balance < 0) CentwiseColors.ExpenseRed else textPrimary
                        )
                    }

                    HorizontalDivider(color = if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))

                    // 3-Column Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumn(
                            title = "Money In",
                            value = CurrencyFormatter.formatBDT(moneyIn, compact = true),
                            color = CentwiseColors.IncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        )
                        StatColumn(
                            title = "Money Out",
                            value = CurrencyFormatter.formatBDT(moneyOut, compact = true),
                            color = CentwiseColors.ExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(if (isDark) Color(0x14FFFFFF) else Color(0x0A000000))
                        )
                        StatColumn(
                            title = "Transactions",
                            value = "${accountTransactions.size}",
                            color = accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Transactions Section Header
            item {
                Text(
                    text = "Transactions (${accountTransactions.size})",
                    style = CentwiseTypography.Headline.copy(fontSize = 14.sp),
                    color = textSecondary
                )
            }

            if (accountTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions recorded for this account.",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(accountTransactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction },
                        showBackground = true,
                        showChevron = true,
                        isDark = isDark
                    )
                }
            }

            // 3. Destructive Action Button (Delete Account)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .iosBounceClick { showDeleteDialog = true }
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Delete Account",
                        style = CentwiseTypography.Headline,
                        fontWeight = FontWeight.SemiBold,
                        color = CentwiseColors.ExpenseRed,
                        fontSize = 15.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp)
                    )
                }
            }
        }
    }

    if (showEditSheet) {
        AddEditAccountSheet(
            initialAccount = currentAccount,
            onDismiss = { showEditSheet = false },
            onSave = { updated ->
                repository.updateAccount(updated)
                showEditSheet = false
            },
            isDark = isDark
        )
    }

    // Detail Sheet
    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            onDismiss = { selectedTransaction = null },
            onDelete = { id ->
                repository.deleteTransaction(id)
                selectedTransaction = null
            },
            onEdit = { toEdit ->
                selectedTransaction = null
                editingTransaction = toEdit
            },
            isDark = isDark
        )
    }

    // Edit Sheet
    editingTransaction?.let { tx ->
        AddEditTransactionSheet(
            initialTransaction = tx,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                val saved = repository.updateTransaction(updatedTx)
                if (saved) editingTransaction = null
                saved
            },
            isDark = isDark
        )
    }
}

@Composable
private fun StatColumn(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = CentwiseTypography.AmountMedium.copy(fontSize = 15.sp), color = color, maxLines = 1)
        Text(title, style = CentwiseTypography.Caption.copy(fontSize = 11.sp), color = CentwiseColors.LightTextSecondary)
    }
}

@Preview(showBackground = true)
@Composable
fun AccountDetailScreenPreview() {
    AccountDetailScreen(
        account = AccountItem(
            name = "bKash",
            type = "MFS Wallet",
            balance = 12500.0,
            providerName = "bKash",
            accountNumber = "01712345678"
        )
    )
}
