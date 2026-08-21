package com.centwise.data.fakes

import com.centwise.data.models.AccountItem
import com.centwise.data.models.BudgetItem
import com.centwise.data.models.SubscriptionItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType

object MockDataProvider {
    val sampleTransactions = listOf(
        TransactionItem(
            title = "Foodpanda BD",
            amount = 650.0,
            type = TransactionType.EXPENSE,
            category = "Food & Dining",
            paymentMethod = "bKash",
            rawSms = "Payment Tk 650.00 to Foodpanda successful. Ref: FP8392. Fee Tk 0.00. Balance Tk 14,250.00. TrxID 9K38AL492 at 21/08/2026 19:42"
        ),
        TransactionItem(
            title = "Pathao Rides",
            amount = 280.0,
            type = TransactionType.EXPENSE,
            category = "Transport",
            paymentMethod = "Nagad",
            rawSms = "Payment of Tk 280.00 to Pathao successful. TrxID 77H88219. Balance: Tk 6,420.00."
        ),
        TransactionItem(
            title = "Salary Deposit",
            amount = 85000.0,
            type = TransactionType.INCOME,
            category = "Salary",
            paymentMethod = "BRAC Bank",
            rawSms = "Your A/C ...8839 is credited with BDT 85,000.00 on 01-Aug-2026 by SALARY. Avail Bal BDT 142,500.00."
        ),
        TransactionItem(
            title = "Unimart Superstore",
            amount = 4850.0,
            type = TransactionType.EXPENSE,
            category = "Groceries",
            paymentMethod = "City Bank",
            rawSms = "Approved at UNIMART GULSHAN for BDT 4,850.00 with Card ...4029 on 19-AUG-26 18:20."
        ),
        TransactionItem(
            title = "DESCO Electricity Bill",
            amount = 2450.0,
            type = TransactionType.EXPENSE,
            category = "Bills & Utilities",
            paymentMethod = "bKash",
            rawSms = "Bill Payment to DESCO for Tk 2,450.00 is successful. TrxID 8M92019A."
        ),
        TransactionItem(
            title = "Netflix Bangladesh",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            category = "Entertainment",
            paymentMethod = "City Bank",
            rawSms = "Approved at NETFLIX.COM for BDT 1,200.00 with Card ...4029."
        )
    )

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
