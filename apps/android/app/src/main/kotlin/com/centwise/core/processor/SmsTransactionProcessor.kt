package com.centwise.core.processor

import android.content.Context
import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.SmsIngestResult

/** Thin Android adapter: capture details enter Rust; parsing and persistence stay in Rust. */
object SmsTransactionProcessor {
    fun processIncomingSms(
        context: Context,
        sender: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ): SmsIngestResult? {
        CentwiseRustBackend.initialize(context.applicationContext)
        return CentwiseRustBackend.ingestSms(sender, body, timestamp)
    }
}
