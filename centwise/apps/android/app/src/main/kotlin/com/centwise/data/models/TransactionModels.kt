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
    val accountNumber: String
)

data class BudgetItem(
    val id: String = UUID.randomUUID().toString(),
    val categoryName: String,
    val allocatedAmount: Double,
    val spentAmount: Double,
    val period: String = "Monthly"
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
    val icon: String = "creditcard"
)
