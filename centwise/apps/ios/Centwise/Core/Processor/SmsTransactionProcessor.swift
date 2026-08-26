import Foundation
import SwiftUI

public final class SmsTransactionProcessor {
    public static let shared = SmsTransactionProcessor()

    private var seenReferences = Set<String>()

    public init() {}

    @discardableResult
    public func processIncomingSms(
        body: String,
        senderHint: String? = nil,
        date: Date = Date()
    ) -> CentwiseTransaction? {
        let trimmed = body.trimmingCharacters(in: .whitespacesAndNewlines)
        let lower = trimmed.lowercased()

        // 1. Safety Filters: Reject OTPs and promotional spam
        if isOtpOrSecurity(lower) || isPromoOrSpam(lower) {
            return nil
        }

        // 2. Extract Reference / TrxID
        let reference = extractReference(trimmed)
        if let ref = reference, seenReferences.contains(ref) {
            return nil
        }

        // 3. Extract Main Amount
        guard let amount = extractAmount(trimmed) else {
            if isFinancialSender(senderHint: senderHint, body: trimmed) {
                ReviewQueueRepository.shared.addItem(
                    ReviewQueueItem(
                        sender: senderHint ?? "Financial SMS",
                        rawSms: trimmed,
                        timestamp: date,
                        reason: "Missing or unreadable amount"
                    )
                )
            }
            return nil
        }

        // 4. Detect Transaction Type
        guard let type = detectTransactionType(lower) else {
            if isFinancialSender(senderHint: senderHint, body: trimmed) {
                ReviewQueueRepository.shared.addItem(
                    ReviewQueueItem(
                        sender: senderHint ?? "Financial SMS",
                        rawSms: trimmed,
                        timestamp: date,
                        candidateAmount: amount,
                        reason: "Unknown transaction action"
                    )
                )
            }
            return nil
        }

        // 5. Extract Party / Merchant
        let party = extractParty(trimmed)
        let provider = resolveProvider(senderHint: senderHint, body: trimmed)
        let category = resolveCategory(party: party, body: trimmed, isIncome: type == .income)
        let title = party ?? "\(provider.rawValue) \(type.rawValue)"

        // Match or default account
        let matchedAccount = TransactionRepository.shared.accounts.first(where: { $0.provider == provider })
            ?? TransactionRepository.shared.accounts.first
            ?? FinancialAccount(name: provider.rawValue, provider: provider, type: .mfs, currentBalance: 0.0)

        let transaction = CentwiseTransaction(
            title: title,
            amount: amount,
            type: type,
            category: category,
            date: date,
            accountId: matchedAccount.id,
            accountName: matchedAccount.name,
            provider: matchedAccount.provider,
            rawSmsBody: trimmed,
            transactionReference: reference,
            notes: reference != nil ? "TrxID: \(reference!)" : nil,
            isAutoTracked: true
        )

        if let ref = reference {
            seenReferences.insert(ref)
        }

        TransactionRepository.shared.addTransaction(transaction)
        CentwiseNotifications.notifyNewTransaction(transaction)
        ProfileManager.shared.isShortcutsSetupActive = true

        return transaction
    }

    private func isOtpOrSecurity(_ lower: String) -> Bool {
        let hasOtp = lower.contains("otp") ||
            lower.contains("one time password") ||
            lower.contains("verification code") ||
            lower.contains("do not share") ||
            lower.contains("security code")

        let hasAction = lower.contains("successful") ||
            lower.contains("credited") ||
            lower.contains("debited") ||
            lower.contains("cash out") ||
            lower.contains("send money") ||
            lower.contains("payment")

        return hasOtp && !hasAction
    }

    private func isPromoOrSpam(_ lower: String) -> Bool {
        (lower.contains("app update") || lower.contains("download now")) && !lower.contains("successful")
    }

    private func isFinancialSender(senderHint: String?, body: String) -> Bool {
        let s = (senderHint ?? "").lowercased()
        let b = body.lowercased()
        return s.contains("bkash") || s.contains("nagad") || s.contains("rocket") ||
            s.contains("16216") || s.contains("city") || s.contains("brac") ||
            s.contains("ebl") || s.contains("dbbl") || s.contains("sonali") ||
            s.contains("islami") || s.contains("pubali") || s.contains("prime") ||
            b.contains("a/c xxxx") || b.contains("[bank name]") || b.contains("trxid")
    }

    private func extractReference(_ text: String) -> String? {
        let pattern = #"(?i)\b(?:TrxID|TxnID|Ref|Txn\s*ID)[:\s]+([A-Za-z0-9]+)"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return nil }
        let nsString = text as NSString
        let results = regex.matches(in: text, range: NSRange(location: 0, length: nsString.length))
        for match in results {
            if match.numberOfRanges > 1 {
                let val = nsString.substring(with: match.range(at: 1)).trimmingCharacters(in: .whitespaces)
                let lowerVal = val.lowercased()
                if lowerVal != "not" && lowerVal != "na" {
                    return val
                }
            }
        }
        return nil
    }

