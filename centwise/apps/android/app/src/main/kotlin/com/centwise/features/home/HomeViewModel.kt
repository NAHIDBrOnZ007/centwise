package com.centwise.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.TransactionItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repository: FakeTransactionRepository = FakeTransactionRepository.shared
) : ViewModel() {

    val recentTransactions: StateFlow<List<TransactionItem>> = repository.transactions
        .map { list -> list.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyExpense: StateFlow<Double> = repository.totalExpense
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyIncome: StateFlow<Double> = repository.totalIncome
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySaved: StateFlow<Double> = repository.totalNet
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
}
