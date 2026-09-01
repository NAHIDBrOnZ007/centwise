package com.centwise

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.CentwiseTab
import com.centwise.core.design.components.FloatingTabBar
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.data.repository.TransactionRepository
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
import com.centwise.features.settings.AppLockManager
import com.centwise.features.settings.AppearancePrefs
import com.centwise.features.settings.AppearanceScreen
import com.centwise.features.settings.CategoriesScreen
import com.centwise.features.settings.CurrencyPickerScreen
import com.centwise.features.settings.FAQScreen
import com.centwise.features.settings.LockScreen
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
    data object ReviewQueue : SubScreen()
    data object DataManagement : SubScreen()
    data class AccountDetail(val account: AccountItem) : SubScreen()
    data class BudgetDetail(val budget: BudgetItem) : SubScreen()
}

class MainActivity : FragmentActivity() {
    private var onboardingPermissionCallback: ((Map<String, Boolean>) -> Unit)? = null

    fun requestOnboardingPermissions(
        permissions: Array<String>,
        callback: (Map<String, Boolean>) -> Unit
    ) {
        onboardingPermissionCallback = callback
        requestPermissions(permissions, ONBOARDING_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ONBOARDING_PERMISSION_REQUEST_CODE) {
            val result = permissions.mapIndexed { index, permission ->
                permission to (grantResults.getOrNull(index) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            }.toMap()
            onboardingPermissionCallback?.invoke(result)
            onboardingPermissionCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.centwise.data.repository.TransactionRepository.shared.init(this)
        enableEdgeToEdge()
        setContent {
            CentwiseApp()
        }
    }

}

private const val ONBOARDING_PERMISSION_REQUEST_CODE = 4201

@Composable
fun CentwiseApp(
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // Load persisted appearance + lock preferences & request runtime permissions once
    LaunchedEffect(Unit) {
        AppearancePrefs.load(context)
        AppLockManager.load(context)
        // Lock on cold start when enabled
        AppLockManager.lockNow()
        // Register notification channels
        com.centwise.core.notifications.CentwiseNotifications.ensureChannels(context)

    }

    // Lock/unlock on background/foreground transitions
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> AppLockManager.onAppBackgrounded()
                Lifecycle.Event.ON_START -> AppLockManager.onAppForegrounded()
                else -> Unit
            }
        }
        androidx.lifecycle.ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(observer)
        onDispose {
            androidx.lifecycle.ProcessLifecycleOwner.get()
                .lifecycle
                .removeObserver(observer)
        }
    }

    var currentTab by remember { mutableStateOf(CentwiseTab.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showOnboarding by remember {
        mutableStateOf(!com.centwise.features.onboarding.OnboardingPrefs.isCompleted(context))
    }

    val homeViewModel: HomeViewModel = viewModel()
    val transactionsViewModel: TransactionsViewModel = viewModel()

    // Effective dark mode from persisted preference
    val effectiveDark = when (AppearancePrefs.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val bg = if (effectiveDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground

    fun requestUnlock() {
        if (activity == null) {
            AppLockManager.unlock()
            return
        }

        val canAuthenticate = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        AppLockManager.unlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Keep locked; user can tap Unlock again
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Centwise")
                .setSubtitle("Confirm your identity to view your finances")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            prompt.authenticate(info)
        } else {
            // No secure lock screen set up; do not trap the user
            AppLockManager.unlock()
        }
    }

    BackHandler(enabled = subScreen != null && !AppLockManager.isLocked) {
        subScreen = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        when {
            showOnboarding -> {
                com.centwise.features.onboarding.OnboardingScreen(
                    onFinished = { showOnboarding = false },
                    isDark = effectiveDark
                )
            }

            AppLockManager.isLocked -> {
                LockScreen(
                    onUnlockClick = { requestUnlock() },
                    isDark = effectiveDark
                )
            }

            subScreen != null -> {
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
                        is SubScreen.ReviewQueue -> com.centwise.features.transactions.ReviewQueueScreen(
                            onBackClick = { subScreen = null },
                            isDark = effectiveDark
                        )
                        is SubScreen.DataManagement -> com.centwise.features.settings.DataManagementScreen(
                            onBackClick = { subScreen = null },
                            isDark = effectiveDark
                        )
                    }
                }
            }

            else -> {
                // Main Active Screen Viewport
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentTab) {
                        CentwiseTab.HOME -> {
                            HomeScreen(
                                onSeeAllClick = { currentTab = CentwiseTab.TRANSACTIONS },
                                onAddClick = { showAddSheet = true },
                                onProfileClick = { currentTab = CentwiseTab.SETTINGS },
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
                                onReviewQueueClick = { subScreen = SubScreen.ReviewQueue },
                                onDataManagementClick = { subScreen = SubScreen.DataManagement },
                                onFAQClick = { subScreen = SubScreen.FAQ },
                                onAboutClick = { subScreen = SubScreen.About },
                                isDark = effectiveDark
                            )
                        }
                    }
                }

                // Floating Pill Bottom Tab Bar (With Safe Navigation Bars Padding)
                FloatingTabBar(
                    selectedTab = currentTab,
                    onTabSelected = { currentTab = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp),
                    isDark = effectiveDark
                )
            }
        }

        // Add Transaction Modal Sheet
        if (showAddSheet && !AppLockManager.isLocked && !showOnboarding) {
            AddEditTransactionSheet(
                onDismiss = { showAddSheet = false },
                onSave = { tx ->
                    val saved = TransactionRepository.shared.addTransaction(tx)
                    if (saved) {
                        com.centwise.core.notifications.CentwiseNotifications.notifyNewTransaction(context, tx)
                    }
                    saved
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
