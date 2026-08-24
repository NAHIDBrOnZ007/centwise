import Foundation
import SwiftUI

// MARK: - Transaction Types
public enum TransactionType: String, CaseIterable, Identifiable, Codable {
    case expense = "Expense"
    case income = "Income"
    case transfer = "Transfer"
    case refund = "Refund"

    public var id: String { rawValue }

    public var icon: String {
        switch self {
        case .expense: return "arrow.up.right"
        case .income: return "arrow.down.left"
        case .transfer: return "arrow.left.arrow.right"
        case .refund: return "arrow.uturn.left"
        }
    }

    public var color: Color {
        switch self {
        case .expense: return CentwiseColors.expenseRed
        case .income: return CentwiseColors.incomeGreen
        case .transfer: return CentwiseColors.transferBlue
        case .refund: return CentwiseColors.refundAmber
        }
    }
}

// MARK: - Account Types
public enum AccountType: String, CaseIterable, Identifiable, Codable {
    case mfs = "Mobile Financial Service"
    case bank = "Bank Account"
    case card = "Credit/Debit Card"
    case cash = "Cash Wallet"

    public var id: String { rawValue }

    public var defaultIcon: String {
        switch self {
        case .mfs: return "iphone.gen3"
        case .bank: return "building.columns.fill"
        case .card: return "creditcard.fill"
        case .cash: return "banknote.fill"
        }
    }
}

// MARK: - Provider
public enum FinancialProvider: String, CaseIterable, Identifiable, Codable {
    case bkash = "bKash"
    case nagad = "Nagad"
    case rocket = "Rocket"
    case upay = "Upay"
    case cellfin = "Cellfin"
    case cityBank = "City Bank"
    case bracBank = "BRAC Bank"
    case easternBank = "Eastern Bank"
    case dutchBangla = "Dutch-Bangla Bank"
    case standardChartered = "Standard Chartered"
    case cash = "Cash"
    case other = "Other"

    public var id: String { rawValue }

    public var brandColor: Color {
        switch self {
        case .bkash: return CentwiseColors.bKashPink
        case .nagad: return CentwiseColors.nagadOrange
        case .rocket: return CentwiseColors.rocketPurple
        case .upay: return CentwiseColors.upayBlue
        case .cellfin: return CentwiseColors.cellfinGreen
        case .cityBank: return CentwiseColors.cityBankRed
        case .bracBank: return CentwiseColors.bracBankBlue
        case .easternBank: return CentwiseColors.easternBankGold
        case .dutchBangla: return Color.blue
        case .standardChartered: return Color.green
        case .cash: return CentwiseColors.primaryEmerald
        case .other: return Color.gray
        }
    }

    public var icon: String {
        switch self {
        case .bkash, .nagad, .rocket, .upay, .cellfin: return "phone.bubble.left.fill"
        case .cityBank, .bracBank, .easternBank, .dutchBangla, .standardChartered: return "building.columns.fill"
        case .cash: return "banknote.fill"
        case .other: return "wallet.pass.fill"
        }
    }
}

// MARK: - Category
public struct TransactionCategory: Identifiable, Hashable, Codable {
    public let id: String
    public let name: String
    public let icon: String
    public let colorHex: String

    public init(id: String, name: String, icon: String, colorHex: String) {
        self.id = id
        self.name = name
        self.icon = icon
        self.colorHex = colorHex
    }

    public var color: Color {
        Color(hex: colorHex) ?? CentwiseColors.primaryEmerald
    }

    public static let food = TransactionCategory(id: "food", name: "Food & Dining", icon: "fork.knife", colorHex: "#F97316")
    public static let groceries = TransactionCategory(id: "groceries", name: "Groceries", icon: "cart.fill", colorHex: "#22C55E")
    public static let transport = TransactionCategory(id: "transport", name: "Transport & Rides", icon: "car.fill", colorHex: "#06B6D4")
    public static let shopping = TransactionCategory(id: "shopping", name: "Shopping", icon: "bag.fill", colorHex: "#EC4899")
    public static let bills = TransactionCategory(id: "bills", name: "Bills & Utilities", icon: "bolt.fill", colorHex: "#EAB308")
    public static let recharge = TransactionCategory(id: "recharge", name: "Mobile Recharge", icon: "antenna.radiowaves.left.and.right", colorHex: "#8B5CF6")
    public static let salary = TransactionCategory(id: "salary", name: "Salary & Income", icon: "banknote.fill", colorHex: "#10B981")
    public static let transfer = TransactionCategory(id: "transfer", name: "Transfers", icon: "arrow.left.arrow.right", colorHex: "#3B82F6")
    public static let health = TransactionCategory(id: "health", name: "Healthcare", icon: "cross.case.fill", colorHex: "#EF4444")
    public static let entertainment = TransactionCategory(id: "entertainment", name: "Entertainment", icon: "play.tv.fill", colorHex: "#6366F1")
    public static let education = TransactionCategory(id: "education", name: "Education", icon: "book.closed.fill", colorHex: "#14B8A6")
    public static let other = TransactionCategory(id: "other", name: "Other", icon: "square.grid.2x2.fill", colorHex: "#64748B")

