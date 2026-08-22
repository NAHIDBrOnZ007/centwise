import AppIntents
import Foundation

public struct ParseTransactionIntent: AppIntent {
    public static var title: LocalizedStringResource = "Track Transaction from SMS"
    public static var description = IntentDescription("Parses incoming bKash, Nagad, Rocket, or Bank SMS and adds the transaction to Centwise.")

    @Parameter(title: "SMS Body")
    public var smsBody: String

    @Parameter(title: "Sender")
    public var senderHint: String?

    public init() {}

    public init(smsBody: String, senderHint: String? = nil) {
        self.smsBody = smsBody
        self.senderHint = senderHint
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        if let transaction = SmsTransactionProcessor.shared.processIncomingSms(body: smsBody, senderHint: senderHint) {
            let sign = transaction.type == .income ? "+" : "-"
            let formatted = "\(transaction.title) (\(sign)৳\(String(format: "%.0f", transaction.amount)))"
            return .result(dialog: "Centwise tracked: \(formatted)")
        } else {
            return .result(dialog: "Centwise checked message and updated your review queue.")
        }
    }
}
