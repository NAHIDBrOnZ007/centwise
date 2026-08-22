package com.centwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.CentwiseTab
import com.centwise.core.design.components.FloatingTabBar
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.features.accounts.AccountDetailScreen
import com.centwise.features.accounts.AccountListScreen
import com.centwise.features.analytics.AnalyticsScreen
import com.centwise.features.budgets.BudgetDetailScreen
import com.centwise.features.budgets.BudgetListScreen
import com.centwise.features.home.HomeScreen
import com.centwise.features.home.HomeViewModel
import com.centwise.features.settings.AboutScreen
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.AppearanceScreen
import com.centwise.features.settings.CategoriesScreen
import com.centwise.features.settings.CurrencyPickerScreen
import com.centwise.features.settings.FAQScreen
import com.centwise.features.settings.RulesScreen
import com.centwise.features.settings.SettingsScreen
import com.centwise.features.settings.ThemeMode
import com.centwise.features.subscriptions.SubscriptionsScreen
import com.centwise.features.transactions.AddEditTransactionSheet
import com.centwise.features.transactions.TransactionListScreen
import com.centwise.features.transactions.TransactionsViewModel

sealed class SubScreen {
    data object Appearance : SubScreen()
    data object Currency : SubScreen()
    data object Categories : SubScreen()
    data object Rules : SubScreen()
    data object Budgets : SubScreen()
    data object Accounts : SubScreen()
    data object Subscriptions : SubScreen()
    data object FAQ : SubScreen()
    data object About : SubScreen()
    data class AccountDetail(val account: AccountItem) : SubScreen()
    data class BudgetDetail(val budget: BudgetItem) : SubScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CentwiseApp()
        }
    }
}

@Composable
fun CentwiseApp(
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current

    // Load persisted appearance + lock preferences once
    LaunchedEffect(Unit) {
        AppearancePrefs.load(context)
        com.centwise.features.settings.AppLockManager.load(context)
    }

    var currentTab by remember { mutableStateOf(CentwiseTab.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel()
    val transactionsViewModel: TransactionsViewModel = viewModel()

    // Effective dark mode from persisted preference
    val effectiveDark = when (AppearancePrefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val bg = if (effectiveDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground

    BackHandler(enabled = subScreen != null) {
        subScreen = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Sub-screens take over the viewport when open
        val activeSubScreen = subScreen
        if (activeSubScreen != null) {
            when (activeSubScreen) {
                is SubScreen.Appearance -> AppearanceScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
                is SubScreen.Currency -> CurrencyPickerScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
                is SubScreen.Categories -> CategoriesScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
                is SubScreen.Rules -> RulesScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
                is SubScreen.Budgets -> BudgetListScreen(
                    onBackClick = { subScreen = null },
                    onBudgetClick = { budget ->
                        subScreen = SubScreen.BudgetDetail(budget)
                    },
                    isDark = effectiveDark
                )
                is SubScreen.BudgetDetail -> BudgetDetailScreen(
                    budget = activeSubScreen.budget,
                    onBackClick = { subScreen = SubScreen.Budgets },
                    isDark = effectiveDark
                )
                is SubScreen.Accounts -> AccountListScreen(
                    onBackClick = { subScreen = null },
                    onAccountClick = { account ->
                        subScreen = SubScreen.AccountDetail(account)
                    },
                    isDark = effectiveDark
                )
                is SubScreen.AccountDetail -> AccountDetailScreen(
                    account = activeSubScreen.account,
                    onBackClick = { subScreen = SubScreen.Accounts },
                    isDark = effectiveDark
                )
                is SubScreen.Subscriptions -> SubscriptionsScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
                is SubScreen.FAQ -> FAQScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
                is SubScreen.About -> AboutScreen(
                    onBackClick = { subScreen = null },
                    isDark = effectiveDark
                )
            }
        } else {
            // Main Active Screen Viewport
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    CentwiseTab.HOME -> {
                        HomeScreen(
                            onSeeAllClick = { currentTab = CentwiseTab.TRANSACTIONS },
                            onAddClick = { showAddSheet = true },
                            viewModel = homeViewModel,
                            isDark = effectiveDark
                        )
                    }
                    CentwiseTab.TRANSACTIONS -> {
                        TransactionListScreen(
                            onAddClick = { showAddSheet = true },
                            viewModel = transactionsViewModel,
                            isDark = effectiveDark
                        )
                    }
                    CentwiseTab.ANALYTICS -> {
                        AnalyticsScreen(isDark = effectiveDark)
                    }
                    CentwiseTab.SETTINGS -> {
                        SettingsScreen(
                            onAppearanceClick = { subScreen = SubScreen.Appearance },
                            onCurrencyClick = { subScreen = SubScreen.Currency },
                            onCategoriesClick = { subScreen = SubScreen.Categories },
                            onBudgetsClick = { subScreen = SubScreen.Budgets },
                            onAccountsClick = { subScreen = SubScreen.Accounts },
                            onSubscriptionsClick = { subScreen = SubScreen.Subscriptions },
                            onSmartRulesClick = { subScreen = SubScreen.Rules },
                            onFAQClick = { subScreen = SubScreen.FAQ },
                            onAboutClick = { subScreen = SubScreen.About },
                            isDark = effectiveDark
                        )
                    }
                }
            }

            // Floating Pill Bottom Tab Bar
            FloatingTabBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter),
                isDark = effectiveDark
            )
        }

        // Add Transaction Modal Sheet
        if (showAddSheet) {
            AddEditTransactionSheet(
                onDismiss = { showAddSheet = false },
                onSave = { tx ->
                    FakeTransactionRepository.shared.addTransaction(tx)
                },
                isDark = effectiveDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CentwiseAppPreview() {
    CentwiseApp()
}
