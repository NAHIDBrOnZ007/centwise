import AppIntents
import Foundation

public struct ParseTransactionIntent: AppIntent {
    public static var title: LocalizedStringResource = "Log Transaction"
    public static var description = IntentDescription("Analyzes financial message text and records the transaction in Centwise.")

    @Parameter(title: "Message Text", description: "The transaction text or notification to log", requestValueDialog: "What is the transaction message?")
    public var smsBody: String?

    @Parameter(title: "Sender (Optional)", description: "The message sender or provider name")
    public var senderHint: String?

    public static var parameterSummary: some ParameterSummary {
        Summary("Log transaction from \(\.$smsBody)") {
            \.$senderHint
        }
    }

    public init() {}

    public init(smsBody: String, senderHint: String? = nil) {
        self.smsBody = smsBody
        self.senderHint = senderHint
    }

    public func perform() async throws -> some IntentResult & ProvidesDialog {
        let bodyToProcess: String
        if let input = smsBody, !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            bodyToProcess = input
        } else {
            bodyToProcess = try await $smsBody.requestValue("What transaction message would you like to log?")
        }

        let result = SmsTransactionProcessor.shared.processIncomingSms(body: bodyToProcess, senderHint: senderHint)
        switch result?.status {
        case .inserted:
            return .result(dialog: "Transaction logged successfully.")
        case .queuedForReview:
            return .result(dialog: "Message added to review queue.")
        case .duplicate:
            return .result(dialog: "Transaction was already logged.")
        default:
            return .result(dialog: "Message processed.")
        }
    }
}
