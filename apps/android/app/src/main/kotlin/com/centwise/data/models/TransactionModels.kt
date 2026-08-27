package com.centwise.data.models

import java.util.UUID

enum class TransactionType(val displayName: String) {
    INCOME("Income"),
    EXPENSE("Expense"),
    TRANSFER("Transfer"),
    CREDIT("Credit")
}

data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val paymentMethod: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val rawSms: String? = null,
    val reference: String? = null
)

data class AccountItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val balance: Double,
    val providerName: String,
    val accountNumber: String,
    val archived: Boolean = false
)

data class BudgetItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryName: String,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val period: String = "Monthly",
    val categoryId: String = "",
    val startEpochMs: Long = 0L,
    val endEpochMs: Long = 0L
) {
    val progress: Float
        get() = if (allocatedAmount > 0) (spentAmount / allocatedAmount).toFloat().coerceIn(0f, 1f) else 0f
}

data class SubscriptionItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val billingCycle: String = "Monthly",
    val nextBillingDate: String,
    val icon: String = "creditcard",
    val nextDueEpochMs: Long = 0L,
    val isActive: Boolean = true
)
