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
    private let analyticsQueue = DispatchQueue(label: "com.centwise.analytics", qos: .userInitiated)
    private var calculationID = 0

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
        let typeFilter: String = switch selectedTypeFilter {
        case .all: "all"
        case .debit: "debit"
        case .credit: "credit"
        }
        let monthsBack: UInt32 = selectedPeriod == .threeMonths ? 3 : 6
        calculationID += 1
        let requestID = calculationID

        analyticsQueue.async { [weak self] in
            let snapshot = CentwiseRustBackend.analyticsSnapshot(
                start: range.start,
                end: range.end.addingTimeInterval(1),
                monthsBack: monthsBack,
                typeFilter: typeFilter
            )
            DispatchQueue.main.async {
                guard let self, requestID == self.calculationID else { return }
                self.apply(snapshot: snapshot)
            }
        }
    }

    private func apply(snapshot: AnalyticsSnapshotRecord?) {
        guard let snapshot else {
            totalIncome = 0
            totalExpense = 0
            transactionCount = 0
            dailyAverage = 0
            categoryBreakdown = []
            topCategoryName = nil
            topMerchants = []
            trendPoints = []
            return
        }
        totalIncome = Double(snapshot.totalIncomeMinor) / 100
        totalExpense = Double(snapshot.totalExpenseMinor) / 100
        transactionCount = Int(snapshot.transactionCount)
        dailyAverage = totalExpense / Double(selectedPeriod.daysCount)
        let totalBase = max(snapshot.categoryBreakdown.reduce(0.0) { $0 + Double($1.totalMinor) / 100 }, 1)
        categoryBreakdown = snapshot.categoryBreakdown.map { item in
            CategorySpendSummary(
                category: TransactionCategory(
                    id: item.categoryId,
                    name: item.categoryName,
                    icon: item.categoryIcon,
                    colorHex: item.categoryColorHex,
                    isSystem: true
                ),
                totalAmount: Double(item.totalMinor) / 100,
                percentage: Double(item.totalMinor) / 100 / totalBase,
                count: Int(item.transactionCount)
            )
        }
        topCategoryName = categoryBreakdown.first?.category.name
        topMerchants = snapshot.topMerchants.map {
            MerchantSpendSummary(
                merchantName: $0.merchant,
                totalAmount: Double($0.totalMinor) / 100,
                transactionCount: Int($0.transactionCount)
            )
        }
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM"
        trendPoints = snapshot.monthlyTrends.compactMap { item in
            var components = DateComponents()
            components.year = Int(item.year)
            components.month = Int(item.month)
            guard let date = Calendar.current.date(from: components) else { return nil }
            return TrendPoint(label: formatter.string(from: date), value: Double(item.totalExpenseMinor) / 100)
        }
    }

    public func transactions(forCategory categoryId: String) -> [CentwiseTransaction] {
        let range = selectedPeriod.dateRange
        return repository.transactions.filter { tx in
            tx.date >= range.start && tx.date <= range.end && tx.category.id == categoryId
        }.sorted { $0.date > $1.date }
    }

    public func transactions(forMerchant merchantName: String) -> [CentwiseTransaction] {
        let range = selectedPeriod.dateRange
        return repository.transactions.filter { tx in
            tx.date >= range.start && tx.date <= range.end &&
            (tx.title.components(separatedBy: " - ").first ?? tx.title) == merchantName
        }.sorted { $0.date > $1.date }
    }
}
