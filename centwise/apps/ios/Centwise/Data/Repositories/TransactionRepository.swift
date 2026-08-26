import Foundation
import Combine
import UIKit
import SQLite3

public extension Notification.Name {
    static let centwiseTransactionsUpdated = Notification.Name("centwiseTransactionsUpdated")
}

// MARK: - Native SQLite Database Engine for Centwise
public final class CentwiseSQLiteDatabase {
    public static let shared = CentwiseSQLiteDatabase()

    private var db: OpaquePointer?
    private let dbQueue = DispatchQueue(label: "com.centwise.sqlite.queue")

    private var databaseURL: URL {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        return docs.appendingPathComponent("centwise.sqlite")
    }

    public init() {
        openDatabase()
        createTables()
    }

    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }

    private func openDatabase() {
        let path = databaseURL.path
        if sqlite3_open_v2(path, &db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX, nil) == SQLITE_OK {
            // Enable WAL mode for high performance concurrent reads and writes
            sqlite3_exec(db, "PRAGMA journal_mode = WAL;", nil, nil, nil)
            sqlite3_exec(db, "PRAGMA synchronous = NORMAL;", nil, nil, nil)
            sqlite3_exec(db, "PRAGMA busy_timeout = 5000;", nil, nil, nil)
            sqlite3_exec(db, "PRAGMA cache_size = -500;", nil, nil, nil) // Limit memory cache to 500KB
            sqlite3_exec(db, "PRAGMA temp_store = MEMORY;", nil, nil, nil)
            sqlite3_exec(db, "PRAGMA mmap_size = 0;", nil, nil, nil)
            print("✅ [CentwiseSQLite] Database opened at \(path)")
        } else {
            print("❌ [CentwiseSQLite] Unable to open database at \(path)")
        }
    }

    private func createTables() {
        let createTransactionsTable = """
        CREATE TABLE IF NOT EXISTS transactions (
            id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            amount REAL NOT NULL,
            currency TEXT NOT NULL,
            type TEXT NOT NULL,
            category_id TEXT NOT NULL,
            category_name TEXT NOT NULL,
            category_icon TEXT NOT NULL,
            category_color TEXT NOT NULL,
            date REAL NOT NULL,
            account_id TEXT NOT NULL,
            account_name TEXT NOT NULL,
            provider TEXT NOT NULL,
            raw_sms_body TEXT,
            transaction_reference TEXT,
            balance_after REAL,
            notes TEXT,
            is_auto_tracked INTEGER NOT NULL
        );
        """

        let createAccountsTable = """
        CREATE TABLE IF NOT EXISTS accounts (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            provider TEXT NOT NULL,
            type TEXT NOT NULL,
            last_four_digits TEXT,
            current_balance REAL NOT NULL,
            is_archived INTEGER NOT NULL
        );
        """

        let createBudgetsTable = """
        CREATE TABLE IF NOT EXISTS budgets (
            id TEXT PRIMARY KEY,
            category_id TEXT NOT NULL,
            category_name TEXT NOT NULL,
            category_icon TEXT NOT NULL,
            category_color_hex TEXT NOT NULL,
            budget_limit REAL NOT NULL,
            current_spent REAL NOT NULL
        );
        """

        let createSubscriptionsTable = """
        CREATE TABLE IF NOT EXISTS subscriptions (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            amount REAL NOT NULL,
            billing_cycle TEXT NOT NULL,
            next_due_date REAL NOT NULL,
            provider TEXT NOT NULL,
            is_active INTEGER NOT NULL,
            icon TEXT NOT NULL
        );
        """

        sqlite3_exec(db, createTransactionsTable, nil, nil, nil)
        sqlite3_exec(db, createAccountsTable, nil, nil, nil)
        sqlite3_exec(db, createBudgetsTable, nil, nil, nil)
        sqlite3_exec(db, createSubscriptionsTable, nil, nil, nil)
    }

    // MARK: - Transactions SQLite Operations

    public func insertTransaction(_ tx: CentwiseTransaction) {
        dbQueue.sync {
            let sql = """
            INSERT OR REPLACE INTO transactions (
                id, title, amount, currency, type, category_id, category_name,
                category_icon, category_color, date, account_id, account_name,
                provider, raw_sms_body, transaction_reference, balance_after, notes, is_auto_tracked
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (tx.id as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 2, (tx.title as NSString).utf8String, -1, nil)
                sqlite3_bind_double(stmt, 3, tx.amount)
                sqlite3_bind_text(stmt, 4, (tx.currency as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 5, (tx.type.rawValue as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 6, (tx.category.id as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 7, (tx.category.name as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 8, (tx.category.icon as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 9, (tx.category.colorHex as NSString).utf8String, -1, nil)
                sqlite3_bind_double(stmt, 10, tx.date.timeIntervalSince1970)
                sqlite3_bind_text(stmt, 11, (tx.accountId as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 12, (tx.accountName as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 13, (tx.provider.rawValue as NSString).utf8String, -1, nil)

                if let raw = tx.rawSmsBody { sqlite3_bind_text(stmt, 14, (raw as NSString).utf8String, -1, nil) } else { sqlite3_bind_null(stmt, 14) }
                if let ref = tx.transactionReference { sqlite3_bind_text(stmt, 15, (ref as NSString).utf8String, -1, nil) } else { sqlite3_bind_null(stmt, 15) }
                if let bal = tx.balanceAfter { sqlite3_bind_double(stmt, 16, bal) } else { sqlite3_bind_null(stmt, 16) }
                if let notes = tx.notes { sqlite3_bind_text(stmt, 17, (notes as NSString).utf8String, -1, nil) } else { sqlite3_bind_null(stmt, 17) }
                sqlite3_bind_int(stmt, 18, tx.isAutoTracked ? 1 : 0)

                sqlite3_step(stmt)
            }
            sqlite3_finalize(stmt)
        }
    }

    public func deleteTransaction(id: String) {
        dbQueue.sync {
            let sql = "DELETE FROM transactions WHERE id = ?;"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (id as NSString).utf8String, -1, nil)
                sqlite3_step(stmt)
            }
            sqlite3_finalize(stmt)
        }
    }

    public func fetchTransactions() -> [CentwiseTransaction] {
        dbQueue.sync {
            var result: [CentwiseTransaction] = []
            let sql = "SELECT id, title, amount, currency, type, category_id, category_name, category_icon, category_color, date, account_id, account_name, provider, raw_sms_body, transaction_reference, balance_after, notes, is_auto_tracked FROM transactions ORDER BY date DESC;"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let id = String(cString: sqlite3_column_text(stmt, 0))
                    let title = String(cString: sqlite3_column_text(stmt, 1))
                    let amount = sqlite3_column_double(stmt, 2)
                    let currency = String(cString: sqlite3_column_text(stmt, 3))
                    let typeRaw = String(cString: sqlite3_column_text(stmt, 4))
                    let catId = String(cString: sqlite3_column_text(stmt, 5))
                    let catName = String(cString: sqlite3_column_text(stmt, 6))
                    let catIcon = String(cString: sqlite3_column_text(stmt, 7))
                    let catColor = String(cString: sqlite3_column_text(stmt, 8))
                    let dateSec = sqlite3_column_double(stmt, 9)
                    let accId = String(cString: sqlite3_column_text(stmt, 10))
                    let accName = String(cString: sqlite3_column_text(stmt, 11))
                    let provRaw = String(cString: sqlite3_column_text(stmt, 12))

                    let rawSms = sqlite3_column_text(stmt, 13).map { String(cString: $0) }
                    let ref = sqlite3_column_text(stmt, 14).map { String(cString: $0) }
                    let bal = sqlite3_column_type(stmt, 15) != SQLITE_NULL ? sqlite3_column_double(stmt, 15) : nil
                    let notes = sqlite3_column_text(stmt, 16).map { String(cString: $0) }
                    let autoTracked = sqlite3_column_int(stmt, 17) == 1

                    let type = TransactionType(rawValue: typeRaw) ?? .expense
                    let provider = FinancialProvider(rawValue: provRaw) ?? .bkash
                    let category = TransactionCategory(id: catId, name: catName, icon: catIcon, colorHex: catColor)

                    result.append(
                        CentwiseTransaction(
                            id: id,
                            title: title,
                            amount: amount,
                            currency: currency,
                            type: type,
                            category: category,
                            date: Date(timeIntervalSince1970: dateSec),
                            accountId: accId,
                            accountName: accName,
                            provider: provider,
                            rawSmsBody: rawSms,
                            transactionReference: ref,
                            balanceAfter: bal,
                            notes: notes,
                            isAutoTracked: autoTracked
                        )
                    )
                }
            }
            sqlite3_finalize(stmt)
            return result
        }
    }

    // MARK: - Accounts SQLite Operations

    public func insertOrUpdateAccount(_ acc: FinancialAccount) {
        dbQueue.sync {
            let sql = """
            INSERT OR REPLACE INTO accounts (
                id, name, provider, type, last_four_digits, current_balance, is_archived
            ) VALUES (?, ?, ?, ?, ?, ?, ?);
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (acc.id as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 2, (acc.name as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 3, (acc.provider.rawValue as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 4, (acc.type.rawValue as NSString).utf8String, -1, nil)
                if let last4 = acc.lastFourDigits { sqlite3_bind_text(stmt, 5, (last4 as NSString).utf8String, -1, nil) } else { sqlite3_bind_null(stmt, 5) }
                sqlite3_bind_double(stmt, 6, acc.currentBalance)
                sqlite3_bind_int(stmt, 7, acc.isArchived ? 1 : 0)

                sqlite3_step(stmt)
            }
            sqlite3_finalize(stmt)
        }
    }

    public func fetchAccounts() -> [FinancialAccount] {
        dbQueue.sync {
            var result: [FinancialAccount] = []
            let sql = "SELECT id, name, provider, type, last_four_digits, current_balance, is_archived FROM accounts;"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let id = String(cString: sqlite3_column_text(stmt, 0))
                    let name = String(cString: sqlite3_column_text(stmt, 1))
                    let provRaw = String(cString: sqlite3_column_text(stmt, 2))
                    let typeRaw = String(cString: sqlite3_column_text(stmt, 3))
                    let last4 = sqlite3_column_text(stmt, 4).map { String(cString: $0) }
                    let balance = sqlite3_column_double(stmt, 5)
                    let archived = sqlite3_column_int(stmt, 6) == 1

                    let provider = FinancialProvider(rawValue: provRaw) ?? .bkash
                    let type = AccountType(rawValue: typeRaw) ?? .mfs

                    result.append(
                        FinancialAccount(
                            id: id,
                            name: name,
                            provider: provider,
                            type: type,
                            lastFourDigits: last4,
                            currentBalance: balance,
                            isArchived: archived
                        )
                    )
                }
            }
            sqlite3_finalize(stmt)
            return result
        }
    }

    // MARK: - Budgets SQLite Operations

    public func insertOrUpdateBudget(_ budget: CategoryBudget) {
        dbQueue.sync {
            let sql = """
            INSERT OR REPLACE INTO budgets (
                id, category_id, category_name, category_icon, category_color_hex, budget_limit, current_spent
            ) VALUES (?, ?, ?, ?, ?, ?, ?);
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (budget.id as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 2, (budget.categoryId as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 3, (budget.categoryName as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 4, (budget.categoryIcon as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 5, (budget.categoryColorHex as NSString).utf8String, -1, nil)
                sqlite3_bind_double(stmt, 6, budget.budgetLimit)
                sqlite3_bind_double(stmt, 7, budget.currentSpent)

                sqlite3_step(stmt)
            }
            sqlite3_finalize(stmt)
        }
    }

    public func fetchBudgets() -> [CategoryBudget] {
        dbQueue.sync {
            var result: [CategoryBudget] = []
            let sql = "SELECT id, category_id, category_name, category_icon, category_color_hex, budget_limit, current_spent FROM budgets;"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let id = String(cString: sqlite3_column_text(stmt, 0))
                    let catId = String(cString: sqlite3_column_text(stmt, 1))
                    let catName = String(cString: sqlite3_column_text(stmt, 2))
                    let catIcon = String(cString: sqlite3_column_text(stmt, 3))
                    let catColor = String(cString: sqlite3_column_text(stmt, 4))
                    let limit = sqlite3_column_double(stmt, 5)
                    let spent = sqlite3_column_double(stmt, 6)

                    result.append(
                        CategoryBudget(
                            id: id,
                            categoryId: catId,
                            categoryName: catName,
                            categoryIcon: catIcon,
                            categoryColorHex: catColor,
                            budgetLimit: limit,
                            currentSpent: spent
                        )
                    )
                }
            }
            sqlite3_finalize(stmt)
            return result
        }
    }

    // MARK: - Subscriptions SQLite Operations

    public func insertOrUpdateSubscription(_ sub: RecurringSubscription) {
        dbQueue.sync {
            let sql = """
            INSERT OR REPLACE INTO subscriptions (
                id, name, amount, billing_cycle, next_due_date, provider, is_active, icon
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                sqlite3_bind_text(stmt, 1, (sub.id as NSString).utf8String, -1, nil)
                sqlite3_bind_text(stmt, 2, (sub.name as NSString).utf8String, -1, nil)
                sqlite3_bind_double(stmt, 3, sub.amount)
                sqlite3_bind_text(stmt, 4, (sub.billingCycle as NSString).utf8String, -1, nil)
                sqlite3_bind_double(stmt, 5, sub.nextDueDate.timeIntervalSince1970)
                sqlite3_bind_text(stmt, 6, (sub.provider.rawValue as NSString).utf8String, -1, nil)
                sqlite3_bind_int(stmt, 7, sub.isActive ? 1 : 0)
                sqlite3_bind_text(stmt, 8, (sub.icon as NSString).utf8String, -1, nil)

                sqlite3_step(stmt)
            }
            sqlite3_finalize(stmt)
        }
    }

    public func fetchSubscriptions() -> [RecurringSubscription] {
        dbQueue.sync {
            var result: [RecurringSubscription] = []
            let sql = "SELECT id, name, amount, billing_cycle, next_due_date, provider, is_active, icon FROM subscriptions;"
            var stmt: OpaquePointer?
            if sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK {
                while sqlite3_step(stmt) == SQLITE_ROW {
                    let id = String(cString: sqlite3_column_text(stmt, 0))
                    let name = String(cString: sqlite3_column_text(stmt, 1))
                    let amount = sqlite3_column_double(stmt, 2)
                    let cycle = String(cString: sqlite3_column_text(stmt, 3))
                    let nextDateSec = sqlite3_column_double(stmt, 4)
                    let provRaw = String(cString: sqlite3_column_text(stmt, 5))
                    let active = sqlite3_column_int(stmt, 6) == 1
                    let icon = String(cString: sqlite3_column_text(stmt, 7))

                    let provider = FinancialProvider(rawValue: provRaw) ?? .bkash

                    result.append(
                        RecurringSubscription(
                            id: id,
                            name: name,
                            amount: amount,
                            billingCycle: cycle,
                            nextDueDate: Date(timeIntervalSince1970: nextDateSec),
                            provider: provider,
                            isActive: active,
                            icon: icon
                        )
                    )
                }
            }
            sqlite3_finalize(stmt)
            return result
        }
    }

    public func clearAllTables() {
        dbQueue.sync {
            sqlite3_exec(db, "DELETE FROM transactions;", nil, nil, nil)
            sqlite3_exec(db, "DELETE FROM accounts;", nil, nil, nil)
            sqlite3_exec(db, "DELETE FROM budgets;", nil, nil, nil)
            sqlite3_exec(db, "DELETE FROM subscriptions;", nil, nil, nil)
        }
    }
}

