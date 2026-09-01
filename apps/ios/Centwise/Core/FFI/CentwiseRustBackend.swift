import Foundation

/// Owns the process-wide Rust core handle. Native code supplies only the
/// storage location and platform metadata; parsing, rules, and persistence are Rust-owned.
enum CentwiseRustBackend {
    private static let appGroupIdentifier = "group.com.centwise.shared"
    private static var core: CentwiseCore?

    static func databaseURL() -> URL {
        FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier)?
            .appendingPathComponent("centwise.db")
            ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("centwise.db")
    }

    static func initialize() {
        guard core == nil else { return }

        let databaseURL = databaseURL()
        try? FileManager.default.createDirectory(
            at: databaseURL.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        do {
            core = try CentwiseCore.open(path: databaseURL.path)
        } catch {
            core = nil
        }
    }

    static func isAvailable() -> Bool {
        initialize()
        return core != nil
    }

    static func ingestSMS(body: String, senderHint: String?, date: Date) -> SmsIngestResult? {
        initialize()
        guard let core else { return nil }
        return try? core.ingestSms(
            body: body,
            senderHint: senderHint,
            occurredAtEpochMs: Int64(date.timeIntervalSince1970 * 1000)
        )
    }

    static func ingestSMSBatch(messages: [SmsBatchMessage]) -> [SmsIngestResult] {
        initialize()
        return (try? core?.ingestSmsBatch(messages: messages)) ?? []
    }

    @discardableResult
    static func loadDemoData() -> DemoDataSummaryRecord? {
        initialize()
        return try? core?.loadDemoData()
    }

    static func resetToEmptyDatabase() -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.resetToEmptyDatabase()) != nil
    }

    static func listTransactions() -> [TransactionRecord] {
        initialize()
        return (try? core?.listTransactions(limit: 10_000)) ?? []
    }

    static func getTransaction(id: String) -> TransactionRecord? {
        initialize()
        return try? core?.getTransaction(id: id)
    }

    static func accountBalance(accountId: String) -> Int64? {
        initialize()
        return try? core?.accountBalance(accountId: accountId)
    }

    static func homeDashboard(start: Date, end: Date, recentLimit: UInt32 = 10) -> HomeDashboardRecord? {
        initialize()
        return try? core?.homeDashboard(
            startEpochMs: Int64(start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(end.timeIntervalSince1970 * 1000),
            recentLimit: recentLimit
        )
    }

    static func analyticsSnapshot(
        start: Date,
        end: Date,
        monthsBack: UInt32,
        typeFilter: String
    ) -> AnalyticsSnapshotRecord? {
        initialize()
        return try? core?.analyticsSnapshot(
            startEpochMs: Int64(start.timeIntervalSince1970 * 1000),
            endEpochMs: Int64(end.timeIntervalSince1970 * 1000),
            monthsBack: monthsBack,
            typeFilter: typeFilter
        )
    }

    static func listAccounts() -> [AccountRecord] {
        initialize()
        return (try? core?.listAccounts()) ?? []
    }

    static func listBudgets() -> [BudgetRecord] {
        initialize()
        return (try? core?.listBudgets()) ?? []
    }

    static func listSubscriptions() -> [SubscriptionRecord] {
        initialize()
        return (try? core?.listSubscriptions()) ?? []
    }

    static func listCategories() -> [CategoryRecord] {
        initialize()
        return (try? core?.listCategories()) ?? []
    }

    static func listRules() -> [SmartRuleRecord] {
        initialize()
        return (try? core?.listRules()) ?? []
    }

    static func insertCategory(_ category: TransactionCategory) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.insertCategory(input: CategoryInput(
            id: category.id, name: category.name, icon: category.icon, colorHex: category.colorHex
        ))) != nil
    }

    static func updateCategory(_ category: TransactionCategory) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.updateCategory(input: CategoryInput(
            id: category.id, name: category.name, icon: category.icon, colorHex: category.colorHex
        ))) ?? false
    }

    static func deleteCategory(id: String) -> Bool {
        initialize()
        return (try? core?.deleteCategory(id: id)) ?? false
    }

    static func insertRule(_ rule: SmartRule) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.insertRule(input: ruleInput(rule))) != nil
    }

    static func updateRule(_ rule: SmartRule) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.updateRule(input: ruleInput(rule))) ?? false
    }

    static func deleteRule(id: String) -> Bool {
        initialize()
        return (try? core?.deleteRule(id: id)) ?? false
    }

    static func insertTransaction(_ transaction: CentwiseTransaction) -> Bool {
        initialize()
        guard let core else { return false }
        do {
            try core.insertTransaction(input: transactionInput(transaction))
            return true
        } catch {
            print("❌ CentwiseRustBackend.insertTransaction failed: \(error)")
            return false
        }
    }

    static func updateTransaction(_ transaction: CentwiseTransaction) -> Bool {
        initialize()
        guard let core else { return false }
        do {
            return try core.updateTransaction(input: transactionInput(transaction))
        } catch {
            print("❌ CentwiseRustBackend.updateTransaction failed: \(error)")
            return false
        }
    }

    static func deleteTransaction(id: String) -> Bool {
        initialize()
        return (try? core?.deleteTransaction(id: id)) ?? false
    }

    static func insertAccount(_ account: FinancialAccount) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.insertAccount(account: accountInput(account))) != nil
    }

    static func updateAccount(_ account: FinancialAccount) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.updateAccount(account: accountInput(account))) ?? false
    }

    static func deleteAccount(id: String) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.deleteAccount(id: id)) ?? false
    }

    static func insertBudget(_ budget: CategoryBudget) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.insertBudget(input: BudgetInput(
            id: budget.id,
            categoryId: budget.categoryId,
            limitMinor: Int64(budget.budgetLimit * 100),
            period: "monthly",
            startEpochMs: 0,
            endEpochMs: 4_102_444_800_000
        ))) != nil
    }

    static func updateBudget(_ budget: CategoryBudget) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.updateBudget(input: BudgetInput(
            id: budget.id,
            categoryId: budget.categoryId,
            limitMinor: Int64(budget.budgetLimit * 100),
            period: "monthly",
            startEpochMs: 0,
            endEpochMs: 4_102_444_800_000
        ))) ?? false
    }

    static func deleteBudget(id: String) -> Bool {
        initialize()
        return (try? core?.deleteBudget(id: id)) ?? false
    }

    static func insertSubscription(_ subscription: RecurringSubscription) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.insertSubscription(input: SubscriptionInput(
            id: subscription.id,
            name: subscription.name,
            amountMinor: Int64(subscription.amount * 100),
            billingCycle: subscription.billingCycle.lowercased(),
            nextDueEpochMs: Int64(subscription.nextDueDate.timeIntervalSince1970 * 1000),
            isActive: subscription.isActive
        ))) != nil
    }

    static func updateSubscription(_ subscription: RecurringSubscription) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.updateSubscription(input: SubscriptionInput(
            id: subscription.id,
            name: subscription.name,
            amountMinor: Int64(subscription.amount * 100),
            billingCycle: subscription.billingCycle.lowercased(),
            nextDueEpochMs: Int64(subscription.nextDueDate.timeIntervalSince1970 * 1000),
            isActive: subscription.isActive
        ))) ?? false
    }

    static func deleteSubscription(id: String) -> Bool {
        initialize()
        return (try? core?.deleteSubscription(id: id)) ?? false
    }

    static func listReviewQueue() -> [ReviewQueueRecord] {
        initialize()
        return (try? core?.listReviewQueue(limit: 100)) ?? []
    }

    static func dismissReviewQueueItem(id: String) -> Bool {
        initialize()
        return (try? core?.dismissReviewQueueItem(id: id)) ?? false
    }

    static func convertReviewQueueItem(id: String, transaction: CentwiseTransaction) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.convertReviewQueueItem(id: id, input: transactionInput(transaction))) ?? false
    }

    private static func accountInput(_ account: FinancialAccount) -> AccountInput {
        AccountInput(
            id: account.id,
            name: account.name,
            provider: canonicalProvider(account.provider),
            lastFour: account.lastFourDigits.map { String($0.suffix(4)) },
            startingBalanceMinor: Int64(account.currentBalance * 100),
            archived: account.isArchived
        )
    }

    private static func transactionInput(_ transaction: CentwiseTransaction) -> TransactionInput {
        let account = TransactionRepository.shared.accounts.first { $0.id == transaction.accountId }
        return TransactionInput(
            id: transaction.id,
            title: transaction.title,
            amountMinor: Int64(transaction.amount * 100),
            currency: transaction.currency,
            kind: transaction.type.rustKind,
            categoryId: transaction.category.id,
            occurredAtEpochMs: Int64(transaction.date.timeIntervalSince1970 * 1000),
            accountId: transaction.accountId,
            accountProvider: canonicalProvider(transaction.provider),
            accountName: transaction.accountName,
            accountLastFour: account.flatMap(\.lastFourDigits).map { String($0.suffix(4)) },
            reference: transaction.transactionReference,
            balanceAfterMinor: transaction.balanceAfter.map { Int64($0 * 100) },
            feeMinor: nil,
            notes: transaction.notes,
            rawSms: transaction.rawSmsBody,
            isAutoTracked: transaction.isAutoTracked
        )
    }

    private static func ruleInput(_ rule: SmartRule) -> SmartRuleInput {
        SmartRuleInput(
            id: rule.id,
            name: rule.name,
            keyword: rule.keyword,
            matchType: rule.matchType.rustValue,
            categoryId: rule.category.id,
            kind: rule.transactionType.rustKind,
            isEnabled: rule.isEnabled
        )
    }

    private static func canonicalProvider(_ provider: FinancialProvider) -> String {
        switch provider {
        case .bkash: return "bkash"
        case .nagad: return "nagad"
        case .rocket: return "rocket"
        case .upay: return "upay"
        case .cellfin: return "cellfin"
        case .cash: return "cash"
        case .dutchBangla: return "dbbl"
        case .cityBank: return "city-bank"
        case .bracBank: return "brac-bank"
        case .easternBank: return "ebl"
        case .standardChartered: return "standard-chartered"
        case .other: return "banks-generic"
        }
    }
}

private extension TransactionType {
    var rustKind: TransactionKind {
        switch self {
        case .expense: return .expense
        case .income: return .income
        case .transfer: return .transfer
        case .refund: return .refund
        }
    }
}

private extension RuleMatchType {
    var rustValue: String {
        switch self {
        case .contains: return "contains"
        case .startsWith: return "starts_with"
        case .equals: return "exactly_matches"
        }
    }
}
