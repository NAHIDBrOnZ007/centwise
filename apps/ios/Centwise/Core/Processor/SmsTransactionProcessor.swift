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
        DispatchQueue.main.async {
            TransactionRepository.shared.loadFromRust()
            ReviewQueueRepository.shared.refresh()
        }
        return result
    }
}
