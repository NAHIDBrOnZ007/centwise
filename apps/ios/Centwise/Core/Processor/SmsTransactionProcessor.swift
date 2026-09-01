import Foundation

/** Thin iOS adapter: Shortcut/App Intent supplies the message; Rust parses and stores it. */
public final class SmsTransactionProcessor {
    public static let shared = SmsTransactionProcessor()

    public init() {}

    @discardableResult
    public func processIncomingSms(
        body: String,
        senderHint: String? = nil,
        date: Date = Date()
    ) -> SmsIngestResult? {
        let result = CentwiseRustBackend.ingestSMS(body: body, senderHint: senderHint, date: date)
        if let txId = result?.transactionId, let tx = CentwiseRustBackend.getTransaction(id: txId) {
            let isIncome = tx.kind == .income
            let catName = tx.categoryId.capitalized
            CentwiseNotifications.notifyIngestedTransaction(
                title: tx.title,
                amountMinor: tx.amountMinor,
                isIncome: isIncome,
                categoryName: catName,
                id: tx.id
            )
        }
        DispatchQueue.main.async {
            TransactionRepository.shared.loadFromRust()
            ReviewQueueRepository.shared.refresh()
        }
        return result
    }
}
