package com.centwise.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.EmptyStateView
import com.centwise.core.design.components.GreetingCard
import com.centwise.core.design.components.SpendingSummaryCard
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem
import com.centwise.features.transactions.TransactionDetailSheet

@Composable
fun HomeScreen(
    onSeeAllClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlySaved by viewModel.monthlySaved.collectAsState()

    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Title Header
            item {
                Text(
                    text = "Centwise",
                    style = CentwiseTypography.LargeTitle,
                    color = textPrimary
                )
            }

            // 1. User Greeting Card (with Avatar & Dynamic Greeting)
            item {
                GreetingCard(userName = "User", isDark = isDark)
            }

            // 2. Spent this Month 3-Column Card
            item {
                SpendingSummaryCard(
                    monthlyExpense = monthlyExpense,
                    monthlyIncome = monthlyIncome,
                    monthlySaved = monthlySaved,
                    isDark = isDark
                )
            }

            // 3. Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = CentwiseTypography.Title2,
                        color = textPrimary
                    )
                    Text(
                        text = "See All",
                        style = CentwiseTypography.Headline,
                        color = CentwiseColors.AccentMauve,
                        modifier = Modifier.clickable { onSeeAllClick() }
                    )
                }
            }

            // 4. Transactions List or Empty State
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No transactions yet",
                        description = "Tap + to add your first transaction",
                        isDark = isDark
                    )
                }
            } else {
                items(recentTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        onClick = { selectedTransaction = tx },
                        isDark = isDark
                    )
                }
            }
        }

        // Floating Blue Action Button (+)
        FloatingActionButton(
            onClick = onAddClick,
            shape = CircleShape,
            containerColor = CentwiseColors.AccentBlue,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                modifier = Modifier.size(28.dp)
            )
        }

        // Detail Sheet
        selectedTransaction?.let { tx ->
            TransactionDetailSheet(
                transaction = tx,
                onDismiss = { selectedTransaction = null },
                onDelete = {},
                isDark = isDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
