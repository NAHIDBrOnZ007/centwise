package com.centwise.features.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*

enum class TransactionSortOrder(val displayName: String) {
    DATE_DESC("Date (Newest First)"),
    DATE_ASC("Date (Oldest First)"),
    AMOUNT_DESC("Amount (Highest First)"),
    AMOUNT_ASC("Amount (Lowest First)")
}

class TransactionsViewModel(
    private val repository: TransactionRepository = TransactionRepository.shared
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("This Month")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType: StateFlow<TransactionType?> = _selectedType.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _sortOrder = MutableStateFlow(TransactionSortOrder.DATE_DESC)
    val sortOrder: StateFlow<TransactionSortOrder> = _sortOrder.asStateFlow()

    val filteredTransactions: StateFlow<List<TransactionItem>> = combine(
        repository.transactions,
        _searchQuery,
        _selectedPeriod,
        _selectedType,
        _selectedCategory
    ) { list, query, period, typeFilter, catFilter ->
        val cal = java.util.Calendar.getInstance()
        val now = java.util.Date()
        cal.time = now

        // 1. Period filter
        val periodFiltered = when (period) {
            "This Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val start = cal.time
                list.filter { it.timestamp >= start.time }
            }
            "Last Month" -> {
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val thisMonthStart = cal.time
                cal.add(java.util.Calendar.MONTH, -1)
                val lastMonthStart = cal.time
                list.filter { it.timestamp >= lastMonthStart.time && it.timestamp < thisMonthStart.time }
            }
            "This Year" -> {
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                val start = cal.time
                list.filter { it.timestamp >= start.time }
            }
            else -> list
        }

        // 2. Type filter
        val typeFiltered = if (typeFilter == null) {
            periodFiltered
        } else {
            periodFiltered.filter { it.type == typeFilter }
        }

        // 3. Category filter
        val categoryFiltered = if (catFilter.isNullOrBlank()) {
            typeFiltered
        } else {
            typeFiltered.filter { it.category.equals(catFilter, ignoreCase = true) }
        }

        // 4. Search query
        if (query.isBlank()) {
            categoryFiltered
        } else {
            val q = query.trim()
            categoryFiltered.filter {
                it.title.contains(q, ignoreCase = true) ||
                it.category.contains(q, ignoreCase = true) ||
                it.paymentMethod.contains(q, ignoreCase = true) ||
                it.reference?.contains(q, ignoreCase = true) == true
            }
        }
    }.combine(_sortOrder) { list, sort ->
        when (sort) {
            TransactionSortOrder.DATE_DESC -> list.sortedByDescending { it.timestamp }
            TransactionSortOrder.DATE_ASC -> list.sortedBy { it.timestamp }
            TransactionSortOrder.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> list.sortedBy { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = filteredTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalNet: StateFlow<Double> = filteredTransactions.map { list ->
        val inc = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val exp = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategory.value = category
    }

    fun setSortOrder(order: TransactionSortOrder) {
        _sortOrder.value = order
    }

    fun toggleSortOrder() {
        _sortOrder.value = if (_sortOrder.value == TransactionSortOrder.DATE_DESC) {
            TransactionSortOrder.DATE_ASC
        } else {
            TransactionSortOrder.DATE_DESC
        }
    }

    fun addTransaction(tx: TransactionItem): Boolean = repository.addTransaction(tx)

    fun updateTransaction(tx: TransactionItem): Boolean = repository.updateTransaction(tx)

    fun deleteTransaction(id: String): Boolean = repository.deleteTransaction(id)
}