    public static let defaultCategories: [TransactionCategory] = [
        .food, .groceries, .transport, .shopping, .bills, .recharge, .salary, .transfer, .health, .entertainment, .education, .other
    ]
}

// MARK: - Account
public struct FinancialAccount: Identifiable, Hashable, Codable {
    public let id: String
    public var name: String
    public var provider: FinancialProvider
    public var type: AccountType
    public var lastFourDigits: String?
    public var currentBalance: Double
    public var isArchived: Bool

    public init(
        id: String = UUID().uuidString,
        name: String,
        provider: FinancialProvider,
        type: AccountType,
        lastFourDigits: String? = nil,
        currentBalance: Double = 0.0,
        isArchived: Bool = false
    ) {
        self.id = id
        self.name = name
        self.provider = provider
        self.type = type
        self.lastFourDigits = lastFourDigits
        self.currentBalance = currentBalance
        self.isArchived = isArchived
    }
}

// MARK: - Transaction
public struct CentwiseTransaction: Identifiable, Hashable, Codable {
    public let id: String
    public var title: String
    public var amount: Double
    public var currency: String
    public var type: TransactionType
    public var category: TransactionCategory
    public var date: Date
    public var accountId: String
    public var accountName: String
    public var provider: FinancialProvider
    public var rawSmsBody: String?
    public var transactionReference: String?
    public var balanceAfter: Double?
    public var notes: String?
    public var isAutoTracked: Bool

    public init(
        id: String = UUID().uuidString,
        title: String,
        amount: Double,
        currency: String = "BDT",
        type: TransactionType,
        category: TransactionCategory,
        date: Date = Date(),
        accountId: String,
        accountName: String,
        provider: FinancialProvider,
        rawSmsBody: String? = nil,
        transactionReference: String? = nil,
        balanceAfter: Double? = nil,
        notes: String? = nil,
        isAutoTracked: Bool = true
    ) {
        self.id = id
        self.title = title
        self.amount = amount
        self.currency = currency
        self.type = type
        self.category = category
        self.date = date
        self.accountId = accountId
        self.accountName = accountName
        self.provider = provider
        self.rawSmsBody = rawSmsBody
        self.transactionReference = transactionReference
        self.balanceAfter = balanceAfter
        self.notes = notes
        self.isAutoTracked = isAutoTracked
    }
}

// MARK: - Budget
public struct CategoryBudget: Identifiable, Hashable, Codable {
    public let id: String
    public var categoryId: String
    public var categoryName: String
    public var categoryIcon: String
    public var categoryColorHex: String
    public var budgetLimit: Double
    public var currentSpent: Double

    public init(
        id: String = UUID().uuidString,
        categoryId: String,
        categoryName: String,
        categoryIcon: String,
        categoryColorHex: String,
        budgetLimit: Double,
        currentSpent: Double
    ) {
        self.id = id
        self.categoryId = categoryId
        self.categoryName = categoryName
        self.categoryIcon = categoryIcon
        self.categoryColorHex = categoryColorHex
        self.budgetLimit = budgetLimit
        self.currentSpent = currentSpent
    }

    public var percentage: Double {
        guard budgetLimit > 0 else { return 0 }
        return min(currentSpent / budgetLimit, 1.0)
    }

    public var isOverBudget: Bool {
        currentSpent > budgetLimit
    }

    public var remainingAmount: Double {
        max(budgetLimit - currentSpent, 0)
    }
}

// MARK: - Subscription
public struct RecurringSubscription: Identifiable, Hashable, Codable {
    public let id: String
    public var name: String
    public var amount: Double
    public var billingCycle: String // Monthly, Yearly
    public var nextDueDate: Date
    public var provider: FinancialProvider
    public var isActive: Bool
    public var icon: String

    public init(
        id: String = UUID().uuidString,
        name: String,
        amount: Double,
        billingCycle: String = "Monthly",
        nextDueDate: Date,
        provider: FinancialProvider = .bkash,
        isActive: Bool = true,
        icon: String = "play.tv.fill"
    ) {
        self.id = id
        self.name = name
        self.amount = amount
        self.billingCycle = billingCycle
        self.nextDueDate = nextDueDate
        self.provider = provider
        self.isActive = isActive
        self.icon = icon
    }
}

// MARK: - Color Hex Helper Extension
extension Color {
    public init?(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")

        var rgb: UInt64 = 0
        guard Scanner(string: hexSanitized).scanHexInt64(&rgb) else { return nil }

        let r = Double((rgb & 0xFF0000) >> 16) / 255.0
        let g = Double((rgb & 0x00FF00) >> 8) / 255.0
        let b = Double(rgb & 0x0000FF) / 255.0

        self.init(red: r, green: g, blue: b)
    }
}
