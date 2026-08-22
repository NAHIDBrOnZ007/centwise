package com.centwise.core.processor

import android.content.Context
import android.util.Log
import com.centwise.core.notifications.CentwiseNotifications
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import java.util.UUID

/**
 * Shared transaction processor for SMS messages in Centwise.
 * Processes single or multi-part SMS messages from bKash, Nagad, Rocket, and Bangladeshi Banks.
 */
object SmsTransactionProcessor {

    private const val TAG = "SmsTransactionProcessor"

    // Set of seen references (TrxID) for deduplication
    private val seenReferences = mutableSetOf<String>()

    /**
     * Parses an incoming SMS message, inserts the transaction into the repository,
     * and shows a system notification.
     *
     * @return The created [TransactionItem] if valid, or null if rejected/duplicate/not a transaction.
     */
    fun processIncomingSms(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): TransactionItem? {
        val trimmed = body.trim()
        val lower = trimmed.lowercase()

        // 1. Safety Filters: Reject OTPs and promotional spam
        if (isOtpOrSecurity(lower) || isPromoOrSpam(lower)) {
            Log.d(TAG, "Ignoring non-transaction or OTP SMS from $sender")
            return null
        }

        // 2. Extract Reference / TrxID
        val reference = extractReference(trimmed)
        if (reference != null && seenReferences.contains(reference)) {
            Log.d(TAG, "Skipping duplicate transaction with TrxID: $reference")
            return null
        }

        // 3. Extract Main Amount
        val amount = extractAmount(trimmed)
        if (amount == null) {
            val isRecognizedSender = isFinancialSender(sender, trimmed)
            if (isRecognizedSender) {
                Log.d(TAG, "Sending ambiguous SMS from $sender to Review Queue")
                com.centwise.data.repository.ReviewQueueRepository.shared.addItem(
                    com.centwise.data.models.ReviewQueueItem(
                        sender = sender,
                        rawSms = trimmed,
                        timestamp = timestamp,
                        reason = "Missing or unreadable amount"
                    )
                )
            }
            return null
        }

        // 4. Detect Transaction Type (Income vs. Expense)
        val type = detectTransactionType(lower)
        if (type == null) {
            val isRecognizedSender = isFinancialSender(sender, trimmed)
            if (isRecognizedSender) {
                Log.d(TAG, "Sending unclassified SMS from $sender to Review Queue")
                com.centwise.data.repository.ReviewQueueRepository.shared.addItem(
                    com.centwise.data.models.ReviewQueueItem(
                        sender = sender,
                        rawSms = trimmed,
                        timestamp = timestamp,
                        candidateAmount = amount,
                        reason = "Unknown transaction action"
                    )
                )
            }
            return null
        }

        // 5. Extract Party / Merchant
        val party = extractParty(trimmed)
        val providerName = resolveProviderName(sender, trimmed)
        val category = resolveCategory(party, trimmed, type == TransactionType.INCOME)
        val title = party ?: "$providerName ${type.displayName}"

        val transaction = TransactionItem(
            id = UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            type = type,
            category = category,
            paymentMethod = providerName,
            timestamp = timestamp,
            note = reference?.let { "TrxID: $it" },
            rawSms = trimmed
        )

        // Store reference for dedup
        reference?.let { seenReferences.add(it) }

        // Insert into repository
        FakeTransactionRepository.shared.addTransaction(transaction)

        // Trigger rich notification
        CentwiseNotifications.notifyNewTransaction(context, transaction)

        Log.i(TAG, "Successfully tracked transaction: ${transaction.title} - ৳${transaction.amount}")
        return transaction
    }

    private fun isOtpOrSecurity(lower: String): Boolean {
        val hasOtpKeyword = lower.contains("otp") ||
                lower.contains("one time password") ||
                lower.contains("verification code") ||
                lower.contains("do not share") ||
                lower.contains("security code")

        val hasAction = lower.contains("successful") ||
                lower.contains("credited") ||
                lower.contains("debited") ||
                lower.contains("cash out") ||
                lower.contains("send money") ||
                lower.contains("payment")

        return hasOtpKeyword && !hasAction
    }

    private fun isPromoOrSpam(lower: String): Boolean {
        return (lower.contains("app update") || lower.contains("download now")) &&
                !lower.contains("successful")
    }

    private fun extractReference(text: String): String? {
        val regex = Regex("""(?i)\b(?:TrxID|TxnID|Ref|Txn\s*ID)[:\s]+([A-Za-z0-9]+)""")
        return regex.find(text)?.groupValues?.get(1)?.takeIf {
            !it.equals("not", ignoreCase = true) && !it.equals("na", ignoreCase = true)
        }
    }

