import Foundation
import Combine
import SwiftUI

public enum AnalyticsPeriod: String, CaseIterable {
    case thisMonth = "This Month"
    case lastMonth = "Last Month"
    case threeMonths = "3 Months"
    case sixMonths = "6 Months"
    case allTime = "All Time"

    public var dateRange: (start: Date, end: Date) {
        let calendar = Calendar.current
        let now = Date()
        let endOfToday = calendar.startOfDay(for: now).addingTimeInterval(86399)

        switch self {
        case .thisMonth:
            let start = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) ?? calendar.date(byAdding: .day, value: -30, to: now)!
            return (start, endOfToday)
        case .lastMonth:
            let startOfThisMonth = calendar.date(from: calendar.dateComponents([.year, .month], from: now)) ?? now
            let start = calendar.date(byAdding: .month, value: -1, to: startOfThisMonth) ?? calendar.date(byAdding: .day, value: -60, to: now)!
            let end = calendar.date(byAdding: .second, value: -1, to: startOfThisMonth) ?? now
            return (start, end)
        case .threeMonths:
            let start = calendar.date(byAdding: .month, value: -3, to: now) ?? calendar.date(byAdding: .day, value: -90, to: now)!
            return (start, endOfToday)
        case .sixMonths:
            let start = calendar.date(byAdding: .month, value: -6, to: now) ?? calendar.date(byAdding: .day, value: -180, to: now)!
            return (start, endOfToday)
        case .allTime:
            return (Date(timeIntervalSince1970: 0), endOfToday)
        }
    }

    public var daysCount: Int {
        let range = dateRange
        let diff = Calendar.current.dateComponents([.day], from: range.start, to: range.end).day ?? 30
        return max(diff, 1)
    }
}

public enum AnalyticsTypeFilter: String, CaseIterable {
    case all = "All"
    case debit = "Debit"
    case credit = "Credit"
}

public struct CategorySpendSummary: Identifiable {
    public var id: String { category.id }
    public let category: TransactionCategory
    public let totalAmount: Double
    public let percentage: Double
    public let count: Int
}

public struct MerchantSpendSummary: Identifiable {
    public var id: String { merchantName }
    public let merchantName: String
    public let totalAmount: Double
    public let transactionCount: Int
}

public final class AnalyticsViewModel: ObservableObject {
    @Published public var selectedPeriod: AnalyticsPeriod = .thisMonth
    @Published public var selectedTypeFilter: AnalyticsTypeFilter = .all

    @Published public var totalIncome: Double = 0.0
    @Published public var totalExpense: Double = 0.0
    @Published public var transactionCount: Int = 0
    @Published public var dailyAverage: Double = 0.0
    @Published public var topCategoryName: String? = nil
    @Published public var categoryBreakdown: [CategorySpendSummary] = []
    @Published public var topMerchants: [MerchantSpendSummary] = []
    @Published public var trendPoints: [TrendPoint] = []

    private var cancellables = Set<AnyCancellable>()
    private let repository: TransactionRepository

    public init(repository: TransactionRepository = .shared) {
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

    public func setPeriod(_ period: AnalyticsPeriod) {
        selectedPeriod = period
        recalculateAnalytics()
    }

    public func setTypeFilter(_ filter: AnalyticsTypeFilter) {
        selectedTypeFilter = filter
        recalculateAnalytics()
    }

    public func recalculateAnalytics() {
        let range = selectedPeriod.dateRange
        let allTxs = repository.transactions.filter { tx in
            tx.date >= range.start && tx.date <= range.end
        }

        // Compute overall period totals
        var income = 0.0
        var expense = 0.0
        for tx in allTxs {
            if tx.type == .income {
                income += tx.amount
            } else if tx.type == .expense {
                expense += tx.amount
            }
        }

        self.totalIncome = income
        self.totalExpense = expense

        // Filter transactions according to selected type (Debit / Credit / All)
        let filteredTxs: [CentwiseTransaction]
        switch selectedTypeFilter {
        case .all:
            filteredTxs = allTxs
        case .debit:
            filteredTxs = allTxs.filter { $0.type == .expense }
        case .credit:
            filteredTxs = allTxs.filter { $0.type == .income }
        }

        self.transactionCount = filteredTxs.count
        self.dailyAverage = expense / Double(selectedPeriod.daysCount)

        // Category breakdown
        var categoryTotals: [TransactionCategory: (amount: Double, count: Int)] = [:]
        var merchantTotals: [String: (amount: Double, count: Int)] = [:]

        for tx in filteredTxs {
            var catTuple = categoryTotals[tx.category, default: (0.0, 0)]
            catTuple.amount += tx.amount
            catTuple.count += 1
            categoryTotals[tx.category] = catTuple

            let m = tx.title.components(separatedBy: " - ").first ?? tx.title
            var mercTuple = merchantTotals[m, default: (0.0, 0)]
            mercTuple.amount += tx.amount
            mercTuple.count += 1
            merchantTotals[m] = mercTuple
        }

        let totalBase = max(filteredTxs.reduce(0.0) { $0 + $1.amount }, 1.0)
        self.categoryBreakdown = categoryTotals.map { cat, tuple in
            CategorySpendSummary(
                category: cat,
                totalAmount: tuple.amount,
                percentage: tuple.amount / totalBase,
                count: tuple.count
            )
        }.sorted { $0.totalAmount > $1.totalAmount }

        self.topCategoryName = self.categoryBreakdown.first?.category.name

        self.topMerchants = merchantTotals.map { name, tuple in
            MerchantSpendSummary(merchantName: name, totalAmount: tuple.amount, transactionCount: tuple.count)
        }.sorted { $0.totalAmount > $1.totalAmount }

        // Monthly trends
        let calendar = Calendar.current
        let monthsBackCount = (selectedPeriod == .threeMonths) ? 3 : ((selectedPeriod == .sixMonths) ? 6 : 6)
        let now = Date()

        self.trendPoints = (0..<monthsBackCount).reversed().compactMap { mBack in
            guard let monthDate = calendar.date(byAdding: .month, value: -mBack, to: now) else {
                return nil
            }
            let monthTotal = repository.transactions
                .filter { tx in
                    tx.type == .expense &&
                    calendar.isDate(tx.date, equalTo: monthDate, toGranularity: .month)
                }
                .reduce(0.0) { $0 + $1.amount }

            let formatter = DateFormatter()
            formatter.dateFormat = "MMM"
            return TrendPoint(label: formatter.string(from: monthDate), value: monthTotal)
        }
    }
}

