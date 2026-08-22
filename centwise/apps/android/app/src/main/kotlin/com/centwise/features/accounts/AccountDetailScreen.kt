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
import androidx.compose.material3.Icon
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
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.AccountItem

@Composable
fun AccountDetailScreen(
    account: AccountItem,
    onBackClick: () -> Unit = {},
    isDark: Boolean = isSystemInDarkTheme()
) {
    val repository = FakeTransactionRepository.shared
    val transactions by repository.transactions.collectAsState()

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val textSecondary = if (isDark) CentwiseColors.DarkTextSecondary else CentwiseColors.LightTextSecondary
    val cardBg = if (isDark) CentwiseColors.DarkSurface else CentwiseColors.LightSurface

    val tint = providerColor(account.providerName)
    val accountTransactions = transactions.filter {
        it.paymentMethod.equals(account.providerName, ignoreCase = true) ||
                it.paymentMethod.equals(account.name, ignoreCase = true)
    }

    val moneyIn = accountTransactions.filter { it.type == com.centwise.data.models.TransactionType.INCOME }
        .sumOf { it.amount }
    val moneyOut = accountTransactions.filter { it.type == com.centwise.data.models.TransactionType.EXPENSE }
        .sumOf { it.amount }

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
                text = account.name,
                style = CentwiseTypography.Headline,
                color = textPrimary,
                maxLines = 1
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(tint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = providerIcon(account.providerName),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Text(account.name, style = CentwiseTypography.Headline, color = textPrimary)
                            Text(
                                text = "${account.type} • •• ${account.accountNumber.takeLast(4)}",
                                style = CentwiseTypography.Caption,
                                color = textSecondary
                            )
                        }
                    }

                    Text("Current Balance", style = CentwiseTypography.Caption, color = textSecondary)
                    Text(
                        text = CurrencyFormatter.formatBDT(account.balance),
                        style = CentwiseTypography.HeroAmount,
                        color = if (account.balance < 0) CentwiseColors.ExpenseRed else textPrimary
                    )
                }
            }

            // Stats Card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusLarge))
                        .background(cardBg)
                        .padding(16.dp)
                ) {
                    StatColumn(
                        title = "Money In",
                        value = CurrencyFormatter.formatBDT(moneyIn, compact = true),
                        color = CentwiseColors.IncomeGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatColumn(
                        title = "Money Out",
                        value = CurrencyFormatter.formatBDT(moneyOut, compact = true),
                        color = CentwiseColors.ExpenseRed,
                        modifier = Modifier.weight(1f)
                    )
                    StatColumn(
                        title = "Transactions",
                        value = "${accountTransactions.size}",
                        color = CentwiseColors.AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Transactions
            item {
                Text("Transactions", style = CentwiseTypography.Headline, color = textPrimary)
            }

            if (accountTransactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions yet",
                        style = CentwiseTypography.Subheadline,
                        color = textSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                items(accountTransactions) { transaction ->
                    TransactionRow(transaction = transaction, isDark = isDark)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, style = CentwiseTypography.AmountMedium, color = color, maxLines = 1)
        Text(title, style = CentwiseTypography.Caption, color = Color(0xFF8E8E93))
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
