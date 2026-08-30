package com.centwise.core.processor

import android.content.Context
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.SmsIngestResult
import com.centwise.core.uniffi.SmsIngestStatus
import com.centwise.data.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Thin Android adapter: capture details enter parser/Rust; persistence and UI update seamlessly. */
object SmsTransactionProcessor {
    fun processIncomingSms(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis(),
        notifyUser: Boolean = true
    ): SmsIngestResult? {
        CentwiseRustBackend.initialize(context.applicationContext)
        val result = CentwiseRustBackend.ingestSms(sender, body, timestamp)

        if (notifyUser && result != null && result.status == SmsIngestStatus.INSERTED && result.transactionId != null) {
            // Show transaction notification
            try {
                val record = CentwiseRustBackend.getTransaction(result.transactionId)
                if (record != null) {
                    com.centwise.core.notifications.CentwiseNotifications.notifyNewTransaction(
                        context = context.applicationContext,
                        transaction = com.centwise.data.models.TransactionItem(
                            id = record.id,
                            title = record.title,
                            amount = record.amountMinor / 100.0,
                            type = if (record.kind == com.centwise.core.uniffi.TransactionKind.INCOME) com.centwise.data.models.TransactionType.INCOME else com.centwise.data.models.TransactionType.EXPENSE,
                            category = record.categoryId,
                            paymentMethod = sender,
                            timestamp = record.occurredAtEpochMs,
                            note = record.notes,
                            rawSms = record.rawSms,
                            reference = record.reference
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // Notify and refresh UI StateFlows immediately
        if (notifyUser) {
            CoroutineScope(Dispatchers.Main).launch {
                TransactionRepository.shared.loadFromRust()
            }
        }

        return result
    }
}
