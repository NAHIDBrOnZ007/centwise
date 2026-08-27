package com.centwise.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.repository.TransactionRepository
import com.centwise.data.models.TransactionItem
import kotlinx.coroutines.flow.*

class TransactionsViewModel(
    private val repository: TransactionRepository = TransactionRepository.shared
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("This Month")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        repository.transactions,
        _searchQuery,
        _selectedPeriod
    ) { list, query, period ->
        val cal = java.util.Calendar.getInstance()
        val now = java.util.Date()
        cal.time = now

        val periodFiltered = when (period) {
            "This Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val start = cal.time
                list.filter { it.date.time >= start.time }
            }
            "Last Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val thisMonthStart = cal.time
                cal.add(java.util.Calendar.MONTH, -1)
                val lastMonthStart = cal.time
                list.filter { it.date.time >= lastMonthStart.time && it.date.time < thisMonthStart.time }
            }
            else -> list
        }

        if (query.isBlank()) {
            periodFiltered
        } else {
            periodFiltered.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.paymentMethod.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == com.centwise.data.models.TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == com.centwise.data.models.TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalNet: StateFlow<Double> = filteredTransactions.map { list ->
        val inc = list.filter { it.type == com.centwise.data.models.TransactionType.INCOME }.sumOf { it.amount }
        val exp = list.filter { it.type == com.centwise.data.models.TransactionType.EXPENSE }.sumOf { it.amount }
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun addTransaction(tx: TransactionItem) {
        repository.addTransaction(tx)
    }

    fun deleteTransaction(id: String) {
        repository.deleteTransaction(id)
    }
}
