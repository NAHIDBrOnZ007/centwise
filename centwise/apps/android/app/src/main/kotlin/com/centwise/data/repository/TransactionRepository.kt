package com.centwise.data.repository

import android.content.Context
import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.CategoryOption
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.TransactionKind
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Production SQLite Transaction Repository for Centwise Android.
 */
class TransactionRepository private constructor() {

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

    private var dbHelper: CentwiseDatabaseHelper? = null

    val totalIncome = _transactions.map { list ->
        list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val totalExpense = _transactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    val totalNet = _transactions.map { list ->
        val inc = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val exp = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        inc - exp
    }

    fun init(context: Context) {
        dbHelper = CentwiseDatabaseHelper.getInstance(context)
        loadFromSQLite()
    }

    fun loadFromSQLite() {
        _categories.value = CentwiseRustBackend.listCategories().map { category ->
            CategoryOption(
                id = category.id,
                name = category.name,
                icon = category.icon,
                colorHex = parseColor(category.colorHex),
                isSystem = category.isSystem
            )
        }
        if (loadFromRustIfPopulated()) return

        val helper = dbHelper ?: return

        // 1. Transactions
        _transactions.value = helper.getAllTransactions()

        // 2. Accounts
        val loadedAccs = helper.getAllAccounts()
        _accounts.value = loadedAccs

        // 3. Budgets (Clean empty by default)
        _budgets.value = helper.getAllBudgets()

        // 4. Subscriptions (Clean empty by default)
        _subscriptions.value = helper.getAllSubscriptions()
    }

    private fun loadFromRustIfPopulated(): Boolean {
        if (!CentwiseRustBackend.isAvailable()) return false

        val rustTransactions = CentwiseRustBackend.listTransactions()
        val rustAccounts = CentwiseRustBackend.listAccounts()
        val rustBudgets = CentwiseRustBackend.listBudgets()
        val rustSubscriptions = CentwiseRustBackend.listSubscriptions()
        val hasRustData = rustTransactions.isNotEmpty() ||
            rustBudgets.isNotEmpty() ||
            rustSubscriptions.isNotEmpty() ||
            rustAccounts.any { it.id.startsWith("demo-") }
        if (!hasRustData) return false

        _transactions.value = rustTransactions.map { transaction ->
            TransactionItem(
                id = transaction.id,
                title = transaction.title,
                amount = transaction.amountMinor / 100.0,
                type = when (transaction.kind) {
                    TransactionKind.INCOME -> TransactionType.INCOME
                    TransactionKind.EXPENSE -> TransactionType.EXPENSE
                    TransactionKind.TRANSFER -> TransactionType.TRANSFER
                    TransactionKind.REFUND -> TransactionType.CREDIT
                },
                category = categoryName(transaction.categoryId),
                paymentMethod = rustAccounts.firstOrNull { it.id == transaction.accountId }?.name
                    ?: "Unknown account",
                timestamp = transaction.occurredAtEpochMs,
                note = transaction.notes,
                rawSms = transaction.rawSms,
                reference = transaction.reference
            )
        }
        _accounts.value = rustAccounts.map { account ->
            AccountItem(
                id = account.id,
                name = account.name,
                type = account.provider,
                balance = account.balanceMinor / 100.0,
                providerName = account.provider,
                accountNumber = account.lastFour?.let { "****$it" } ?: ""
            )
        }
        _budgets.value = rustBudgets.map { budget ->
            BudgetItem(
                id = budget.id,
                categoryName = budget.categoryName,
                allocatedAmount = budget.limitMinor / 100.0,
                spentAmount = budget.spentMinor / 100.0,
                period = budget.period
            )
        }
        _subscriptions.value = rustSubscriptions.map { subscription ->
            SubscriptionItem(
                id = subscription.id,
                name = subscription.name,
                amount = subscription.amountMinor / 100.0,
                billingCycle = subscription.billingCycle,
                nextBillingDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                    .format(Date(subscription.nextDueEpochMs))
            )
        }
        return true
    }

    private fun categoryName(id: String): String =
        _categories.value.firstOrNull { it.id == id }?.name ?: id

    private fun parseColor(value: String): Long {
        val digits = value.removePrefix("#")
        val parsed = digits.toLongOrNull(16) ?: return 0xFF64748BL
        return if (digits.length <= 6) parsed or 0xFF000000L else parsed
    }

    fun addTransaction(tx: TransactionItem) {
        dbHelper?.insertTransaction(tx)

        // Update matching account balance
        val currentAccs = _accounts.value.toMutableList()
        val accIdx = currentAccs.indexOfFirst {
            it.name.contains(tx.paymentMethod, ignoreCase = true) ||
                tx.paymentMethod.contains(it.name, ignoreCase = true)
        }
        if (accIdx != -1) {
            val acc = currentAccs[accIdx]
            val newBalance = if (tx.type == TransactionType.INCOME) {
                acc.balance + tx.amount
            } else {
                acc.balance - tx.amount
            }
            val updated = acc.copy(balance = newBalance)
            currentAccs[accIdx] = updated
            dbHelper?.insertOrUpdateAccount(updated)
            _accounts.value = currentAccs
        }

        _transactions.value = listOf(tx) + _transactions.value
    }

    fun deleteTransaction(id: String) {
        dbHelper?.deleteTransaction(id)
        _transactions.value = _transactions.value.filter { it.id != id }
    }

    fun addAccount(account: AccountItem) {
        dbHelper?.insertOrUpdateAccount(account)
        _accounts.value = _accounts.value + account
    }

    fun addBudget(budget: BudgetItem) {
        dbHelper?.insertOrUpdateBudget(budget)
        _budgets.value = _budgets.value + budget
    }

    fun updateBudget(budget: BudgetItem) {
        dbHelper?.insertOrUpdateBudget(budget)
        _budgets.value = _budgets.value.map { if (it.id == budget.id) budget else it }
    }

    fun deleteBudget(id: String) {
        dbHelper?.deleteBudget(id)
        _budgets.value = _budgets.value.filter { it.id != id }
    }

    fun addSubscription(subscription: SubscriptionItem) {
        dbHelper?.insertOrUpdateSubscription(subscription)
        _subscriptions.value = _subscriptions.value + subscription
    }

    fun deleteSubscription(id: String) {
        dbHelper?.deleteSubscription(id)
        _subscriptions.value = _subscriptions.value.filter { it.id != id }
    }

    fun resetToEmptyDatabase() {
        val helper = dbHelper ?: return
        CentwiseRustBackend.resetToEmptyDatabase()
        helper.clearAllTables()

        _transactions.value = emptyList()
        _accounts.value = emptyList()
        _budgets.value = emptyList()
        _subscriptions.value = emptyList()
    }

    fun clearLegacyStorage() {
        dbHelper?.clearAllTables()
        _transactions.value = emptyList()
        _accounts.value = emptyList()
        _budgets.value = emptyList()
        _subscriptions.value = emptyList()
    }

    companion object {
        val shared = TransactionRepository()
    }
}
