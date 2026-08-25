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

    public var transactions: [CentwiseTransaction] = MockDataProvider.generateOneYearTransactions()

    public static func generateOneYearTransactions() -> [CentwiseTransaction] {
        var list: [CentwiseTransaction] = []
        let calendar = Calendar.current
        let today = Date()

        let expenseTemplates: [(title: String, minAmt: Double, maxAmt: Double, cat: TransactionCategory, provider: FinancialProvider, accId: String, accName: String)] = [
            ("Foodpanda - Sultans Dine", 650, 1400, .food, .bkash, "bkash-1", "bKash Personal"),
            ("Star Kabab Dinner", 450, 950, .food, .cash, "cash-1", "Pocket Cash"),
            ("North End Coffee", 280, 560, .food, .cityBank, "city-1", "City Bank Amex Card"),
            ("Pathao Ride", 120, 380, .transport, .nagad, "nagad-1", "Nagad Wallet"),
            ("Uber Premier Ride", 350, 850, .transport, .cityBank, "city-1", "City Bank Amex Card"),
            ("Unimart Grocery", 2200, 6500, .groceries, .cityBank, "city-1", "City Bank Amex Card"),
            ("Agora Superstore", 1200, 3800, .groceries, .bracBank, "brac-1", "BRAC Bank Salary"),
            ("Grameenphone Flexiload", 200, 500, .recharge, .bkash, "bkash-1", "bKash Personal"),
            ("Daraz Online Shopping", 850, 4200, .shopping, .bkash, "bkash-1", "bKash Personal"),
            ("Aarong Lifestyle", 1800, 5400, .shopping, .cityBank, "city-1", "City Bank Amex Card"),
            ("Pharmacy - Lazz Pharma", 350, 1850, .health, .cash, "cash-1", "Pocket Cash"),
            ("Cineplex Movie Tickets", 900, 1800, .entertainment, .bkash, "bkash-1", "bKash Personal")
        ]

        // 1. Generate 12 months of salary on the 1st of each month
        for monthOffset in 0..<12 {
            if let monthDate = calendar.date(byAdding: .month, value: -monthOffset, to: today) {
                var components = calendar.dateComponents([.year, .month], from: monthDate)
                components.day = 1
                components.hour = 10
                components.minute = 30
                if let salaryDate = calendar.date(from: components), salaryDate <= today {
                    list.append(
                        CentwiseTransaction(
                            title: "Salary Credit - Tech Corp",
                            amount: 85000.0,
                            type: .income,
                            category: .salary,
                            date: salaryDate,
                            accountId: "brac-1",
                            accountName: "BRAC Bank Salary",
                            provider: .bracBank,
                            rawSmsBody: "Your A/C *4190 credited by BDT 85,000.00 on \(salaryDate.formatted(date: .abbreviated, time: .omitted)) by TECH CORP SALARY. Ref: SAL-\(monthOffset)",
                            transactionReference: "SAL-\(1000 + monthOffset)",
                            balanceAfter: 98400.0
                        )
                    )

                    // Add Monthly Utilities / Rent
                    if let billDate = calendar.date(byAdding: .day, value: 5, to: salaryDate), billDate <= today {
                        list.append(
                            CentwiseTransaction(
                                title: "Apartment Rent & Service",
                                amount: 28000.0,
                                type: .expense,
                                category: .bills,
                                date: billDate,
                                accountId: "brac-1",
                                accountName: "BRAC Bank Salary",
                                provider: .bracBank,
                                rawSmsBody: "Your A/C *4190 debited BDT 28,000.00 to House Rent on \(billDate.formatted(date: .abbreviated, time: .omitted))",
                                transactionReference: "RENT-\(monthOffset)"
                            )
                        )
                    }

                    if let elecDate = calendar.date(byAdding: .day, value: 12, to: salaryDate), elecDate <= today {
                        list.append(
                            CentwiseTransaction(
                                title: "DESCO Electricity Bill",
                                amount: Double(Int.random(in: 2800...4500)),
                                type: .expense,
                                category: .bills,
                                date: elecDate,
                                accountId: "bkash-1",
                                accountName: "bKash Personal",
                                provider: .bkash,
                                rawSmsBody: "DESCO bill payment successful. Fee Tk 0.00",
                                transactionReference: "DESCO-\(monthOffset)"
                            )
                        )
                    }

                    if let netDate = calendar.date(byAdding: .day, value: 15, to: salaryDate), netDate <= today {
                        list.append(
                            CentwiseTransaction(
                                title: "Dot Internet 50Mbps",
                                amount: 1500.0,
                                type: .expense,
                                category: .bills,
                                date: netDate,
                                accountId: "bkash-1",
                                accountName: "bKash Personal",
                                provider: .bkash,
                                rawSmsBody: "Payment of Tk 1,500.00 to Dot Internet successful."
                            )
                        )
                    }
                }
            }
        }

        // 2. Generate daily/weekly random realistic transactions across the entire past 365 days
        for dayOffset in 0..<365 {
            // Generate 1-3 transactions per day randomly
            let txCount = (dayOffset % 3 == 0) ? 2 : (dayOffset % 5 == 0 ? 3 : 1)
            guard let baseDate = calendar.date(byAdding: .day, value: -dayOffset, to: today) else { continue }

            for i in 0..<txCount {
                let templateIndex = (dayOffset * 7 + i * 3) % expenseTemplates.count
                let template = expenseTemplates[templateIndex]
                let amount = Double(Int.random(in: Int(template.minAmt)...Int(template.maxAmt)))

                let hour = (8 + (dayOffset * 3 + i * 5) % 14)
                let minute = (i * 17 + dayOffset * 11) % 60
                var comps = calendar.dateComponents([.year, .month, .day], from: baseDate)
                comps.hour = hour
                comps.minute = minute
                let txDate = calendar.date(from: comps) ?? baseDate

                list.append(
                    CentwiseTransaction(
                        title: template.title,
                        amount: amount,
                        type: .expense,
                        category: template.cat,
                        date: txDate,
                        accountId: template.accId,
                        accountName: template.accName,
                        provider: template.provider,
                        rawSmsBody: "Auto-parsed SMS notification for \(template.title) Tk \(amount)",
                        transactionReference: "TX\(dayOffset)\(i)\(Int.random(in: 100...999))"
                    )
                )
            }

            // Occasional Transfer or Freelance / Gift Income (every 14-20 days)
            if dayOffset % 18 == 0 && dayOffset > 0 {
                let freelanceDate = calendar.date(byAdding: .hour, value: 14, to: baseDate) ?? baseDate
                list.append(
                    CentwiseTransaction(
                        title: "Freelance Project Payment",
                        amount: Double(Int.random(in: 15000...35000)),
                        type: .income,
                        category: .salary,
                        date: freelanceDate,
                        accountId: "city-1",
                        accountName: "City Bank Amex Card",
                        provider: .cityBank,
                        rawSmsBody: "Credit inward remittance received. Ref: UPWORK-REM"
                    )
                )
            }
        }

        // Sort descending (newest first)
        return list.sorted { $0.date > $1.date }
    }
}
