package com.centwise.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.centwise.core.processor.SmsTransactionProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that intercepts incoming SMS messages in real-time
 * from Bangladeshi Banks and MFS providers (bKash, Nagad, Rocket, City Bank, etc.).
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        // Re-assemble multi-part SMS messages by sender address
        data class SmsData(val body: StringBuilder, var timestamp: Long)
        val smsMap = mutableMapOf<String, SmsData>()

        for (message in messages) {
            val sender = message.originatingAddress ?: continue
            val body = message.messageBody ?: continue
            val timestamp = message.timestampMillis

            val existing = smsMap.getOrPut(sender) { SmsData(StringBuilder(), timestamp) }
            existing.body.append(body)
            if (timestamp < existing.timestamp) {
                existing.timestamp = timestamp
            }
        }

        val pendingResult = goAsync()
        receiverScope.launch {
            try {
                smsMap.map { (sender, smsData) ->
                    launch {
                        try {
                            SmsTransactionProcessor.processIncomingSms(
                                context = context.applicationContext,
                                sender = sender,
                                body = smsData.body.toString(),
                                timestamp = smsData.timestamp
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing incoming SMS from $sender", e)
                        }
                    }
                }.joinAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
