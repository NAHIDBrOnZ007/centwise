package com.centwise.data.repository

import com.centwise.data.models.ReviewQueueItem
import com.centwise.data.models.TransactionItem
import com.centwise.core.backend.CentwiseRustBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository managing unparsed or ambiguous SMS messages waiting in the review queue.
 */
class ReviewQueueRepository private constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _items = MutableStateFlow<List<ReviewQueueItem>>(emptyList())
    val items: StateFlow<List<ReviewQueueItem>> = _items.asStateFlow()

    val pendingCount = _items.map { it.size }

    fun refresh() {
        scope.launch {
            _items.value = CentwiseRustBackend.listReviewQueue().map { item ->
                ReviewQueueItem(
                    id = item.id,
                    sender = item.sender ?: "Financial SMS",
                    rawSms = item.rawSms,
                    timestamp = item.receivedAtEpochMs,
                    candidateAmount = item.candidateAmountMinor?.div(100.0),
                    candidateParty = item.party ?: item.merchant,
                    candidateType = item.candidateKind?.let { kind ->
                        when (kind) {
                            com.centwise.core.uniffi.TransactionKind.EXPENSE -> com.centwise.data.models.TransactionType.EXPENSE
                            com.centwise.core.uniffi.TransactionKind.INCOME -> com.centwise.data.models.TransactionType.INCOME
                            com.centwise.core.uniffi.TransactionKind.TRANSFER -> com.centwise.data.models.TransactionType.TRANSFER
                            com.centwise.core.uniffi.TransactionKind.REFUND -> com.centwise.data.models.TransactionType.CREDIT
                        }
                    },
                    reference = item.reference,
                    reason = item.reason
                )
            }
        }
    }

    fun dismissItem(id: String) {
        scope.launch {
            if (CentwiseRustBackend.dismissReviewQueueItem(id)) refresh()
        }
    }

    suspend fun confirmAsTransaction(item: ReviewQueueItem, transaction: TransactionItem): Boolean =
        withContext(Dispatchers.IO) {
            if (!CentwiseRustBackend.convertReviewQueueItem(item.id, transaction)) return@withContext false
            refresh()
            TransactionRepository.shared.loadFromRust()
            true
        }

    companion object {
        val shared = ReviewQueueRepository()
    }
}
