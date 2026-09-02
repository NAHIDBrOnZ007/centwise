package com.centwise.data.repository

import android.content.Context
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.TransactionKind
import com.centwise.core.uniffi.HomeDashboardRecord
import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.CategoryOption
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Android's thin view-model-facing adapter over the Rust-owned database.
 * It never opens SQLite and never maintains a second persistence fallback.
 */
class TransactionRepository private constructor() {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val _transactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val transactions: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()
    private val _accounts = MutableStateFlow<List<AccountItem>>(emptyList())
    val accounts: StateFlow<List<AccountItem>> = _accounts.asStateFlow()
    private val _budgets = MutableStateFlow<List<BudgetItem>>(emptyList())
    val budgets: StateFlow<List<BudgetItem>> = _budgets.asStateFlow()
    private val _subscriptions = MutableStateFlow<List<SubscriptionItem>>(emptyList())
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()
    private val _categories = MutableStateFlow<List<CategoryOption>>(emptyList())
    val categories: StateFlow<List<CategoryOption>> = _categories.asStateFlow()
    private val _homeDashboard = MutableStateFlow<HomeDashboardRecord?>(null)
    val homeDashboard: StateFlow<HomeDashboardRecord?> = _homeDashboard.asStateFlow()
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    val totalIncome = _transactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = _transactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalNet = _transactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } -
            list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    fun init(context: Context) {
        repositoryScope.launch {
            refreshMutex.withLock {
                _isReady.value = false
                CentwiseRustBackend.initialize(context.applicationContext)
                refreshFromRust()
                _isReady.value = true
            }
        }
    }

    fun loadFromRust() {
        repositoryScope.launch {
            refreshMutex.withLock { refreshFromRust() }
        }
    }

    /** Refreshes state in the caller's IO coroutine, avoiding a second queued refresh. */
    suspend fun refreshNow() {
        refreshMutex.withLock { refreshFromRust() }
    }

    private fun refreshFromRust() {
        if (!CentwiseRustBackend.isAvailable()) {
            clearLoadedState()
            return
        }

        _categories.value = CentwiseRustBackend.listCategories().map { category ->
            CategoryOption(
                id = category.id,
                name = category.name,
                icon = category.icon,
                colorHex = parseColor(category.colorHex),
                isSystem = category.isSystem
            )
        }
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextMonth = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        _homeDashboard.value = CentwiseRustBackend.homeDashboard(
            monthStart.timeInMillis,
            nextMonth.timeInMillis
        )
        val accounts = CentwiseRustBackend.listAccounts()
        _accounts.value = accounts.map { account ->
            AccountItem(
                id = account.id,
                name = account.name,
                type = account.provider,
                balance = account.balanceMinor / 100.0,
                providerName = account.provider,
                accountNumber = account.lastFour?.let { "****$it" } ?: "",
                archived = account.archived
            )
        }
        _transactions.value = CentwiseRustBackend.listTransactions().map { transaction ->
            TransactionItem(
                id = transaction.id,
                title = transaction.title,
                amount = transaction.amountMinor / 100.0,
                type = transaction.kind.toNativeType(),
                category = categoryName(transaction.categoryId),
                paymentMethod = accounts.firstOrNull { it.id == transaction.accountId }?.name
                    ?: "Unknown account",
                timestamp = transaction.occurredAtEpochMs,
                note = transaction.notes,
                rawSms = transaction.rawSms,
                reference = transaction.reference
            )
        }
        _budgets.value = CentwiseRustBackend.listBudgets().map { budget ->
            BudgetItem(
                id = budget.id,
                categoryName = budget.categoryName,
                allocatedAmount = budget.limitMinor / 100.0,
                spentAmount = budget.spentMinor / 100.0,
                period = budget.period,
                categoryId = budget.categoryId,
                startEpochMs = budget.startEpochMs,
                endEpochMs = budget.endEpochMs
            )
        }
        _subscriptions.value = CentwiseRustBackend.listSubscriptions().map { subscription ->
            SubscriptionItem(
                id = subscription.id,
                name = subscription.name,
                amount = subscription.amountMinor / 100.0,
                billingCycle = subscription.billingCycle,
                nextBillingDate = formatDate(subscription.nextDueEpochMs),
                nextDueEpochMs = subscription.nextDueEpochMs,
                isActive = subscription.isActive
            )
        }
        SmartRulesRepository.shared.refresh()
        ReviewQueueRepository.shared.refresh()
    }

    fun addTransaction(tx: TransactionItem): Boolean =
        CentwiseRustBackend.insertTransaction(tx).also { if (it) loadFromRust() }

    fun updateTransaction(tx: TransactionItem): Boolean =
        CentwiseRustBackend.updateTransaction(tx).also { if (it) loadFromRust() }

    fun deleteTransaction(id: String): Boolean =
        CentwiseRustBackend.deleteTransaction(id).also { if (it) loadFromRust() }

    fun addAccount(account: AccountItem): Boolean =
        CentwiseRustBackend.insertAccount(account).also { if (it) loadFromRust() }

    fun updateAccount(account: AccountItem): Boolean =
        CentwiseRustBackend.updateAccount(account).also { if (it) loadFromRust() }

    fun deleteAccount(id: String): Boolean =
        CentwiseRustBackend.deleteAccount(id).also { if (it) loadFromRust() }

    fun addBudget(budget: BudgetItem): Boolean =
        CentwiseRustBackend.insertBudget(budget).also { if (it) loadFromRust() }

    fun updateBudget(budget: BudgetItem): Boolean =
        CentwiseRustBackend.updateBudget(budget).also { if (it) loadFromRust() }

    fun deleteBudget(id: String): Boolean =
        CentwiseRustBackend.deleteBudget(id).also { if (it) loadFromRust() }

    fun addSubscription(subscription: SubscriptionItem): Boolean =
        CentwiseRustBackend.insertSubscription(subscription).also { if (it) loadFromRust() }

    fun updateSubscription(subscription: SubscriptionItem): Boolean =
        CentwiseRustBackend.updateSubscription(subscription).also { if (it) loadFromRust() }

    fun deleteSubscription(id: String): Boolean =
        CentwiseRustBackend.deleteSubscription(id).also { if (it) loadFromRust() }

    fun insertCategory(category: CategoryOption): Boolean =
        CentwiseRustBackend.insertCategory(category.toRustInput()).also { if (it) loadFromRust() }

    fun updateCategory(category: CategoryOption): Boolean =
        CentwiseRustBackend.updateCategory(category.toRustInput()).also { if (it) loadFromRust() }

    fun deleteCategory(id: String): Boolean =
        CentwiseRustBackend.deleteCategory(id).also { if (it) loadFromRust() }

    fun resetToEmptyDatabase() {
        if (CentwiseRustBackend.resetToEmptyDatabase()) loadFromRust()
    }

    /** There is no native store to clear after the migration. */
    fun clearLegacyStorage() = loadFromRust()

    private fun clearLoadedState() {
        _transactions.value = emptyList()
        _accounts.value = emptyList()
        _budgets.value = emptyList()
        _subscriptions.value = emptyList()
        _categories.value = emptyList()
        _homeDashboard.value = null
    }

    private fun categoryName(id: String): String =
        _categories.value.firstOrNull { it.id == id }?.name ?: id

    private fun parseColor(value: String): Long {
        val digits = value.removePrefix("#")
        val parsed = digits.toLongOrNull(16) ?: return 0xFF64748BL
        return if (digits.length <= 6) parsed or 0xFF000000L else parsed
    }

    private fun formatDate(epochMs: Long): String =
        SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(epochMs))

    private fun CategoryOption.toRustInput() = com.centwise.core.uniffi.CategoryInput(
        id = id,
        name = name,
        icon = icon,
        colorHex = String.format(Locale.US, "#%06X", colorHex and 0xFFFFFFL)
    )

    private fun TransactionKind.toNativeType() = when (this) {
        TransactionKind.INCOME -> TransactionType.INCOME
        TransactionKind.EXPENSE -> TransactionType.EXPENSE
        TransactionKind.TRANSFER -> TransactionType.TRANSFER
        TransactionKind.REFUND -> TransactionType.CREDIT
    }

    companion object {
        val shared = TransactionRepository()
    }
}
