import Foundation
import Combine
import UIKit

public extension Notification.Name {
    static let centwiseTransactionsUpdated = Notification.Name("centwiseTransactionsUpdated")
}

public protocol TransactionRepositoryProtocol {
    func getTransactions() -> AnyPublisher<[CentwiseTransaction], Never>
    func getAccounts() -> AnyPublisher<[FinancialAccount], Never>
    func getBudgets() -> AnyPublisher<[CategoryBudget], Never>
    func getSubscriptions() -> AnyPublisher<[RecurringSubscription], Never>
    @discardableResult func addTransaction(_ transaction: CentwiseTransaction) -> Bool
    @discardableResult func updateTransaction(_ transaction: CentwiseTransaction) -> Bool
    func deleteTransaction(id: String)
    func addAccount(_ account: FinancialAccount)
}

/// iOS's thin observable adapter over the single Rust-owned SQLite database.
/// These published arrays are UI caches only; they are never used as storage.
public final class TransactionRepository: TransactionRepositoryProtocol, ObservableObject {
    public static let shared = TransactionRepository()

    @Published public private(set) var transactions: [CentwiseTransaction] = []
    @Published public private(set) var accounts: [FinancialAccount] = []
    @Published public private(set) var budgets: [CategoryBudget] = []
    @Published public private(set) var subscriptions: [RecurringSubscription] = []
    @Published public private(set) var categories: [TransactionCategory] = []
    @Published public private(set) var homeDashboard: HomeDashboardRecord? = nil
    private var notificationObservers: [NSObjectProtocol] = []
    private let loadQueue = DispatchQueue(label: "com.centwise.repository-load", qos: .userInitiated)
    private var isLoading = false
    private var refreshPending = false
    private var refreshWorkItem: DispatchWorkItem?

    public init() {
        loadFromRust()
        setupNotificationObservers()
    }

    private func setupNotificationObservers() {
        notificationObservers.append(NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: nil
        ) { [weak self] _ in self?.loadFromRust() }
        )

