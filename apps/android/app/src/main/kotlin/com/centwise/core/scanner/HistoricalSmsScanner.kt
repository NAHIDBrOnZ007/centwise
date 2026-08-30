package com.centwise.core.scanner

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.SmsBatchMessage
import com.centwise.core.uniffi.SmsIngestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Scans historical SMS and sends every message to the shared Rust engine. */
object HistoricalSmsScanner {
    private const val TAG = "HistoricalSmsScanner"
    private const val PREFS_NAME = "centwise_settings"
    private const val LAST_SCAN_EPOCH_MS = "lastSmsScanEpochMs"
    // Bump when the scan pipeline changes so an old cursor cannot hide history.
    private const val SCAN_PIPELINE_VERSION = 2
    private const val SCAN_PIPELINE_VERSION_KEY = "smsScanPipelineVersion"
    private const val OVERLAP_MS = 60_000L
    private const val BATCH_SIZE = 100

    data class ScanResult(
        val totalScanned: Int,
        val transactionsImported: Int
    )

    suspend fun scanInbox(
        context: Context,
        onProgress: (scanned: Int, imported: Int) -> Unit = { _, _ -> }
    ): ScanResult = withContext(Dispatchers.IO) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousScan = preferences.getLong(LAST_SCAN_EPOCH_MS, 0L)
        val pipelineIsCurrent = preferences.getInt(SCAN_PIPELINE_VERSION_KEY, 0) == SCAN_PIPELINE_VERSION
        // A new pipeline must rebuild history once; subsequent scans are incremental.
        val scanStart = if (pipelineIsCurrent && previousScan > 0L) {
            (previousScan - OVERLAP_MS).coerceAtLeast(0L)
        } else {
            0L
        }
        val messages = mutableListOf<SmsBatchMessage>()
        var newestMessageTime = previousScan
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                if (scanStart > 0L) "${Telephony.Sms.DATE} >= ?" else null,
                if (scanStart > 0L) arrayOf(scanStart.toString()) else null,
                "${Telephony.Sms.DATE} ASC"
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val body = bodyIndex.takeIf { it >= 0 }?.let(cursor::getString) ?: continue
                    val timestamp = dateIndex.takeIf { it >= 0 }?.let(cursor::getLong)
                        ?: System.currentTimeMillis()
                    newestMessageTime = maxOf(newestMessageTime, timestamp)
                    messages += SmsBatchMessage(
                        body = body,
                        senderHint = addressIndex.takeIf { it >= 0 }?.let(cursor::getString),
                        occurredAtEpochMs = timestamp
                    )
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Error reading historical SMS inbox", error)
        }

        var imported = 0
        var processed = 0
        CentwiseRustBackend.initialize(context.applicationContext)
        messages.chunked(BATCH_SIZE).forEach { batch ->
            val results = CentwiseRustBackend.ingestSmsBatch(batch)
            imported += results.count { it.status == SmsIngestStatus.INSERTED }
            processed += batch.size
            onProgress(processed, imported)
        }

        withContext(Dispatchers.Main) {
            com.centwise.data.repository.TransactionRepository.shared.loadFromRust()
        }
        preferences.edit()
            .putInt(SCAN_PIPELINE_VERSION_KEY, SCAN_PIPELINE_VERSION)
            .apply {
                if (newestMessageTime > previousScan) {
                    putLong(LAST_SCAN_EPOCH_MS, newestMessageTime)
                }
            }
            .apply()
        Log.i(TAG, "Finished scan from $scanStart: ${messages.size} scanned, $imported imported")
        ScanResult(messages.size, imported)
    }
}
