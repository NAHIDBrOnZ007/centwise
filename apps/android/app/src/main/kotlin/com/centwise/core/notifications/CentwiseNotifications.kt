package com.centwise.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.centwise.R
import com.centwise.data.models.TransactionItem

object CentwiseNotifications {

    private const val CHANNEL_TRANSACTIONS = "centwise_transactions"
    private const val NOTIFICATION_ID_BASE = 1000

    /** Creates notification channels. Safe to call on every app start. */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val transactionsChannel = NotificationChannel(
            CHANNEL_TRANSACTIONS,
            context.getString(R.string.notification_channel_transactions),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_transactions_description)
        }
        manager.createNotificationChannel(transactionsChannel)
    }

    fun canNotify(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /** Shows a local notification for a newly tracked transaction. */
    fun notifyNewTransaction(context: Context, transaction: TransactionItem) {
        if (!canNotify(context)) return

        ensureChannels(context)

        val sign = if (transaction.type == com.centwise.data.models.TransactionType.INCOME) "+" else "-"
        val content = "${transaction.category} • $sign৳${"%,.0f".format(transaction.amount)}"

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(context.getString(R.string.notification_new_transaction_title))
            .setContentText("${transaction.title} — $content")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${transaction.title}\n$content"))
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + transaction.id.hashCode() % 10000, notification)
        } catch (securityException: SecurityException) {
            // Permission revoked between check and notify; ignore
        }
    }
}
