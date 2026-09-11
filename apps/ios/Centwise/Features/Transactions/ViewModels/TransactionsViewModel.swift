import Foundation
import Combine
import SwiftUI

public enum TransactionSortOrder: String, CaseIterable {
    case newestFirst = "Newest"
    case oldestFirst = "Oldest"
    case amountHigh = "Amount (High)"
    case amountLow = "Amount (Low)"
}

public enum DatePeriodFilter: String, CaseIterable {
    case thisMonth = "This Month"
    case lastMonth = "Last Month"
    case allTime = "All Time"

    public var icon: String {
        switch self {
        case .thisMonth, .lastMonth: return "calendar"
        case .allTime: return "infinity"
        }
    }
}

public final class TransactionsViewModel: ObservableObject {
    @Published public var allTransactions: [CentwiseTransaction] = []
    @Published public var filteredTransactions: [CentwiseTransaction] = []
    @Published public var searchQuery: String = ""
    @Published public var selectedPeriod: DatePeriodFilter = .allTime
    @Published public var selectedTypeFilter: TransactionType? = nil
    @Published public var selectedCategoryFilter: String? = nil
    @Published public var selectedProviderFilter: FinancialProvider? = nil
    @Published public var sortOrder: TransactionSortOrder = .newestFirst

    @Published public var totalIncome: Double = 0.0
    @Published public var totalExpense: Double = 0.0
    @Published public var totalNet: Double = 0.0

    private var cancellables = Set<AnyCancellable>()
    private let repository: TransactionRepository
    private var cachedGroupedByMonth: [(key: String, items: [CentwiseTransaction])] = []
    private let filterQueue = DispatchQueue(label: "com.centwise.transactions-filter", qos: .userInitiated)
    private var calculationID: Int = 0
    private static let monthFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter
    }()

    public init(repository: TransactionRepository = .shared) {
        self.repository = repository
        bindRepository()
    }

    private func bindRepository() {
        repository.$transactions
            .receive(on: DispatchQueue.main)
            .sink { [weak self] items in
                guard let self = self else { return }
                self.allTransactions = items
                self.applyFilters()
            }
            .store(in: &cancellables)

        Publishers.CombineLatest4($searchQuery, $selectedTypeFilter, $selectedCategoryFilter, $selectedPeriod)
            .debounce(for: .milliseconds(100), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
    }

    public func applyFilters() {
        calculationID += 1
        let requestID = calculationID
        let source = allTransactions
        let query = searchQuery.trimmingCharacters(in: .whitespaces)
        let period = selectedPeriod
        let typeFilter = selectedTypeFilter
        let categoryFilter = selectedCategoryFilter
        let providerFilter = selectedProviderFilter
        let sort = sortOrder

        filterQueue.async { [weak self] in
            var result = source
            let calendar = Calendar.current
            let today = Date()

            // Date period filter
            switch period {
            case .thisMonth:
                if let start = calendar.date(from: calendar.dateComponents([.year, .month], from: today)) {
                    result = result.filter { $0.date >= start }
                }
            case .lastMonth:
                if let thisMonthStart = calendar.date(from: calendar.dateComponents([.year, .month], from: today)),
                   let lastMonthStart = calendar.date(byAdding: .month, value: -1, to: thisMonthStart) {
                    result = result.filter { $0.date >= lastMonthStart && $0.date < thisMonthStart }
                }
            case .allTime:
                break
            }

            if !query.isEmpty {
                let q = query.lowercased()
                result = result.filter {
                    $0.title.lowercased().contains(q) ||
                    $0.accountName.lowercased().contains(q) ||
                    $0.category.name.lowercased().contains(q) ||
                    ($0.transactionReference?.lowercased().contains(q) ?? false)
                }
            }

            if let type = typeFilter {
                result = result.filter { $0.type == type }
            }

            if let catId = categoryFilter {
                result = result.filter { $0.category.id == catId }
            }

            if let provider = providerFilter {
                result = result.filter { $0.provider == provider }
            }

            switch sort {
            case .newestFirst:
                result.sort { $0.date > $1.date }
            case .oldestFirst:
                result.sort { $0.date < $1.date }
            case .amountHigh:
                result.sort { $0.amount > $1.amount }
            case .amountLow:
                result.sort { $0.amount < $1.amount }
            }

            // Calculate Totals
            var income = 0.0
            var expense = 0.0
            for tx in result {
                if tx.type == .income {
                    income += tx.amount
                } else if tx.type == .expense {
                    expense += tx.amount
                }
            }

            // Rebuild month groups off main thread
            var groups: [String: [CentwiseTransaction]] = [:]
            var order: [String] = []

            for item in result {
                let monthKey = Self.monthFormatter.string(from: item.date).uppercased()
                if groups[monthKey] == nil {
                    groups[monthKey] = []
                    order.append(monthKey)
                }
                groups[monthKey]?.append(item)
            }

            let monthGroups = order.map { (key: $0, items: groups[$0] ?? []) }

            DispatchQueue.main.async {
                guard let self = self, self.calculationID == requestID else { return }
                self.filteredTransactions = result
                self.totalIncome = income
                self.totalExpense = expense
                self.totalNet = income - expense
                self.cachedGroupedByMonth = monthGroups
            }
        }
    }

    public func deleteTransaction(id: String) {
        repository.deleteTransaction(id: id)
    }

    /// Groups transactions by Month (e.g. "AUGUST 2026")
    public var groupedByMonth: [(key: String, items: [CentwiseTransaction])] {
        cachedGroupedByMonth
    }
}
