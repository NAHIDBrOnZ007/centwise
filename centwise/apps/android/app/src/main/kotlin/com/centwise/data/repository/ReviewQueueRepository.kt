package com.centwise.data.repository

import com.centwise.data.models.ReviewQueueItem
import com.centwise.data.models.TransactionItem
import com.centwise.data.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository managing unparsed or ambiguous SMS messages waiting in the review queue.
 */
class ReviewQueueRepository private constructor() {

    private val _items = MutableStateFlow<List<ReviewQueueItem>>(emptyList())
    val items: StateFlow<List<ReviewQueueItem>> = _items.asStateFlow()

    val pendingCount = _items.map { it.size }

    fun addItem(item: ReviewQueueItem) {
        // Prevent duplicate queue entries for the identical SMS text
        if (_items.value.none { it.rawSms == item.rawSms }) {
            _items.value = listOf(item) + _items.value
        }
    }

    fun dismissItem(id: String) {
        _items.value = _items.value.filter { it.id != id }
    }

    fun confirmAsTransaction(item: ReviewQueueItem, transaction: TransactionItem) {
        FakeTransactionRepository.shared.addTransaction(transaction)
        dismissItem(item.id)
    }

    companion object {
        val shared = ReviewQueueRepository()
    }
}