    private fun extractAmount(text: String): Double? {
        // Look for verb-associated amounts first
        val verbRegex = Regex(
            """(?i)(?:Cash\s+In|Cash\s+Out|Send\s+Money|Payment|Recharge|Withdrawal|credited\s+with|debited\s+with|EMI\s+of|Cashback(?:/Interest)?\s+of|received|credited\s+to|deposited)\s+(?:of\s+)?(?:Tk\s*)?([0-9.,]+)"""
        )
        verbRegex.find(text)?.groupValues?.get(1)?.let { raw ->
            parseCleanAmount(raw)?.let { return it }
        }

        // Look for Tk <number> that is not preceded by Fee or Balance
        val tkRegex = Regex("""(?i)\b(?:Tk|tk|TK|৳|BDT)\s*([0-9.,]+)""")
        for (match in tkRegex.findAll(text)) {
            val start = match.range.first
            val preceding = text.substring(maxOf(0, start - 20), start).lowercase()
            if (preceding.contains("fee") || preceding.contains("balance")) {
                continue
            }
            match.groupValues.getOrNull(1)?.let { raw ->
                parseCleanAmount(raw)?.let { return it }
            }
        }

        return null
    }

    private fun parseCleanAmount(raw: String): Double? {
        val cleaned = raw.replace(",", "").trim()
        return cleaned.toDoubleOrNull()?.takeIf { it > 0 }
    }

    private fun detectTransactionType(lower: String): TransactionType? {
        if (lower.contains("cash in") ||
            lower.contains("received") ||
            lower.contains("credited") ||
            lower.contains("add money") ||
            lower.contains("cashback") ||
            lower.contains("interest") ||
            lower.contains("salary") ||
            lower.contains("deposit")
        ) {
            return TransactionType.INCOME
        }

        if (lower.contains("cash out") ||
            lower.contains("send money") ||
            lower.contains("payment") ||
            lower.contains("debited") ||
            lower.contains("withdrawal") ||
            lower.contains("recharge") ||
            lower.contains("emi") ||
            lower.contains("deducted") ||
            lower.contains("purchase") ||
            lower.contains("bill pay")
        ) {
            return TransactionType.EXPENSE
        }

        return null
    }

    private fun extractParty(text: String): String? {
        val toRegex = Regex("""(?i)\bto\s+([0-9A-Za-z\s'.-]+?)(?:\s+is)?\s+successful""")
        toRegex.find(text)?.groupValues?.get(1)?.trim()?.let {
            if (it.isNotEmpty() && !it.equals("your a/c", ignoreCase = true)) return it
        }

        val fromRegex = Regex("""(?i)\bfrom\s+([0-9A-Za-z\s'.-]+?)\s+(?:successful|\.|\,)""")
        fromRegex.find(text)?.groupValues?.get(1)?.trim()?.let {
            if (it.isNotEmpty() && !it.lowercase().startsWith("a/c")) return it
        }

        return null
    }

    private fun resolveProviderName(sender: String, body: String): String {
        val s = sender.lowercase()
        val b = body.lowercase()
        return when {
            s.contains("bkash") || b.contains("bkash") -> "bKash"
            s.contains("nagad") || b.contains("nagad") -> "Nagad"
            s.contains("rocket") || s.contains("16216") || b.contains("rocket") -> "Rocket"
            s.contains("city") || b.contains("city bank") -> "City Bank"
            s.contains("brac") || b.contains("brac bank") -> "BRAC Bank"
            s.contains("ebl") || b.contains("eastern bank") -> "EBL"
            s.contains("dbbl") || b.contains("dutch-bangla") -> "DBBL"
            else -> "Bank Account"
        }
    }

    private fun isFinancialSender(sender: String, body: String): Boolean {
        val s = sender.lowercase()
        val b = body.lowercase()
        return s.contains("bkash") || s.contains("nagad") || s.contains("rocket") ||
                s.contains("16216") || s.contains("city") || s.contains("brac") ||
                s.contains("ebl") || s.contains("dbbl") || s.contains("sonali") ||
                s.contains("islami") || s.contains("pubali") || s.contains("prime") ||
                b.contains("a/c xxxx") || b.contains("[bank name]") || b.contains("trxid")
    }

    private fun resolveCategory(party: String?, body: String, isIncome: Boolean): String {
        // 1. Check user-defined Smart Rules first (highest priority)
        val target = party ?: body
        com.centwise.data.repository.SmartRulesRepository.shared.applyRules(target)?.let { rule ->
            return rule.categoryName
        }

        // 2. Default merchant dictionary & keywords
        val combined = "${party ?: ""} $body".lowercase()

        return when {
            combined.contains("foodpanda") || combined.contains("hungerstation") ||
                    combined.contains("kfc") || combined.contains("pizza") || combined.contains("dine") -> "Food & Dining"

            combined.contains("pathao") || combined.contains("uber") ||
                    combined.contains("shohoz") || combined.contains("obhai") || combined.contains("cng") -> "Transport"

            combined.contains("daraz") || combined.contains("pickaboo") ||
                    combined.contains("unimart") || combined.contains("shwapno") || combined.contains("agora") -> "Shopping"

            combined.contains("recharge") || combined.contains("airtel") ||
                    combined.contains("gp") || combined.contains("grameenphone") || combined.contains("robi") || combined.contains("banglalink") -> "Mobile Recharge"

            combined.contains("netflix") || combined.contains("spotify") || combined.contains("cineplex") -> "Entertainment"

            combined.contains("atm") || combined.contains("cash withdrawal") -> "Cash Withdrawal"

            combined.contains("emi") || combined.contains("bill") || combined.contains("desco") || combined.contains("wasa") -> "Bills & Utilities"

            isIncome -> "Income"
            else -> "General"
        }
    }
}
