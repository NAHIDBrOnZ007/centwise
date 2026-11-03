package com.centwise.features.loans

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.centwise.MainActivity
import com.centwise.R
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.LoanRecord
import java.util.concurrent.TimeUnit

class LoanReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(EXTRA_LOAN_ID) ?: return Result.failure()
        val due = inputData.getLong(DUE, 0)
        CentwiseRustBackend.initialize(applicationContext)
        val loan = try {
            LoanRepository(applicationContext).loans().firstOrNull { it.id == id }
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Exception) {
            return Result.retry()
        }
        if (loan == null || loan.remainingMinor == 0L || loan.dueAtEpochMs != due) return Result.success()
        if (System.currentTimeMillis() < due) return Result.retry()
        val notifications = NotificationManagerCompat.from(applicationContext)
        if (!notifications.areNotificationsEnabled()) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, applicationContext.getString(R.string.loan_reminder_channel), NotificationManager.IMPORTANCE_DEFAULT
        ))
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_LOAN_ID, id)
            .setData(android.net.Uri.fromParts("centwise-loan", id, null))
        val open = PendingIntent.getActivity(applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.loan_reminder_channel))
            .setContentText(applicationContext.getString(R.string.loan_reminder_message))
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build()
        try {
            notifications.notify(id, 0, notification)
        } catch (_: SecurityException) {
            // Notification permission may be revoked between the check and delivery.
        }
        return Result.success()
    }

    companion object {
        const val EXTRA_LOAN_ID = "com.centwise.LOAN_ID"
        private const val DUE = "due"
        private const val CHANNEL = "loan_reminders"

        internal fun schedule(context: Context, loan: LoanRecord) {
            val work = WorkManager.getInstance(context)
            val name = "loan-reminder-${loan.id}"
            NotificationManagerCompat.from(context).cancel(loan.id, 0)
            val due = loan.dueAtEpochMs
            if (loan.remainingMinor == 0L || due == null) {
                work.cancelUniqueWork(name)
                return
            }
            val request = OneTimeWorkRequestBuilder<LoanReminderWorker>()
                .setInputData(workDataOf(EXTRA_LOAN_ID to loan.id, DUE to due))
                .setInitialDelay((due - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .build()
            work.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
