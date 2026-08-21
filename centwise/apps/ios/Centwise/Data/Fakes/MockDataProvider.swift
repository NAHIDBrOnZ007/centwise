import Foundation

public final class MockDataProvider {
    public static let shared = MockDataProvider()

    public var accounts: [FinancialAccount] = [
        FinancialAccount(
            name: "bKash Personal",
            provider: .bkash,
            type: .mfs,
            lastFourDigits: "7821",
            currentBalance: 14250.0
        ),
        FinancialAccount(
            name: "BRAC Bank Salary",
            provider: .bracBank,
            type: .bank,
            lastFourDigits: "4190",
            currentBalance: 98400.0
        ),
        FinancialAccount(
            name: "City Bank Amex Card",
            provider: .cityBank,
            type: .card,
            lastFourDigits: "1008",
            currentBalance: -12800.0
        ),
        FinancialAccount(
            name: "Nagad Wallet",
            provider: .nagad,
            type: .mfs,
            lastFourDigits: "9042",
            currentBalance: 5320.0
        ),
        FinancialAccount(
            name: "Pocket Cash",
            provider: .cash,
            type: .cash,
            currentBalance: 3500.0
        )
    ]

    public var budgets: [CategoryBudget] = [
        CategoryBudget(
            categoryId: "food",
            categoryName: "Food & Dining",
            categoryIcon: "fork.knife",
            categoryColorHex: "#F97316",
            budgetLimit: 15000.0,
            currentSpent: 11450.0
        ),
        CategoryBudget(
            categoryId: "transport",
            categoryName: "Transport & Rides",
            categoryIcon: "car.fill",
            categoryColorHex: "#06B6D4",
            budgetLimit: 8000.0,
            currentSpent: 4200.0
        ),
        CategoryBudget(
            categoryId: "bills",
            categoryName: "Bills & Utilities",
            categoryIcon: "bolt.fill",
            categoryColorHex: "#EAB308",
            budgetLimit: 10000.0,
            currentSpent: 7500.0
        ),
        CategoryBudget(
            categoryId: "shopping",
            categoryName: "Shopping",
            categoryIcon: "bag.fill",
            categoryColorHex: "#EC4899",
            budgetLimit: 12000.0,
            currentSpent: 6800.0
        )
    ]

    public var subscriptions: [RecurringSubscription] = [
        RecurringSubscription(
            name: "Netflix Standard",
            amount: 1150.0,
            billingCycle: "Monthly",
            nextDueDate: Calendar.current.date(byAdding: .day, value: 5, to: Date()) ?? Date(),
            provider: .cityBank,
            icon: "play.tv.fill"
        ),
        RecurringSubscription(
            name: "Dot Internet 50Mbps",
            amount: 1500.0,
            billingCycle: "Monthly",
            nextDueDate: Calendar.current.date(byAdding: .day, value: 12, to: Date()) ?? Date(),
            provider: .bkash,
            icon: "wifi"
        ),
        RecurringSubscription(
            name: "Spotify Premium",
            amount: 450.0,
            billingCycle: "Monthly",
            nextDueDate: Calendar.current.date(byAdding: .day, value: 18, to: Date()) ?? Date(),
            provider: .cityBank,
            icon: "music.note"
        ),
        RecurringSubscription(
            name: "DESCO Electricity Bill",
            amount: 3200.0,
            billingCycle: "Monthly",
            nextDueDate: Calendar.current.date(byAdding: .day, value: 24, to: Date()) ?? Date(),
            provider: .bkash,
            icon: "bolt.fill"
        )
    ]

    public var transactions: [CentwiseTransaction] = [
        CentwiseTransaction(
            title: "Foodpanda - Sultans Dine",
            amount: 850.0,
            type: .expense,
            category: .food,
            date: Date(),
            accountId: "bkash-1",
            accountName: "bKash Personal",
            provider: .bkash,
            rawSmsBody: "Payment Tk 850.00 to Foodpanda Bangladesh successful. Fee Tk 0.00. Balance Tk 14,250.00. TrxID 9K8L7M6N on 22/08/2026 13:45",
            transactionReference: "9K8L7M6N",
            balanceAfter: 14250.0,
            notes: "Kacchi Platter lunch"
        ),
        CentwiseTransaction(
            title: "Pathao Ride - Gulshan 2",
            amount: 280.0,
            type: .expense,
            category: .transport,
            date: Calendar.current.date(byAdding: .hour, value: -3, to: Date()) ?? Date(),
            accountId: "nagad-1",
            accountName: "Nagad Wallet",
            provider: .nagad,
            rawSmsBody: "You paid Tk 280.00 to Pathao Rides. TxnID: NAG984128. Current Balance: Tk 5,320.00",
            transactionReference: "NAG984128",
            balanceAfter: 5320.0
        ),
        CentwiseTransaction(
            title: "Unimart Gulshan Grocery",
            amount: 3450.0,
            type: .expense,
            category: .shopping,
            date: Calendar.current.date(byAdding: .day, value: -1, to: Date()) ?? Date(),
            accountId: "city-1",
            accountName: "City Bank Amex Card",
            provider: .cityBank,
            rawSmsBody: "Your City Bank AMEX Card ending 1008 used for BDT 3,450.00 at UNIMART DHAKA on 21-AUG-26. Avail Limit BDT 187,200.00",
            transactionReference: "CBX77123"
        ),
        CentwiseTransaction(
            title: "Salary Credit - Tech Corp",
            amount: 85000.0,
            type: .income,
            category: .salary,
            date: Calendar.current.date(byAdding: .day, value: -2, to: Date()) ?? Date(),
            accountId: "brac-1",
            accountName: "BRAC Bank Salary",
            provider: .bracBank,
            rawSmsBody: "Your A/C *4190 credited by BDT 85,000.00 on 20-AUG-26 by TECH CORP SALARY. Total Balance BDT 98,400.00. Ref: SAL-AUG-26",
            transactionReference: "SAL-AUG-26",
            balanceAfter: 98400.0
        ),
        CentwiseTransaction(
            title: "Grameenphone Flexiload",
            amount: 300.0,
            type: .expense,
            category: .recharge,
            date: Calendar.current.date(byAdding: .day, value: -3, to: Date()) ?? Date(),
            accountId: "bkash-1",
            accountName: "bKash Personal",
            provider: .bkash,
            rawSmsBody: "Mobile Recharge Tk 300.00 to 01712345678 is successful. Fee Tk 0.00. Balance Tk 15,100.00. TrxID 8A7B6C5D",
            transactionReference: "8A7B6C5D",
            balanceAfter: 15100.0
        ),
        CentwiseTransaction(
            title: "Daraz Online Shopping",
            amount: 2150.0,
            type: .expense,
            category: .shopping,
            date: Calendar.current.date(byAdding: .day, value: -4, to: Date()) ?? Date(),
            accountId: "bkash-1",
            accountName: "bKash Personal",
            provider: .bkash,
            rawSmsBody: "Payment Tk 2,150.00 to Daraz Bangladesh successful. TrxID 7F6E5D4C",
            transactionReference: "7F6E5D4C"
        ),
        CentwiseTransaction(
            title: "Send Money to Mom",
            amount: 5000.0,
            type: .transfer,
            category: .transfer,
            date: Calendar.current.date(byAdding: .day, value: -5, to: Date()) ?? Date(),
            accountId: "bkash-1",
            accountName: "bKash Personal",
            provider: .bkash,
            rawSmsBody: "Send Money Tk 5,000.00 to 01811223344 successful. Fee Tk 5.00. TrxID 6P5O4N3M",
            transactionReference: "6P5O4N3M"
        )
    ]
}