    private func extractAmount(_ text: String) -> Double? {
        let verbPattern = #"(?i)(?:Cash\s+In|Cash\s+Out|Send\s+Money|Payment|Recharge|Withdrawal|credited\s+with|debited\s+with|EMI\s+of|Cashback(?:/Interest)?\s+of|received|credited\s+to|deposited)\s+(?:of\s+)?(?:Tk\s*)?([0-9.,]+)"#
        if let regex = try? NSRegularExpression(pattern: verbPattern) {
            let nsString = text as NSString
            if let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: nsString.length)), match.numberOfRanges > 1 {
                let raw = nsString.substring(with: match.range(at: 1))
                if let amt = parseCleanAmount(raw) { return amt }
            }
        }

        let tkPattern = #"(?i)\b(?:Tk|tk|TK|৳|BDT)\s*([0-9.,]+)"#
        if let regex = try? NSRegularExpression(pattern: tkPattern) {
            let nsString = text as NSString
            let matches = regex.matches(in: text, range: NSRange(location: 0, length: nsString.length))
            for match in matches {
                let start = match.range.location
                let prefixRange = NSRange(location: max(0, start - 20), length: min(start, 20))
                let preceding = nsString.substring(with: prefixRange).lowercased()
                if preceding.contains("fee") || preceding.contains("balance") {
                    continue
                }
                if match.numberOfRanges > 1 {
                    let raw = nsString.substring(with: match.range(at: 1))
                    if let amt = parseCleanAmount(raw) { return amt }
                }
            }
        }

        return nil
    }

    private func parseCleanAmount(_ raw: String) -> Double? {
        let cleaned = raw.replacingOccurrences(of: ",", with: "").trimmingCharacters(in: .whitespaces)
        guard let value = Double(cleaned), value > 0 else { return nil }
        return value
    }

    private func detectTransactionType(_ lower: String) -> TransactionType? {
        if lower.contains("cash in") ||
            lower.contains("received") ||
            lower.contains("credited") ||
            lower.contains("add money") ||
            lower.contains("cashback") ||
            lower.contains("interest") ||
            lower.contains("salary") ||
            lower.contains("deposit") {
            return .income
        }

        if lower.contains("cash out") ||
            lower.contains("send money") ||
            lower.contains("payment") ||
            lower.contains("debited") ||
            lower.contains("withdrawal") ||
            lower.contains("recharge") ||
            lower.contains("emi") ||
            lower.contains("deducted") ||
            lower.contains("purchase") ||
            lower.contains("bill pay") {
            return .expense
        }

        return nil
    }

    private func extractParty(_ text: String) -> String? {
        let toPattern = #"(?i)\bto\s+([0-9A-Za-z\s'.-]+?)(?:\s+is)?\s+successful"#
        if let regex = try? NSRegularExpression(pattern: toPattern) {
            let nsString = text as NSString
            if let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: nsString.length)), match.numberOfRanges > 1 {
                let party = nsString.substring(with: match.range(at: 1)).trimmingCharacters(in: .whitespaces)
                if !party.isEmpty && party.lowercased() != "your a/c" {
                    return party
                }
            }
        }

        let fromPattern = #"(?i)\bfrom\s+([0-9A-Za-z\s'.-]+?)\s+(?:successful|\.|\,)"#
        if let regex = try? NSRegularExpression(pattern: fromPattern) {
            let nsString = text as NSString
            if let match = regex.firstMatch(in: text, range: NSRange(location: 0, length: nsString.length)), match.numberOfRanges > 1 {
                let party = nsString.substring(with: match.range(at: 1)).trimmingCharacters(in: .whitespaces)
                if !party.isEmpty && !party.lowercased().hasPrefix("a/c") {
                    return party
                }
            }
        }

        return nil
    }

    private func resolveProvider(senderHint: String?, body: String) -> FinancialProvider {
        let s = (senderHint ?? "").lowercased()
        let b = body.lowercased()
        if s.contains("bkash") || b.contains("bkash") { return .bkash }
        if s.contains("nagad") || b.contains("nagad") { return .nagad }
        if s.contains("rocket") || s.contains("16216") || b.contains("rocket") { return .rocket }
        if s.contains("city") || b.contains("city bank") { return .cityBank }
        if s.contains("brac") || b.contains("brac bank") { return .bracBank }
        if s.contains("ebl") || b.contains("eastern bank") { return .easternBank }
        return .bkash
    }

    private func resolveCategory(party: String?, body: String, isIncome: Bool) -> TransactionCategory {
        let target = party ?? body

        // 1. Check user-defined Smart Rules first
        if let rule = SmartRulesRepository.shared.applyRules(merchantOrParty: target) {
            return rule.category
        }

        let combined = "\(party ?? "") \(body)".lowercased()

        if combined.contains("foodpanda") || combined.contains("hungerstation") ||
            combined.contains("kfc") || combined.contains("pizza") || combined.contains("dine") {
            return .food
        }
        if combined.contains("pathao") || combined.contains("uber") ||
            combined.contains("shohoz") || combined.contains("obhai") || combined.contains("cng") {
            return .transport
        }
        if combined.contains("daraz") || combined.contains("pickaboo") ||
            combined.contains("unimart") || combined.contains("shwapno") || combined.contains("agora") {
            return .shopping
        }
        if combined.contains("recharge") || combined.contains("airtel") ||
            combined.contains("gp") || combined.contains("grameenphone") || combined.contains("robi") || combined.contains("banglalink") {
            return .recharge
        }
        if combined.contains("netflix") || combined.contains("spotify") || combined.contains("cineplex") {
            return .entertainment
        }
        if combined.contains("emi") || combined.contains("bill") || combined.contains("desco") || combined.contains("wasa") {
            return .bills
        }

        return isIncome ? .salary : .other
    }
}