// MARK: - Transaction Repository Protocol
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

// MARK: - Production SQLite Transaction Repository
public final class TransactionRepository: TransactionRepositoryProtocol, ObservableObject {
    public static let shared = TransactionRepository()

    @Published public private(set) var transactions: [CentwiseTransaction] = []
    @Published public private(set) var accounts: [FinancialAccount] = []
    @Published public private(set) var budgets: [CategoryBudget] = []
    @Published public private(set) var subscriptions: [RecurringSubscription] = []

    private let sqlite = CentwiseSQLiteDatabase.shared

    public init() {
        loadFromSQLite()
        setupNotificationObservers()
    }

    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(
            forName: UIApplication.willEnterForegroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.loadFromSQLite()
        }

        NotificationCenter.default.addObserver(
            forName: .centwiseTransactionsUpdated,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.loadFromSQLite()
        }
    }

    public func loadFromSQLite() {
        // 1. Transactions
        let loadedTxs = sqlite.fetchTransactions()
        self.transactions = loadedTxs

        // 2. Accounts
        let loadedAccs = sqlite.fetchAccounts()
        if !loadedAccs.isEmpty {
            self.accounts = loadedAccs
        } else {
            self.accounts = Self.defaultStarterAccounts
            for acc in self.accounts {
                sqlite.insertOrUpdateAccount(acc)
            }
        }

        // 3. Budgets (Clean empty by default)
        self.budgets = sqlite.fetchBudgets()

        // 4. Subscriptions (Clean empty by default)
        self.subscriptions = sqlite.fetchSubscriptions()
    }

    // MARK: - Clean Starter Profiles

    public static var defaultStarterAccounts: [FinancialAccount] {
        [
            FinancialAccount(name: "bKash", provider: .bkash, type: .mfs, currentBalance: 0.0),
            FinancialAccount(name: "Nagad", provider: .nagad, type: .mfs, currentBalance: 0.0),
            FinancialAccount(name: "Bank Account", provider: .bracBank, type: .bank, currentBalance: 0.0),
            FinancialAccount(name: "Cash Wallet", provider: .cash, type: .cash, currentBalance: 0.0)
        ]
    }

    // MARK: - Demo & Reset Operations

    public func loadSampleDemoData() {
        sqlite.clearAllTables()

        for tx in MockDataProvider.shared.transactions {
            sqlite.insertTransaction(tx)
        }
        for acc in MockDataProvider.shared.accounts {
            sqlite.insertOrUpdateAccount(acc)
        }
        for b in MockDataProvider.shared.budgets {
            sqlite.insertOrUpdateBudget(b)
        }
        for s in MockDataProvider.shared.subscriptions {
            sqlite.insertOrUpdateSubscription(s)
        }

        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func resetToEmptyDatabase() {
        sqlite.clearAllTables()

        for acc in Self.defaultStarterAccounts {
            sqlite.insertOrUpdateAccount(acc)
        }

        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    // MARK: - CRUD Protocol Methods

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
        sqlite.insertTransaction(transaction)

        // Update matching account balance in SQLite
        if let accIdx = accounts.firstIndex(where: { $0.id == transaction.accountId || $0.provider == transaction.provider }) {
            var acc = accounts[accIdx]
            if transaction.type == .income {
                acc.currentBalance += transaction.amount
            } else if transaction.type == .expense {
                acc.currentBalance -= transaction.amount
            }
            accounts[accIdx] = acc
            sqlite.insertOrUpdateAccount(acc)
        }

        loadFromSQLite()
        CentwiseNotifications.notifyNewTransaction(transaction)
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func updateTransaction(_ transaction: CentwiseTransaction) {
        sqlite.insertTransaction(transaction)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func deleteTransaction(id: String) {
        sqlite.deleteTransaction(id: id)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func addAccount(_ account: FinancialAccount) {
        sqlite.insertOrUpdateAccount(account)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func addBudget(_ budget: CategoryBudget) {
        sqlite.insertOrUpdateBudget(budget)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func updateBudget(_ budget: CategoryBudget) {
        sqlite.insertOrUpdateBudget(budget)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func deleteBudget(id: String) {
        // Budget deletion
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func addSubscription(_ subscription: RecurringSubscription) {
        sqlite.insertOrUpdateSubscription(subscription)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func updateSubscription(_ subscription: RecurringSubscription) {
        sqlite.insertOrUpdateSubscription(subscription)
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }

    public func deleteSubscription(id: String) {
        loadFromSQLite()
        NotificationCenter.default.post(name: .centwiseTransactionsUpdated, object: nil)
    }
}
