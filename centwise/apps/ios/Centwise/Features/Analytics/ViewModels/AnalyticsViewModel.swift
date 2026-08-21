import Foundation
import Combine
import SwiftUI

public struct CategorySpendSummary: Identifiable {
    public var id: String { category.id }
    public let category: TransactionCategory
    public let totalAmount: Double
    public let percentage: Double
}

public struct MerchantSpendSummary: Identifiable {
    public var id: String { merchantName }
    public let merchantName: String
    public let totalAmount: Double
    public let transactionCount: Int
}

public final class AnalyticsViewModel: ObservableObject {
    @Published public var totalIncome: Double = 0.0
    @Published public var totalExpense: Double = 0.0
    @Published public var categoryBreakdown: [CategorySpendSummary] = []
    @Published public var topMerchants: [MerchantSpendSummary] = []
    @Published public var selectedTimeFrame: String = "This Month"

    public let timeFrames = ["This Week", "This Month", "Last 30 Days", "This Year"]

    private var cancellables = Set<AnyCancellable>()
    private let repository: FakeTransactionRepository

    public init(repository: FakeTransactionRepository = .shared) {
        self.repository = repository
        bindRepository()
    }

    private func bindRepository() {
        repository.$transactions
            .receive(on: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.recalculateAnalytics()
            }
            .store(in: &cancellables)
    }

    public func recalculateAnalytics() {
        var income = 0.0
        var expense = 0.0
        var categoryTotals: [TransactionCategory: Double] = [:]
        var merchantTotals: [String: (amount: Double, count: Int)] = [:]

        for tx in repository.transactions {
            if tx.type == .income {
                income += tx.amount
            } else if tx.type == .expense {
                expense += tx.amount
                categoryTotals[tx.category, default: 0.0] += tx.amount
                let m = tx.title.components(separatedBy: " - ").first ?? tx.title
                var curr = merchantTotals[m, default: (0.0, 0)]
                curr.amount += tx.amount
                curr.count += 1
                merchantTotals[m] = curr
            }
        }

        self.totalIncome = income
        self.totalExpense = expense

        // Category breakdown
        let totalExp = max(expense, 1.0)
        self.categoryBreakdown = categoryTotals.map { cat, amount in
            CategorySpendSummary(category: cat, totalAmount: amount, percentage: amount / totalExp)
        }.sorted { $0.totalAmount > $1.totalAmount }

        // Top merchants
        self.topMerchants = merchantTotals.map { name, tuple in
            MerchantSpendSummary(merchantName: name, totalAmount: tuple.amount, transactionCount: tuple.count)
        }.sorted { $0.totalAmount > $1.totalAmount }
    }
}
