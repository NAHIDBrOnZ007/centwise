package com.centwise.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.TransactionItem
import kotlinx.coroutines.flow.*

class TransactionsViewModel(
    private val repository: FakeTransactionRepository = FakeTransactionRepository.shared
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        repository.transactions,
        _searchQuery
    ) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.paymentMethod.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun addTransaction(tx: TransactionItem) {
        repository.addTransaction(tx)
    }

    fun deleteTransaction(id: String) {
        repository.deleteTransaction(id)
    }
}
