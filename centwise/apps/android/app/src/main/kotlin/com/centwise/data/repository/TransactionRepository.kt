package com.centwise.data.repository

import android.content.Context
import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
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
        val helper = dbHelper ?: return

        // 1. Transactions
        _transactions.value = helper.getAllTransactions()

        // 2. Accounts
        val loadedAccs = helper.getAllAccounts()
        if (loadedAccs.isNotEmpty()) {
            _accounts.value = loadedAccs
        } else {
            val starterAccs = defaultStarterAccounts
            starterAccs.forEach { helper.insertOrUpdateAccount(it) }
            _accounts.value = starterAccs
        }

        // 3. Budgets (Clean empty by default)
        _budgets.value = helper.getAllBudgets()

        // 4. Subscriptions (Clean empty by default)
        _subscriptions.value = helper.getAllSubscriptions()
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
        helper.clearAllTables()

        defaultStarterAccounts.forEach { helper.insertOrUpdateAccount(it) }
        defaultStarterBudgets.forEach { helper.insertOrUpdateBudget(it) }

        _transactions.value = emptyList()
        _accounts.value = defaultStarterAccounts
        _budgets.value = defaultStarterBudgets
        _subscriptions.value = emptyList()
    }

    companion object {
        val defaultStarterAccounts = listOf(
            AccountItem(name = "bKash", type = "MFS", balance = 0.0, providerName = "bKash", accountNumber = "bKash Wallet"),
            AccountItem(name = "Nagad", type = "MFS", balance = 0.0, providerName = "Nagad", accountNumber = "Nagad Wallet"),
            AccountItem(name = "Bank Account", type = "Bank", balance = 0.0, providerName = "Bank", accountNumber = "Primary Bank A/C"),
            AccountItem(name = "Cash Wallet", type = "Cash", balance = 0.0, providerName = "Cash", accountNumber = "Cash")
        )

        val defaultStarterBudgets = listOf(
            BudgetItem(categoryName = "Food & Dining", allocatedAmount = 10000.0, spentAmount = 0.0),
            BudgetItem(categoryName = "Transport & Rides", allocatedAmount = 5000.0, spentAmount = 0.0),
            BudgetItem(categoryName = "Bills & Utilities", allocatedAmount = 8000.0, spentAmount = 0.0),
            BudgetItem(categoryName = "Shopping", allocatedAmount = 7000.0, spentAmount = 0.0)
        )

        val shared = TransactionRepository()
    }
}
