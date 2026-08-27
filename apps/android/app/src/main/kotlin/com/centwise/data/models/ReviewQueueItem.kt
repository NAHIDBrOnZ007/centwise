package com.centwise.data.models

import java.util.UUID

/**
 * An unparsed or ambiguous SMS message from a recognized financial provider
 * awaiting user confirmation or review.
 */
data class ReviewQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val rawSms: String,
    val timestamp: Long = System.currentTimeMillis(),
    val candidateAmount: Double? = null,
    val candidateParty: String? = null,
    val candidateType: TransactionType? = null,
    val reference: String? = null,
    val reason: String = "Format needs confirmation"
)
