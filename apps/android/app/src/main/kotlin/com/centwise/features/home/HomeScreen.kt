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
import com.centwise.data.repository.TransactionRepository
import com.centwise.core.profile.UserPrefs
import com.centwise.features.settings.AccentOptions
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.EditProfileSheet
import com.centwise.features.transactions.AddEditTransactionSheet
import com.centwise.features.transactions.TransactionDetailSheet

@Composable
fun HomeScreen(
    onSeeAllClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    isDark: Boolean = isSystemInDarkTheme()
) {
    val recentTransactions by viewModel.recentTransactions.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlySaved by viewModel.monthlySaved.collectAsState()
    val accounts by viewModel.accounts.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var currentUserName by remember { mutableStateOf(UserPrefs.getUserName(context)) }
    var currentUserAvatar by remember { mutableStateOf(UserPrefs.getUserAvatar(context)) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    var selectedTransaction by remember { mutableStateOf<TransactionItem?>(null) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground
    val textPrimary = if (isDark) CentwiseColors.DarkTextPrimary else CentwiseColors.LightTextPrimary

    val accent = AccentOptions.byName(AppearancePrefs.accentName).color

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

            // 1. User Greeting Card (Matching iOS GreetingCard 1:1)
            item {
                GreetingCard(
                    userName = currentUserName,
                    avatarResId = UserPrefs.getAvatarResId(currentUserAvatar),
                    onProfileClick = { showEditProfileSheet = true },
                    isDark = isDark
                )
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

            // 3. Accounts Section Carousel
            if (accounts.isNotEmpty()) {
                item {
                    com.centwise.core.design.components.AccountCarousel(
                        accounts = accounts,
                        isDark = isDark
                    )
                }
            }

            // 4. Recent Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = CentwiseTypography.Headline,
                        color = textPrimary
                    )
                    Text(
                        text = "See all",
                        style = CentwiseTypography.Subheadline,
                        color = accent,
                        modifier = Modifier.clickable { onSeeAllClick() }
                    )
                }
            }

            // 5. Transactions List or Empty State
            if (recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No transactions yet",
                        description = "Add a transaction or configure SMS capture to get started.",
                        isDark = isDark
                    )
                }
            } else {
                items(recentTransactions, key = { it.id }) { tx ->
                    TransactionRow(
                        transaction = tx,
                        onClick = { selectedTransaction = tx },
                        showBackground = true,
                        showChevron = true,
                        isDark = isDark
                    )
                }
            }
        }

        // Floating Action Button (+) with Dynamic Theme Accent
        FloatingActionButton(
            onClick = onAddClick,
            shape = CircleShape,
            containerColor = accent,
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
                onDelete = { id ->
                    TransactionRepository.shared.deleteTransaction(id)
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
                    val saved = TransactionRepository.shared.updateTransaction(updatedTx)
                    if (saved) editingTransaction = null
                    saved
                },
                isDark = isDark
            )
        }

        // Edit Profile Sheet (Directly inside Home Tab Matching iOS GreetingCard)
        if (showEditProfileSheet) {
            EditProfileSheet(
                currentName = currentUserName,
                currentAvatar = currentUserAvatar,
                onDismiss = { showEditProfileSheet = false },
                onSave = { newName, newAvatar ->
                    UserPrefs.setUserName(context, newName)
                    UserPrefs.setUserAvatar(context, newAvatar)
                    currentUserName = newName
                    currentUserAvatar = newAvatar
                    showEditProfileSheet = false
                },
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
