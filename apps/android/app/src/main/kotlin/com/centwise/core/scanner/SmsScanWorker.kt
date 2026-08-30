package com.centwise.core.scanner

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf
import com.centwise.core.backend.CentwiseRustBackend

/** Runs the Rust-backed inbox scan away from the Compose/UI lifecycle. */
class SmsScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        if (isStopped) return Result.failure()
        return try {
            val result = HistoricalSmsScanner.scanInbox(applicationContext) { scanned, imported ->
                setProgressAsync(workDataOf("scanned" to scanned, "imported" to imported))
            }
            Result.success(
                workDataOf(
                    "scanned" to result.totalScanned,
                    "imported" to result.transactionsImported
                )
            )
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "centwise-rust-sms-scan"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<SmsScanWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
