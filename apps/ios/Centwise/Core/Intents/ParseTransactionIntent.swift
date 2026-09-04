import AppIntents
import Foundation
import os.log

private let logger = Logger(subsystem: "com.centwise", category: "ParseTransactionIntent")

public struct ParseTransactionIntent: AppIntent {
    public static var title: LocalizedStringResource = "Track Transaction from SMS"
    public static var description = IntentDescription("Parses incoming bKash, Nagad, Rocket, or Bank SMS and adds the transaction to Centwise.")

    @Parameter(
        title: "SMS Body",
        description: "The transaction text or SMS message to track",
        inputConnectionBehavior: .connectToPreviousIntentResult
    )
    public var smsBody: String?

    @Parameter(
        title: "Sender",
        description: "The message sender or provider name"
    )
    public var senderHint: String?



    public init() {}

    public init(smsBody: String? = nil, senderHint: String? = nil) {
        self.smsBody = smsBody
        self.senderHint = senderHint
    }

    public func perform() async throws -> some IntentResult {
        logger.info("ParseTransactionIntent.perform() called")

        // Ensure Rust backend is ready
        CentwiseRustBackend.initialize()

        var bodyToProcess = smsBody?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        var senderToProcess = senderHint?.trimmingCharacters(in: .whitespacesAndNewlines)

        // Smart auto-recovery: If user mapped the SMS text into `senderHint` or `smsBody`
        if let sender = senderToProcess, sender.contains("Tk") || sender.contains("BDT") || sender.contains("Payment") || sender.contains("Balance") || sender.count > 35 {
            if bodyToProcess.isEmpty || (!bodyToProcess.contains("Tk") && !bodyToProcess.contains("BDT")) {
                logger.info("Auto-recovered SMS body from senderHint parameter")
                bodyToProcess = sender
                senderToProcess = nil
            } else {
                // User mistakenly mapped Shortcut Input (full SMS) to BOTH smsBody and senderHint
                logger.info("Discarding full SMS text mistakenly passed as senderHint")
                senderToProcess = nil
            }
        } else if let sender = senderToProcess, sender == bodyToProcess {
            senderToProcess = nil
        }

        // If body is empty, return silently
        guard !bodyToProcess.isEmpty else {
            logger.warning("smsBody was nil or empty")
            return .result()
        }

        guard CentwiseRustBackend.isAvailable() else {
            logger.error("Rust backend not available")
            return .result()
        }

        logger.info("Processing SMS: \(bodyToProcess.prefix(60), privacy: .public)")
        _ = SmsTransactionProcessor.shared.processIncomingSms(body: bodyToProcess, senderHint: senderToProcess)
        return .result()
    }
}

