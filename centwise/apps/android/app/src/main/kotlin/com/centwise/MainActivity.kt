package com.centwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.centwise.core.design.components.CentwiseTab
import com.centwise.core.design.components.FloatingTabBar
import com.centwise.core.design.theme.CentwiseColors
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.features.analytics.AnalyticsScreen
import com.centwise.features.home.HomeScreen
import com.centwise.features.home.HomeViewModel
import com.centwise.features.settings.SettingsScreen
import com.centwise.features.transactions.AddEditTransactionSheet
import com.centwise.features.transactions.TransactionListScreen
import com.centwise.features.transactions.TransactionsViewModel

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
    var currentTab by remember { mutableStateOf(CentwiseTab.HOME) }
    var showAddSheet by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel()
    val transactionsViewModel: TransactionsViewModel = viewModel()

    val bg = if (isDark) CentwiseColors.DarkBackground else CentwiseColors.LightBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .statusBarsPadding()
    ) {
        // Main Active Screen Viewport
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                CentwiseTab.HOME -> {
                    HomeScreen(
                        onSeeAllClick = { currentTab = CentwiseTab.TRANSACTIONS },
                        onAddClick = { showAddSheet = true },
                        viewModel = homeViewModel,
                        isDark = isDark
                    )
                }
                CentwiseTab.TRANSACTIONS -> {
                    TransactionListScreen(
                        onAddClick = { showAddSheet = true },
                        viewModel = transactionsViewModel,
                        isDark = isDark
                    )
                }
                CentwiseTab.ANALYTICS -> {
                    AnalyticsScreen(isDark = isDark)
                }
                CentwiseTab.SETTINGS -> {
                    SettingsScreen(isDark = isDark)
                }
            }
        }

        // Floating Pill Bottom Tab Bar
        FloatingTabBar(
            selectedTab = currentTab,
            onTabSelected = { currentTab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
            isDark = isDark
        )

        // Add Transaction Modal Sheet
        if (showAddSheet) {
            AddEditTransactionSheet(
                onDismiss = { showAddSheet = false },
                onSave = { tx ->
                    FakeTransactionRepository.shared.addTransaction(tx)
                },
                isDark = isDark
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CentwiseAppPreview() {
    CentwiseApp()
}
