package com.centwise.data.fakes

import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionRepository private constructor() {

    private val _transactions = MutableStateFlow(MockDataProvider.sampleTransactions)
    val transactions: StateFlow<List<TransactionItem>> = _transactions.asStateFlow()

    private val _accounts = MutableStateFlow(MockDataProvider.sampleAccounts)
    val accounts: StateFlow<List<AccountItem>> = _accounts.asStateFlow()

    private val _budgets = MutableStateFlow(MockDataProvider.sampleBudgets)
    val budgets: StateFlow<List<BudgetItem>> = _budgets.asStateFlow()

    private val _subscriptions = MutableStateFlow(MockDataProvider.sampleSubscriptions)
    val subscriptions: StateFlow<List<SubscriptionItem>> = _subscriptions.asStateFlow()

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

    fun addTransaction(tx: TransactionItem) {
        _transactions.value = listOf(tx) + _transactions.value
    }

    fun deleteTransaction(id: String) {
        _transactions.value = _transactions.value.filter { it.id != id }
    }

    fun addAccount(account: AccountItem) {
        _accounts.value = _accounts.value + account
    }

    fun addBudget(budget: BudgetItem) {
        _budgets.value = _budgets.value + budget
    }

    fun updateBudget(budget: BudgetItem) {
        _budgets.value = _budgets.value.map { if (it.id == budget.id) budget else it }
    }

    fun deleteBudget(id: String) {
        _budgets.value = _budgets.value.filter { it.id != id }
    }

    fun addSubscription(subscription: SubscriptionItem) {
        _subscriptions.value = _subscriptions.value + subscription
    }

    fun deleteSubscription(id: String) {
        _subscriptions.value = _subscriptions.value.filter { it.id != id }
    }

    companion object {
        val shared = FakeTransactionRepository()
    }
}
