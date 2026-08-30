package com.centwise.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.repository.TransactionRepository
import com.centwise.data.models.TransactionItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repository: TransactionRepository = TransactionRepository.shared
) : ViewModel() {

    val recentTransactions: StateFlow<List<TransactionItem>> = repository.transactions
        .map { list -> list.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyExpense: StateFlow<Double> = repository.homeDashboard
        .map { it?.periodExpenseMinor?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncome: StateFlow<Double> = repository.homeDashboard
        .map { it?.periodIncomeMinor?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySaved: StateFlow<Double> = repository.homeDashboard
        .map { dashboard ->
            ((dashboard?.periodIncomeMinor ?: 0L) - (dashboard?.periodExpenseMinor ?: 0L)) / 100.0
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val accounts: StateFlow<List<com.centwise.data.models.AccountItem>> = repository.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
