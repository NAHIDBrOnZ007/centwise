package com.centwise.features.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.ActionPillGroup
import com.centwise.core.design.components.EmptyStateView
import com.centwise.core.design.components.FilterPillRow
import com.centwise.core.design.components.TransactionRow
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.design.theme.CentwiseSpacing
import com.centwise.core.design.theme.CentwiseTypography
import com.centwise.data.models.TransactionItem

@Composable
fun TransactionListScreen(
    onAddClick: () -> Unit = {},
    viewModel: TransactionsViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary
    val searchBg = if (isDark) CentwiseColors.DarkSearchBg else CentwiseColors.LightSearchBg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title + Action Pill (+, ⇅, 📤)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = CentwiseTypography.LargeTitle,
                        color = textPrimary
                    )
                    ActionPillGroup(
                        onAddClick = onAddClick,
                        onSortClick = {},
                        onExportClick = {},
                        isDark = isDark
                    )
                }
            }

            // Search Bar Capsule
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    placeholder = {
                        Text(
                            text = "Search transactions",
                            style = CentwiseTypography.Body,
                            color = CentwiseColors.LightTextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CentwiseColors.LightTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CentwiseSpacing.CornerRadiusMedium)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = searchBg,
                        unfocusedContainerColor = searchBg,
                        disabledContainerColor = searchBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // Filter Chips (All Time, Type, Category)
            item {
                FilterPillRow(isDark = isDark)
            }

            // Transactions List or Empty State
            if (transactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Transactions Yet",
                        description = "Add your first transaction to start tracking your finances.",
                        buttonText = "Add Transaction",
                        onButtonClick = onAddClick,
                        isDark = isDark
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        onClick = { selectedTransaction = tx },
                        isDark = isDark
                    )
                }
            }
        }

        // Transaction Detail Sheet
        selectedTransaction?.let { tx ->
            TransactionDetailSheet(
                transaction = tx,
                onDismiss = { selectedTransaction = null },
                onDelete = { id -> viewModel.deleteTransaction(id) },
                isDark = isDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionListScreenPreview() {
    TransactionListScreen()
}
