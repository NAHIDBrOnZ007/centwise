import Foundation
import Combine
import SwiftUI

public enum TransactionSortOrder: String, CaseIterable {
    case newestFirst = "Newest"
    case oldestFirst = "Oldest"
    case amountHigh = "Amount (High)"
    case amountLow = "Amount (Low)"
}

public final class TransactionsViewModel: ObservableObject {
    @Published public var allTransactions: [CentwiseTransaction] = []
    @Published public var filteredTransactions: [CentwiseTransaction] = []
    @Published public var searchQuery: String = ""
    @Published public var selectedTypeFilter: TransactionType? = nil
    @Published public var selectedCategoryFilter: String? = nil
    @Published public var selectedProviderFilter: FinancialProvider? = nil
    @Published public var sortOrder: TransactionSortOrder = .newestFirst

    private var cancellables = Set<AnyCancellable>()
    private let repository: FakeTransactionRepository

    public init(repository: FakeTransactionRepository = .shared) {
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

        Publishers.CombineLatest4($searchQuery, $selectedTypeFilter, $selectedCategoryFilter, $selectedProviderFilter)
            .debounce(for: .milliseconds(100), scheduler: DispatchQueue.main)
            .sink { [weak self] _ in
                self?.applyFilters()
            }
            .store(in: &cancellables)
    }

    public func applyFilters() {
        var result = allTransactions

        if !searchQuery.trimmingCharacters(in: .whitespaces).isEmpty {
            let q = searchQuery.lowercased()
            result = result.filter {
                $0.title.lowercased().contains(q) ||
                $0.accountName.lowercased().contains(q) ||
                $0.category.name.lowercased().contains(q) ||
                ($0.transactionReference?.lowercased().contains(q) ?? false)
            }
        }

        if let type = selectedTypeFilter {
            result = result.filter { $0.type == type }
        }

        if let catId = selectedCategoryFilter {
            result = result.filter { $0.category.id == catId }
        }

        if let provider = selectedProviderFilter {
            result = result.filter { $0.provider == provider }
        }

        switch sortOrder {
        case .newestFirst:
            result.sort { $0.date > $1.date }
        case .oldestFirst:
            result.sort { $0.date < $1.date }
        case .amountHigh:
            result.sort { $0.amount > $1.amount }
        case .amountLow:
            result.sort { $0.amount < $1.amount }
        }

        self.filteredTransactions = result
    }

    public func deleteTransaction(id: String) {
        repository.deleteTransaction(id: id)
    }

    /// Groups transactions by date for Section headers
    public var groupedTransactions: [(String, [CentwiseTransaction])] {
        let calendar = Calendar.current
        let grouped = Dictionary(grouping: filteredTransactions) { tx in
            calendar.startOfDay(for: tx.date)
        }

        let sortedKeys = grouped.keys.sorted(by: >)
        return sortedKeys.map { date in
            let title = DateFormatterHelper.shared.sectionHeaderTitle(for: date)
            return (title, grouped[date] ?? [])
        }
    }
}
