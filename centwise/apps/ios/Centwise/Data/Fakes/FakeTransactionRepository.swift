import Foundation
import Combine

public protocol TransactionRepositoryProtocol {
    func getTransactions() -> AnyPublisher<[CentwiseTransaction], Never>
    func getAccounts() -> AnyPublisher<[FinancialAccount], Never>
    func getBudgets() -> AnyPublisher<[CategoryBudget], Never>
    func getSubscriptions() -> AnyPublisher<[RecurringSubscription], Never>
    func addTransaction(_ transaction: CentwiseTransaction)
    func updateTransaction(_ transaction: CentwiseTransaction)
    func deleteTransaction(id: String)
    func addAccount(_ account: FinancialAccount)
}

public final class FakeTransactionRepository: TransactionRepositoryProtocol, ObservableObject {
    public static let shared = FakeTransactionRepository()

    @Published public private(set) var transactions: [CentwiseTransaction] = []
    @Published public private(set) var accounts: [FinancialAccount] = []
    @Published public private(set) var budgets: [CategoryBudget] = []
    @Published public private(set) var subscriptions: [RecurringSubscription] = []

    public init() {
        self.transactions = MockDataProvider.shared.transactions
        self.accounts = MockDataProvider.shared.accounts
        self.budgets = MockDataProvider.shared.budgets
        self.subscriptions = MockDataProvider.shared.subscriptions
    }

    public func getTransactions() -> AnyPublisher<[CentwiseTransaction], Never> {
        $transactions.eraseToAnyPublisher()
    }

    public func getAccounts() -> AnyPublisher<[FinancialAccount], Never> {
        $accounts.eraseToAnyPublisher()
    }

    public func getBudgets() -> AnyPublisher<[CategoryBudget], Never> {
        $budgets.eraseToAnyPublisher()
    }

    public func getSubscriptions() -> AnyPublisher<[RecurringSubscription], Never> {
        $subscriptions.eraseToAnyPublisher()
    }

    public func addTransaction(_ transaction: CentwiseTransaction) {
        transactions.insert(transaction, at: 0)
        CentwiseNotifications.notifyNewTransaction(transaction)
    }

    public func updateTransaction(_ transaction: CentwiseTransaction) {
        if let index = transactions.firstIndex(where: { $0.id == transaction.id }) {
            transactions[index] = transaction
        }
    }

    public func deleteTransaction(id: String) {
        transactions.removeAll { $0.id == id }
    }

    public func addAccount(_ account: FinancialAccount) {
        accounts.append(account)
    }

    public func addBudget(_ budget: CategoryBudget) {
        budgets.append(budget)
    }

    public func updateBudget(_ budget: CategoryBudget) {
        if let index = budgets.firstIndex(where: { $0.id == budget.id }) {
            budgets[index] = budget
        }
    }

    public func deleteBudget(id: String) {
        budgets.removeAll { $0.id == id }
    }

    public func addSubscription(_ subscription: RecurringSubscription) {
        subscriptions.append(subscription)
    }

    public func updateSubscription(_ subscription: RecurringSubscription) {
        if let index = subscriptions.firstIndex(where: { $0.id == subscription.id }) {
            subscriptions[index] = subscription
        }
    }

    public func deleteSubscription(id: String) {
        subscriptions.removeAll { $0.id == id }
    }
}
