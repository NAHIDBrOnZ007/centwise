package com.centwise.core.scanner

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.centwise.core.processor.SmsTransactionProcessor
import com.centwise.core.uniffi.SmsIngestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scans historical SMS inbox messages for initial onboarding transaction import.
 */
object HistoricalSmsScanner {

    private const val TAG = "HistoricalSmsScanner"

    data class ScanResult(
        val totalScanned: Int,
        val transactionsImported: Int
    )

    private val KNOWN_SENDER_PATTERNS = listOf(
        "bkash", "nagad", "rocket", "16216", "dbbl",
        "city", "brac", "ebl", "sonali", "islami", "pubali", "prime", "ucb"
    )

    /**
     * Scans the user's SMS inbox and parses historical transactions.
     * Must be called only after READ_SMS permission is granted.
     */
    suspend fun scanInbox(
        context: Context,
        onProgress: (scanned: Int, imported: Int) -> Unit = { _, _ -> }
    ): ScanResult = withContext(Dispatchers.IO) {
        var totalScanned = 0
        var transactionsImported = 0

        val contentResolver = context.contentResolver
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            val cursor = contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} ASC" // Process oldest to newest
            )

            cursor?.use { c ->
                val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)

                while (c.moveToNext()) {
                    totalScanned++
                    val address = if (addressIdx != -1) c.getString(addressIdx) else null
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) else null
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (address != null && body != null) {
                        val senderLower = address.lowercase()
                        val isRecognized = KNOWN_SENDER_PATTERNS.any { senderLower.contains(it) }

                        if (isRecognized) {
                            val tx = SmsTransactionProcessor.processIncomingSms(
                                context = context.applicationContext,
                                sender = address,
                                body = body,
                                timestamp = date
                            )
                            if (tx?.status == SmsIngestStatus.INSERTED) {
                                transactionsImported++
                            }
                        }
                    }

                    if (totalScanned % 20 == 0) {
                        onProgress(totalScanned, transactionsImported)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning historical SMS inbox", e)
        }

        onProgress(totalScanned, transactionsImported)
        Log.i(TAG, "Finished historical scan: $totalScanned scanned, $transactionsImported imported")
        ScanResult(totalScanned, transactionsImported)
    }
}
