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
        let result = SmsTransactionProcessor.shared.processIncomingSms(body: smsBody, senderHint: senderHint)
        switch result?.status {
        case .inserted:
            return .result(dialog: "Centwise tracked the transaction.")
        case .queuedForReview:
            return .result(dialog: "Centwise added the message to your review queue.")
        case .duplicate:
            return .result(dialog: "Centwise had already tracked this transaction.")
        default:
            return .result(dialog: "Centwise filtered this message.")
        }
    }
}
