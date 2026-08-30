package com.centwise.core.scanner

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.centwise.core.processor.SmsTransactionProcessor
import com.centwise.core.uniffi.SmsIngestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans historical SMS and sends every message to the shared Rust engine. */
object HistoricalSmsScanner {
    private const val TAG = "HistoricalSmsScanner"

    data class ScanResult(
        val totalScanned: Int,
        val transactionsImported: Int
    )

    suspend fun scanInbox(
        context: Context,
        onProgress: (scanned: Int, imported: Int) -> Unit = { _, _ -> }
    ): ScanResult = withContext(Dispatchers.IO) {
        var totalScanned = 0
        var transactionsImported = 0
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)

                while (cursor.moveToNext()) {
                    totalScanned++
                    val body = bodyIndex.takeIf { it >= 0 }?.let(cursor::getString)
                    if (body != null) {
                        // Rust owns sender/provider matching, filtering, parsing, and persistence.
                        val result = SmsTransactionProcessor.processIncomingSms(
                            context = context.applicationContext,
                            sender = addressIndex.takeIf { it >= 0 }?.let(cursor::getString)
                                ?: "Financial SMS",
                            body = body,
                            timestamp = dateIndex.takeIf { it >= 0 }?.let(cursor::getLong)
                                ?: System.currentTimeMillis()
                        )
                        if (result?.status == SmsIngestStatus.INSERTED) {
                            transactionsImported++
                        }
                    }
                    if (totalScanned % 10 == 0) onProgress(totalScanned, transactionsImported)
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Error scanning historical SMS inbox", error)
        }

        withContext(Dispatchers.Main) {
            com.centwise.data.repository.TransactionRepository.shared.loadFromRust()
        }
        onProgress(totalScanned, transactionsImported)
        Log.i(TAG, "Finished historical scan: $totalScanned scanned, $transactionsImported imported")
        ScanResult(totalScanned, transactionsImported)
    }
}
