import Foundation

/// Owns the process-wide Rust core handle. Native code supplies only storage
/// location, account identity migration, and incoming message metadata.
enum CentwiseRustBackend {
    private static let appGroupIdentifier = "group.com.centwise.shared"
    private static var core: CentwiseCore?

    static func initialize() {
        guard core == nil else { return }

        let databaseURL = FileManager.default
            .containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier)?
            .appendingPathComponent("centwise.db")
            ?? FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
                .appendingPathComponent("centwise.db")
        try? FileManager.default.createDirectory(
            at: databaseURL.deletingLastPathComponent(),
            withIntermediateDirectories: true
        )
        do {
            core = try CentwiseCore.open(path: databaseURL.path)
            syncExistingAccounts()
        } catch {
            // Keep the app launchable while a native Rust binary is being packaged.
            core = nil
        }
    }

    static func ingestSMS(body: String, senderHint: String?, date: Date) -> SmsIngestResult? {
        initialize()
        guard let core else { return nil }
        do {
            return try core.ingestSms(
                body: body,
                senderHint: senderHint,
                occurredAtEpochMs: Int64(date.timeIntervalSince1970 * 1000)
            )
        } catch {
            return nil
        }
    }

    static func isAvailable() -> Bool {
        initialize()
        return core != nil
    }

    @discardableResult
    static func loadDemoData() -> DemoDataSummaryRecord? {
        initialize()
        return try? core?.loadDemoData()
    }

    static func resetToEmptyDatabase() -> Bool {
        initialize()
        guard let core else { return false }
        do {
            try core.resetToEmptyDatabase()
            return true
        } catch {
            return false
        }
    }

    static func listTransactions() -> [TransactionRecord] {
        initialize()
        return (try? core?.listTransactions(limit: 10_000)) ?? []
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

    static func listReviewQueue() -> [ReviewQueueRecord] {
        initialize()
        guard let core else { return [] }
        return (try? core.listReviewQueue(limit: 100)) ?? []
    }

    static func dismissReviewQueueItem(id: String) -> Bool {
        initialize()
        guard let core else { return false }
        return (try? core.dismissReviewQueueItem(id: id)) ?? false
    }

    static func convertReviewQueueItem(id: String, transaction: CentwiseTransaction) -> Bool {
        initialize()
        guard let core else { return false }
        let kind: TransactionKind = switch transaction.type {
        case .expense: .expense
        case .income: .income
        case .transfer: .transfer
        case .refund: .refund
        }
        let input = TransactionInput(
            id: transaction.id,
            title: transaction.title,
            amountMinor: Int64(transaction.amount * 100),
            currency: transaction.currency,
            kind: kind,
            categoryId: transaction.category.id,
            occurredAtEpochMs: Int64(transaction.date.timeIntervalSince1970 * 1000),
            accountId: transaction.accountId,
            reference: transaction.transactionReference,
            balanceAfterMinor: transaction.balanceAfter.map { Int64($0 * 100) },
            feeMinor: nil,
            notes: transaction.notes,
            rawSms: transaction.rawSmsBody,
            isAutoTracked: transaction.isAutoTracked
        )
        return (try? core.convertReviewQueueItem(id: id, input: input)) ?? false
    }

    private static func syncExistingAccounts() {
        TransactionRepository.shared.accounts.forEach { account in
            try? core?.insertAccount(
                account: AccountInput(
                    id: account.id,
                    name: account.name,
                    provider: canonicalProvider(account.provider),
                    lastFour: account.lastFourDigits,
                    startingBalanceMinor: Int64(account.currentBalance * 100)
                )
            )
        }
    }

    private static func canonicalProvider(_ provider: FinancialProvider) -> String {
        switch provider {
        case .bkash: return "bkash"
        case .nagad: return "nagad"
        case .rocket: return "rocket"
        case .dutchBangla: return "dbbl"
        case .cityBank: return "city-bank"
        case .bracBank: return "brac-bank"
        case .easternBank: return "ebl"
        default: return "banks-generic"
        }
    }
}
