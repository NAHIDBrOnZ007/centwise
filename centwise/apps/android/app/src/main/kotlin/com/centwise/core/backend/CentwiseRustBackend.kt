package com.centwise.core.backend

import android.content.Context
import android.util.Log
import com.centwise.core.uniffi.AccountInput
import com.centwise.core.uniffi.CentwiseCore
import com.centwise.core.uniffi.DemoDataSummaryRecord
import com.centwise.core.uniffi.AccountRecord
import com.centwise.core.uniffi.BudgetRecord
import com.centwise.core.uniffi.CategoryRecord
import com.centwise.core.uniffi.SubscriptionRecord
import com.centwise.core.uniffi.TransactionRecord
import com.centwise.core.uniffi.SmsIngestResult
import com.centwise.core.uniffi.ReviewQueueRecord
import com.centwise.core.uniffi.TransactionInput
import com.centwise.core.uniffi.TransactionKind
import com.centwise.data.models.TransactionItem
import com.centwise.data.repository.TransactionRepository

/** Owns the process-wide Rust core handle. UI and SMS capture never open SQLite. */
object CentwiseRustBackend {
    private const val TAG = "CentwiseRustBackend"

    @Volatile
    private var core: CentwiseCore? = null

    @Synchronized
    fun initialize(context: Context) {
        if (core != null) return

        try {
            val databasePath = context.noBackupFilesDir.resolve("centwise.db").absolutePath
            core = CentwiseCore.open(databasePath)
            syncExistingAccounts()
        } catch (error: Exception) {
            // The app can still launch when a native ABI has not been packaged yet.
            Log.e(TAG, "Rust core is unavailable; SMS import is disabled", error)
        }
    }

    fun ingestSms(sender: String?, body: String, timestamp: Long): SmsIngestResult? {
        val rustCore = core ?: return null
        return try {
            rustCore.ingestSms(body, sender, timestamp)
        } catch (error: Exception) {
            Log.e(TAG, "Rust SMS ingestion failed", error)
            null
        }
    }

    fun isAvailable(): Boolean = core != null

    fun loadDemoData(): DemoDataSummaryRecord? = try {
        core?.loadDemoData()
    } catch (error: Exception) {
        Log.e(TAG, "Rust demo data load failed", error)
        null
    }

    fun resetToEmptyDatabase(): Boolean = try {
        core?.resetToEmptyDatabase()
        core != null
    } catch (error: Exception) {
        Log.e(TAG, "Rust database reset failed", error)
        false
    }

    fun listTransactions(): List<TransactionRecord> = try {
        core?.listTransactions(10_000u).orEmpty()
    } catch (error: Exception) {
        Log.e(TAG, "Rust transaction read failed", error)
        emptyList()
    }

    fun listAccounts(): List<AccountRecord> = try {
        core?.listAccounts().orEmpty()
    } catch (error: Exception) {
        Log.e(TAG, "Rust account read failed", error)
        emptyList()
    }

    fun listBudgets(): List<BudgetRecord> = try {
        core?.listBudgets().orEmpty()
    } catch (error: Exception) {
        Log.e(TAG, "Rust budget read failed", error)
        emptyList()
    }

    fun listSubscriptions(): List<SubscriptionRecord> = try {
        core?.listSubscriptions().orEmpty()
    } catch (error: Exception) {
        Log.e(TAG, "Rust subscription read failed", error)
        emptyList()
    }

    fun listCategories(): List<CategoryRecord> = try {
        core?.listCategories().orEmpty()
    } catch (error: Exception) {
        Log.e(TAG, "Rust category read failed", error)
        emptyList()
    }

    fun listReviewQueue(): List<ReviewQueueRecord> = try {
        core?.listReviewQueue(100u).orEmpty()
    } catch (error: Exception) {
        Log.e(TAG, "Rust review queue read failed", error)
        emptyList()
    }

    fun dismissReviewQueueItem(id: String): Boolean = try {
        core?.dismissReviewQueueItem(id) == true
    } catch (error: Exception) {
        Log.e(TAG, "Rust review queue dismissal failed", error)
        false
    }

    fun convertReviewQueueItem(itemId: String, transaction: TransactionItem): Boolean {
        val rustCore = core ?: return false
        val accounts = TransactionRepository.shared.accounts.value.filter { account ->
            account.providerName.equals(transaction.paymentMethod, ignoreCase = true) ||
                account.name.equals(transaction.paymentMethod, ignoreCase = true)
        }
        // Do not silently assign a review item to an arbitrary wallet when the
        // user has multiple accounts for the same provider.
        val account = accounts.singleOrNull() ?: return false
        return try {
            rustCore.convertReviewQueueItem(
                itemId,
                TransactionInput(
                    id = transaction.id,
                    title = transaction.title,
                    amountMinor = (transaction.amount * 100).toLong(),
                    currency = "BDT",
                    kind = transaction.type.toRustKind(),
                    categoryId = categoryId(transaction.category),
                    occurredAtEpochMs = transaction.timestamp,
                    accountId = account.id,
                    reference = transaction.reference,
                    balanceAfterMinor = null,
                    feeMinor = null,
                    notes = transaction.note,
                    rawSms = transaction.rawSms,
                    isAutoTracked = false
                )
            )
        } catch (error: Exception) {
            Log.e(TAG, "Rust review conversion failed", error)
            false
        }
    }

    private fun syncExistingAccounts() {
        // Transitional migration: existing account identities are copied into
        // Rust so SMS account resolution is fail-closed and does not invent one.
        TransactionRepository.shared.accounts.value.forEach { account ->
            try {
                core?.insertAccount(
                    AccountInput(
                        id = account.id,
                        name = account.name,
                        provider = canonicalProvider(account.providerName),
                        lastFour = account.accountNumber.takeLast(4).takeIf { value ->
                            value.all { character -> character.isDigit() }
                        },
                        startingBalanceMinor = (account.balance * 100).toLong()
                    )
                )
            } catch (_: Exception) {
                // Existing Rust account: keep startup idempotent.
            }
        }
    }

    private fun canonicalProvider(provider: String): String = when {
        provider.contains("bkash", ignoreCase = true) -> "bkash"
        provider.contains("nagad", ignoreCase = true) -> "nagad"
        provider.contains("rocket", ignoreCase = true) -> "rocket"
        provider.contains("dbbl", ignoreCase = true) -> "dbbl"
        provider.contains("city", ignoreCase = true) -> "city-bank"
        provider.contains("brac", ignoreCase = true) -> "brac-bank"
        provider.contains("ebl", ignoreCase = true) -> "ebl"
        else -> "banks-generic"
    }

    private fun categoryId(category: String): String {
        val categories = core?.listCategories().orEmpty()
        return categories.firstOrNull {
            it.id == category || it.name.equals(category, ignoreCase = true)
        }?.id ?: categories.firstOrNull { it.id == "other" }?.id.orEmpty()
    }

    private fun com.centwise.data.models.TransactionType.toRustKind(): TransactionKind = when (this) {
        com.centwise.data.models.TransactionType.INCOME -> TransactionKind.INCOME
        com.centwise.data.models.TransactionType.EXPENSE -> TransactionKind.EXPENSE
        com.centwise.data.models.TransactionType.TRANSFER -> TransactionKind.TRANSFER
        com.centwise.data.models.TransactionType.CREDIT -> TransactionKind.REFUND
    }
}
