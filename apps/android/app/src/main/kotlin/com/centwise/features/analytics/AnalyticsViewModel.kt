package com.centwise.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.centwise.data.repository.TransactionRepository
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.core.backend.CentwiseRustBackend
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val repository: TransactionRepository = TransactionRepository.shared
) : ViewModel() {

    val selectedPeriod = MutableStateFlow("This Month")
    val selectedType = MutableStateFlow("All")

    companion object {
        fun getPeriodDateRange(period: String): Pair<Date, Date> {
            val cal = Calendar.getInstance()
            val now = Date()
            cal.time = now
            val endOfToday = now

            return when (period) {
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

                    val calEnd = Calendar.getInstance()
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
    }

    private val snapshot = combine(repository.transactions, selectedPeriod, selectedType) { _, period, type ->
        period to type
    }.mapLatest { (period, type) ->
        val range = getPeriodDateRange(period)
        CentwiseRustBackend.analyticsSnapshot(
            range.first.time,
            range.second.time + 1,
            if (period == "3 Months") 3u else 6u,
            when (type) {
                "Debit" -> "debit"
                "Credit" -> "credit"
                else -> "all"
            }
        )
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalIncome: StateFlow<Double> = snapshot.map { it?.totalIncomeMinor?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalExpense: StateFlow<Double> = snapshot.map { it?.totalExpenseMinor?.div(100.0) ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val transactionCount: StateFlow<Int> = snapshot.map { it?.transactionCount?.toInt() ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val categoryBreakdown: StateFlow<List<CategorySpendItem>> = snapshot.map { value ->
        val total = maxOf(value?.categoryBreakdown?.sumOf { it.totalMinor }?.div(100.0) ?: 0.0, 1.0)
        value?.categoryBreakdown?.map {
            CategorySpendItem(it.categoryName, it.totalMinor / 100.0, it.totalMinor / 100.0 / total, it.transactionCount.toInt())
        } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val topMerchants: StateFlow<List<MerchantSpendItem>> = snapshot.map { value ->
        value?.topMerchants?.map { MerchantSpendItem(it.merchant, it.totalMinor / 100.0, it.transactionCount.toInt()) }
            ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val monthlyTrends: StateFlow<List<TrendPoint>> = snapshot.map { value ->
        value?.monthlyTrends?.map { TrendPoint("${it.month}/${it.year}", it.totalExpenseMinor / 100.0) } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun transactionsForCategory(category: String): List<TransactionItem> {
        val range = getPeriodDateRange(selectedPeriod.value)
        val type = selectedType.value
        return repository.transactions.value.filter { tx ->
            val inRange = tx.date.time >= range.first.time && tx.date.time <= range.second.time
            val matchesCategory = tx.category.equals(category, ignoreCase = true)
            val matchesType = when (type) {
                "Debit" -> tx.type == TransactionType.EXPENSE
                "Credit" -> tx.type == TransactionType.INCOME
                else -> true
            }
            inRange && matchesCategory && matchesType
        }.sortedByDescending { it.timestamp }
    }

    fun transactionsForMerchant(merchantName: String): List<TransactionItem> {
        val range = getPeriodDateRange(selectedPeriod.value)
        val type = selectedType.value
        return repository.transactions.value.filter { tx ->
            val inRange = tx.date.time >= range.first.time && tx.date.time <= range.second.time
            val matchesMerchant = tx.title.substringBefore(" - ").equals(merchantName, ignoreCase = true)
            val matchesType = when (type) {
                "Debit" -> tx.type == TransactionType.EXPENSE
                "Credit" -> tx.type == TransactionType.INCOME
                else -> true
            }
            inRange && matchesMerchant && matchesType
        }.sortedByDescending { it.timestamp }
    }

    fun setPeriod(period: String) {
        selectedPeriod.value = period
    }

    fun setTypeFilter(type: String) {
        selectedType.value = type
    }
}
