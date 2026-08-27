package com.centwise.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.repository.TransactionRepository
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Date

data class CategorySpendItem(
    val category: String,
    val totalAmount: Double,
    val percentage: Double,
    val count: Int
)

data class MerchantSpendItem(
    val merchantName: String,
    val totalAmount: Double,
    val transactionCount: Int
)

class AnalyticsViewModel(
    private val repository: TransactionRepository = TransactionRepository.shared
) : ViewModel() {

    val selectedPeriod = MutableStateFlow("This Month")
    val selectedType = MutableStateFlow("All")

    private val periodRange: Flow<Pair<Date, Date>> = selectedPeriod.map { period ->
        val cal = Calendar.currentOrInstance()
        val now = Date()
        cal.time = now
        val endOfToday = now

        when (period) {
            "This Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                Pair(cal.time, endOfToday)
            }
            "Last Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.add(Calendar.MONTH, -1)
                val start = cal.time

                val calEnd = Calendar.currentOrInstance()
                calEnd.time = now
                calEnd.set(Calendar.DAY_OF_MONTH, 1)
                calEnd.set(Calendar.HOUR_OF_DAY, 0)
                calEnd.set(Calendar.MINUTE, 0)
                calEnd.set(Calendar.SECOND, 0)
                calEnd.add(Calendar.SECOND, -1)
                Pair(start, calEnd.time)
            }
            "3 Months" -> {
                cal.add(Calendar.MONTH, -3)
                Pair(cal.time, endOfToday)
            }
            "6 Months" -> {
                cal.add(Calendar.MONTH, -6)
                Pair(cal.time, endOfToday)
            }
            else -> {
                cal.time = Date(0)
                Pair(cal.time, endOfToday)
            }
        }
    }

    private val periodTransactions: Flow<List<TransactionItem>> = combine(
        repository.transactions,
        periodRange
    ) { list, range ->
        list.filter { it.date.time >= range.first.time && it.date.time <= range.second.time }
    }

    val totalIncome: StateFlow<Double> = periodTransactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = periodTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val filteredTypedTransactions: Flow<List<TransactionItem>> = combine(
        periodTransactions,
        selectedType
    ) { list, type ->
        when (type) {
            "Debit" -> list.filter { it.type == TransactionType.EXPENSE }
            "Credit" -> list.filter { it.type == TransactionType.INCOME }
            else -> list
        }
    }

    val transactionCount: StateFlow<Int> = filteredTypedTransactions.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val categoryBreakdown: StateFlow<List<CategorySpendItem>> = filteredTypedTransactions.map { list ->
        val total = maxOf(list.sumOf { it.amount }, 1.0)
        list.groupBy { it.category }
            .map { (cat, items) ->
                val amt = items.sumOf { it.amount }
                CategorySpendItem(
                    category = cat,
                    totalAmount = amt,
                    percentage = amt / total,
                    count = items.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topMerchants: StateFlow<List<MerchantSpendItem>> = filteredTypedTransactions.map { list ->
        list.groupBy { it.title.substringBefore(" - ") }
            .map { (name, items) ->
                MerchantSpendItem(
                    merchantName = name,
                    totalAmount = items.sumOf { it.amount },
                    transactionCount = items.size
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPeriod(period: String) {
        selectedPeriod.value = period
    }

    fun setTypeFilter(type: String) {
        selectedType.value = type
    }
}

private fun Calendar.Companion.currentOrInstance(): Calendar = Calendar.getInstance()

