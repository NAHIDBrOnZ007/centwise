import Foundation
import Combine
import SwiftUI

public final class HomeViewModel: ObservableObject {
    @Published public var recentTransactions: [CentwiseTransaction] = []
    @Published public var accounts: [FinancialAccount] = []
    @Published public var budgets: [CategoryBudget] = []
    @Published public var subscriptions: [RecurringSubscription] = []

    @Published public var monthlyExpense: Double = 0.0
    @Published public var monthlyIncome: Double = 0.0
    @Published public var monthlyNet: Double = 0.0
    @Published public var totalNetWorth: Double = 0.0

    private var cancellables = Set<AnyCancellable>()
    private let repository: FakeTransactionRepository

    public init(repository: FakeTransactionRepository = .shared) {
        self.repository = repository
        bindRepository()
    }

    public func loadHome() {
        calculateMetrics()
    }

    private func bindRepository() {
        repository.$transactions
            .receive(on: DispatchQueue.main)
            .sink { [weak self] items in
                guard let self = self else { return }
                self.recentTransactions = Array(items.prefix(5))
                self.calculateMetrics()
            }
            .store(in: &cancellables)

        repository.$accounts
            .receive(on: DispatchQueue.main)
            .sink { [weak self] items in
                guard let self = self else { return }
                self.accounts = items.filter { !$0.isArchived }
                self.totalNetWorth = items.reduce(0) { $0 + $1.currentBalance }
            }
            .store(in: &cancellables)

        repository.$budgets
            .receive(on: DispatchQueue.main)
            .assign(to: &$budgets)

        repository.$subscriptions
            .receive(on: DispatchQueue.main)
            .assign(to: &$subscriptions)
    }

    private func calculateMetrics() {
        var expense = 0.0
        var income = 0.0

        for item in repository.transactions {
            switch item.type {
            case .expense:
                expense += item.amount
            case .income:
                income += item.amount
            case .transfer, .refund:
                break
            }
        }

        self.monthlyExpense = expense
        self.monthlyIncome = income
        self.monthlyNet = income - expense
    }
}
