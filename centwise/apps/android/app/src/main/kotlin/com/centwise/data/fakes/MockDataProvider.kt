package com.centwise.data.fakes

import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

object MockDataProvider {
    val sampleTransactions: List<TransactionItem> = generateOneYearTransactions()

    fun generateOneYearTransactions(): List<TransactionItem> {
        val list = mutableListOf<TransactionItem>()
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        val expenseTemplates = listOf(
            Triple("Foodpanda BD", "Food & Dining", "bKash"),
            Triple("Star Kabab Dinner", "Food & Dining", "Cash"),
            Triple("North End Coffee", "Food & Dining", "City Bank"),
            Triple("Pathao Rides", "Transport", "Nagad"),
            Triple("Uber Premier Ride", "Transport", "City Bank"),
            Triple("Unimart Superstore", "Groceries", "City Bank"),
            Triple("Agora Superstore", "Groceries", "BRAC Bank"),
            Triple("Grameenphone Flexiload", "Bills & Utilities", "bKash"),
            Triple("Daraz Online Shopping", "Shopping", "bKash"),
            Triple("Aarong Lifestyle", "Shopping", "City Bank"),
            Triple("Lazz Pharma", "Health", "Cash"),
            Triple("Cineplex Tickets", "Entertainment", "bKash")
        )

        // 1. 12 Monthly Salary and Rent entries
        for (monthOffset in 0 until 12) {
            val salaryTime = now - (monthOffset * 30L * dayMillis)
            list.add(
                TransactionItem(
                    title = "Salary Deposit",
                    amount = 85000.0,
                    type = TransactionType.INCOME,
                    category = "Salary",
                    paymentMethod = "BRAC Bank",
                    timestamp = salaryTime,
                    rawSms = "Your A/C ...8839 is credited with BDT 85,000.00 by TECH CORP SALARY."
                )
            )

            // Monthly Rent
            list.add(
                TransactionItem(
                    title = "Apartment Rent",
                    amount = 28000.0,
                    type = TransactionType.EXPENSE,
                    category = "Bills & Utilities",
                    paymentMethod = "BRAC Bank",
                    timestamp = salaryTime + 3 * dayMillis,
                    rawSms = "Debit BDT 28,000.00 for House Rent"
                )
            )
        }

        // 2. 365 Days of Daily Expenses
        for (dayOffset in 0 until 365) {
            val txCount = if (dayOffset % 3 == 0) 2 else if (dayOffset % 5 == 0) 3 else 1
            val baseTime = now - (dayOffset * dayMillis)

            for (i in 0 until txCount) {
                val t = expenseTemplates[(dayOffset * 7 + i * 3) % expenseTemplates.size]
                val amount = (150..3500).random().toDouble()
                val txTime = baseTime - (i * 3600000L * 4)

                list.add(
                    TransactionItem(
                        title = t.first,
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        category = t.second,
                        paymentMethod = t.third,
                        timestamp = txTime,
                        rawSms = "Payment of Tk $amount to ${t.first} successful."
                    )
                )
            }
        }

        return list.sortedByDescending { it.timestamp }
    }

    val sampleAccounts = listOf(
        AccountItem(
            name = "Personal bKash",
            type = "MFS Wallet",
            balance = 14250.0,
            providerName = "bKash",
            accountNumber = "017****8899"
        ),
        AccountItem(
            name = "Nagad Primary",
            type = "MFS Wallet",
            balance = 6420.0,
            providerName = "Nagad",
            accountNumber = "018****4422"
        ),
        AccountItem(
            name = "Salary Account",
            type = "Savings Bank",
            balance = 142500.0,
            providerName = "BRAC Bank",
            accountNumber = "150****8839"
        ),
        AccountItem(
            name = "CityMaxx Visa Card",
            type = "Credit Card",
            balance = 45000.0,
            providerName = "City Bank",
            accountNumber = "4029****9912"
        )
    )

    val sampleBudgets = listOf(
        BudgetItem(categoryName = "Food & Dining", allocatedAmount = 15000.0, spentAmount = 11450.0),
        BudgetItem(categoryName = "Groceries", allocatedAmount = 20000.0, spentAmount = 14850.0),
        BudgetItem(categoryName = "Transport", allocatedAmount = 5000.0, spentAmount = 2800.0),
        BudgetItem(categoryName = "Entertainment", allocatedAmount = 4000.0, spentAmount = 1200.0)
    )

    val sampleSubscriptions = listOf(
        SubscriptionItem(name = "Netflix Standard", amount = 1200.0, nextBillingDate = "Sep 15, 2026"),
        SubscriptionItem(name = "Spotify Premium", amount = 299.0, nextBillingDate = "Sep 01, 2026"),
        SubscriptionItem(name = "Carnival Internet", amount = 1150.0, nextBillingDate = "Sep 05, 2026")
    )
}