        notificationObservers.append(NotificationCenter.default.addObserver(
            forName: .centwiseTransactionsUpdated,
            object: nil,
            queue: nil
        ) { [weak self] _ in self?.loadFromRust() }
        )
    }

    deinit {
        notificationObservers.forEach { NotificationCenter.default.removeObserver($0) }
    }

    public func loadFromRust() {
        loadQueue.async { [weak self] in
            guard let self else { return }
            self.refreshWorkItem?.cancel()
            let workItem = DispatchWorkItem { [weak self] in
                self?.performLoadFromRust()
            }
            self.refreshWorkItem = workItem
            self.loadQueue.asyncAfter(deadline: .now() + .milliseconds(50), execute: workItem)
        }
    }

    private func performLoadFromRust() {
        guard !isLoading else {
            refreshPending = true
            return
        }
        isLoading = true
        guard CentwiseRustBackend.isAvailable() else {
            DispatchQueue.main.async {
                self.transactions = []
                self.accounts = []
                self.budgets = []
                self.subscriptions = []
                self.categories = []
                self.homeDashboard = nil
            }
            finishLoad()
            return
        }

        let categoryRecords = CentwiseRustBackend.listCategories()
        let loadedCategories = categoryRecords.map { record in
            TransactionCategory(
                id: record.id,
                name: record.name,
                icon: record.icon,
                colorHex: record.colorHex,
                isSystem: record.isSystem
            )
        }
        let accountRecords = CentwiseRustBackend.listAccounts()
        let loadedAccounts = accountRecords.map { account in
            let providerValue = self.provider(account.provider)
            return FinancialAccount(
                id: account.id,
                name: account.name,
                provider: providerValue,
                type: self.accountType(providerValue),
                lastFourDigits: account.lastFour,
                currentBalance: Double(account.balanceMinor) / 100,
                isArchived: account.archived
            )
        }
        let accountsById = Dictionary(uniqueKeysWithValues: accountRecords.map { ($0.id, $0) })
        let categoriesById = Dictionary(uniqueKeysWithValues: loadedCategories.map { ($0.id, $0) })
        let loadedTransactions = CentwiseRustBackend.listTransactions().map { transaction in
            let account = accountsById[transaction.accountId]
            let cat = categoriesById[transaction.categoryId] ?? TransactionCategory(
                id: transaction.categoryId,
                name: "Other",
                icon: "square.grid.2x2",
                colorHex: "#6B7280",
                isSystem: true
            )
            return CentwiseTransaction(
                id: transaction.id,
                title: transaction.title,
                amount: Double(transaction.amountMinor) / 100,
                currency: transaction.currency,
                type: self.transactionType(transaction.kind),
                category: cat,
                date: Date(timeIntervalSince1970: TimeInterval(transaction.occurredAtEpochMs) / 1000),
                accountId: transaction.accountId,
                accountName: account?.name ?? "Unknown account",
                provider: self.provider(account?.provider),
                rawSmsBody: transaction.rawSms,
                transactionReference: transaction.reference,
                balanceAfter: transaction.balanceAfterMinor.map { Double($0) / 100 },
                notes: transaction.notes,
                isAutoTracked: transaction.isAutoTracked
            )
        }
        let loadedBudgets = CentwiseRustBackend.listBudgets().map { budget in
            let cat = categoriesById[budget.categoryId]
            return CategoryBudget(
                id: budget.id,
                categoryId: budget.categoryId,
                categoryName: budget.categoryName,
                categoryIcon: cat?.icon ?? "square.grid.2x2",
                categoryColorHex: cat?.colorHex ?? "#6B7280",
                budgetLimit: Double(budget.limitMinor) / 100,
                currentSpent: Double(budget.spentMinor) / 100
            )
        }
        let loadedSubscriptions = CentwiseRustBackend.listSubscriptions().map { subscription in
            RecurringSubscription(
                id: subscription.id,
                name: subscription.name,
                amount: Double(subscription.amountMinor) / 100,
                billingCycle: subscription.billingCycle,
                nextDueDate: Date(timeIntervalSince1970: TimeInterval(subscription.nextDueEpochMs) / 1000),
                isActive: subscription.isActive
            )
        }

        let calendar = Calendar.current
        let now = Date()
        let components = calendar.dateComponents([.year, .month], from: now)
        let startOfMonth = calendar.date(from: components) ?? now
        let nextMonth = calendar.date(byAdding: .month, value: 1, to: startOfMonth) ?? now
        let loadedDashboard = CentwiseRustBackend.homeDashboard(start: startOfMonth, end: nextMonth)

        DispatchQueue.main.async {
            self.categories = loadedCategories
            self.accounts = loadedAccounts
            self.transactions = loadedTransactions
            self.budgets = loadedBudgets
            self.subscriptions = loadedSubscriptions
            self.homeDashboard = loadedDashboard
        }
        finishLoad()
    }

    /// Runs on `loadQueue`; coalesces notifications received during a refresh.
    private func finishLoad() {
        isLoading = false
        guard refreshPending else { return }
        refreshPending = false
        loadFromRust()
    }

    public func category(id: String) -> TransactionCategory {
        categories.first { $0.id == id } ?? TransactionCategory(
            id: id, name: id, icon: "tag", colorHex: "#64748B"
        )
    }

    @discardableResult
    public func addTransaction(_ transaction: CentwiseTransaction) -> Bool {
        guard CentwiseRustBackend.insertTransaction(transaction) else { return false }
        CentwiseNotifications.notifyNewTransaction(transaction)
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        return true
    }

    public func addTransactionAsync(_ transaction: CentwiseTransaction, completion: ((Bool) -> Void)? = nil) {
        loadQueue.async {
            let ok = CentwiseRustBackend.insertTransaction(transaction)
            if ok {
                CentwiseNotifications.notifyNewTransaction(transaction)
                NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
            }
            if let completion {
                DispatchQueue.main.async {
                    completion(ok)
                }
            }
        }
    }

    @discardableResult
    public func updateTransaction(_ transaction: CentwiseTransaction) -> Bool {
        guard CentwiseRustBackend.updateTransaction(transaction) else { return false }
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        return true
    }

    public func updateTransactionAsync(_ transaction: CentwiseTransaction, completion: ((Bool) -> Void)? = nil) {
        loadQueue.async {
            let ok = CentwiseRustBackend.updateTransaction(transaction)
            if ok {
                NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
            }
            if let completion {
                DispatchQueue.main.async {
                    completion(ok)
                }
            }
        }
    }

    public func deleteTransaction(id: String) {
        loadQueue.async {
            guard CentwiseRustBackend.deleteTransaction(id: id) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func addAccount(_ account: FinancialAccount) {
        loadQueue.async {
            guard CentwiseRustBackend.insertAccount(account) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func updateAccount(_ account: FinancialAccount) {
        loadQueue.async {
            guard CentwiseRustBackend.updateAccount(account) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func deleteAccount(id: String) {
        loadQueue.async {
            guard CentwiseRustBackend.deleteAccount(id: id) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func addBudget(_ budget: CategoryBudget) {
        loadQueue.async {
            guard CentwiseRustBackend.insertBudget(budget) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func updateBudget(_ budget: CategoryBudget) {
        loadQueue.async {
            guard CentwiseRustBackend.updateBudget(budget) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func deleteBudget(id: String) {
        loadQueue.async {
            guard CentwiseRustBackend.deleteBudget(id: id) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func addSubscription(_ subscription: RecurringSubscription) {
        loadQueue.async {
            guard CentwiseRustBackend.insertSubscription(subscription) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func updateSubscription(_ subscription: RecurringSubscription) {
        loadQueue.async {
            guard CentwiseRustBackend.updateSubscription(subscription) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func deleteSubscription(id: String) {
        loadQueue.async {
            guard CentwiseRustBackend.deleteSubscription(id: id) else { return }
            NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        }
    }

    public func addCategory(_ category: TransactionCategory) {
        loadQueue.async { [weak self] in
            guard CentwiseRustBackend.insertCategory(category) else { return }
            self?.loadFromRust()
        }
    }

    public func updateCategory(_ category: TransactionCategory) {
        loadQueue.async { [weak self] in
            guard !category.isSystem, CentwiseRustBackend.updateCategory(category) else { return }
            self?.loadFromRust()
        }
    }

    public func deleteCategory(id: String) {
        loadQueue.async { [weak self] in
            guard let self else { return }
            guard let category = self.categories.first(where: { $0.id == id }), !category.isSystem else { return }
            guard CentwiseRustBackend.deleteCategory(id: id) else { return }
            self.loadFromRust()
        }
    }

    @discardableResult
    public func loadSampleDemoData() -> DemoDataSummaryRecord? {
        guard let summary = CentwiseRustBackend.loadDemoData() else { return nil }
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
        return summary
    }

    public func loadSampleDemoDataAsync(completion: @escaping (DemoDataSummaryRecord?) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            let summary = self?.loadSampleDemoData()
            DispatchQueue.main.async {
                completion(summary)
            }
        }
    }

    public func resetToEmptyDatabase() {
        guard CentwiseRustBackend.resetToEmptyDatabase() else { return }
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func resetToEmptyDatabaseAsync(completion: @escaping (Bool) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async {
            let succeeded = CentwiseRustBackend.resetToEmptyDatabase()
            if succeeded {
                NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
            }
            DispatchQueue.main.async {
                completion(succeeded)
            }
        }
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

    private func transactionType(_ kind: TransactionKind) -> TransactionType {
        switch kind {
        case .expense: return .expense
        case .income: return .income
        case .transfer: return .transfer
        case .refund: return .refund
        }
    }

    private func provider(_ value: String?) -> FinancialProvider {
        switch value {
        case "bkash": return .bkash
        case "nagad": return .nagad
        case "rocket": return .rocket
        case "upay": return .upay
        case "cellfin": return .cellfin
        case "cash": return .cash
        case "dbbl": return .dutchBangla
        case "city-bank": return .cityBank
        case "brac-bank": return .bracBank
        case "ebl": return .easternBank
        case "standard-chartered": return .standardChartered
        default: return .other
        }
    }

    private func accountType(_ provider: FinancialProvider) -> AccountType {
        switch provider {
        case .bkash, .nagad, .rocket, .upay, .cellfin: return .mfs
        case .cityBank, .bracBank, .easternBank, .dutchBangla, .standardChartered: return .bank
        case .cash: return .cash
        case .other: return .bank
        }
    }
}
