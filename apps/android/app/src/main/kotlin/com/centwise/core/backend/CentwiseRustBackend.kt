package com.centwise.core.backend

import android.content.Context
import android.util.Log
import com.centwise.core.uniffi.AccountInput
import com.centwise.core.uniffi.BudgetInput
import com.centwise.core.uniffi.CentwiseCore
import com.centwise.core.uniffi.CategoryInput
import com.centwise.core.uniffi.DemoDataSummaryRecord
import com.centwise.core.uniffi.AccountRecord
import com.centwise.core.uniffi.BudgetRecord
import com.centwise.core.uniffi.CategoryRecord
import com.centwise.core.uniffi.SubscriptionRecord
import com.centwise.core.uniffi.SubscriptionInput
import com.centwise.core.uniffi.SmartRuleInput
import com.centwise.core.uniffi.SmartRuleRecord
import com.centwise.core.uniffi.TransactionRecord
import com.centwise.core.uniffi.SmsIngestResult
import com.centwise.core.uniffi.ReviewQueueRecord
import com.centwise.core.uniffi.TransactionInput
import com.centwise.core.uniffi.TransactionKind
import com.centwise.data.models.TransactionItem
import com.centwise.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Locale

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
        } catch (error: Throwable) {
            Log.e(TAG, "Rust core is unavailable; data features are disabled", error)
        }
    }

    fun ingestSms(sender: String?, body: String, timestamp: Long): SmsIngestResult? {
        val rustCore = core ?: return null
        return try {
            rustCore.ingestSms(body, sender, timestamp)
        } catch (error: Throwable) {
            Log.e(TAG, "Rust SMS ingestion failed", error)
            null
        }
    }

    fun isAvailable(): Boolean = core != null

    fun loadDemoData(): DemoDataSummaryRecord? = try {
        core?.loadDemoData()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust demo data load failed", error)
        null
    }

    fun resetToEmptyDatabase(): Boolean = try {
        core?.resetToEmptyDatabase()
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust database reset failed", error)
        false
    }

    fun listTransactions(): List<TransactionRecord> = try {
        core?.listTransactions(10_000u).orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction read failed", error)
        emptyList()
    }

    fun listAccounts(): List<AccountRecord> = try {
        core?.listAccounts().orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account read failed", error)
        emptyList()
    }

    fun listBudgets(): List<BudgetRecord> = try {
        core?.listBudgets().orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget read failed", error)
        emptyList()
    }

    fun listSubscriptions(): List<SubscriptionRecord> = try {
        core?.listSubscriptions().orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription read failed", error)
        emptyList()
    }

    fun listCategories(): List<CategoryRecord> = try {
        core?.listCategories().orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category read failed", error)
        emptyList()
    }

    fun insertCategory(input: CategoryInput): Boolean = try {
        core?.insertCategory(input)
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category insert failed", error)
        false
    }

    fun updateCategory(input: CategoryInput): Boolean = try {
        core?.updateCategory(input) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category update failed", error)
        false
    }

    fun deleteCategory(id: String): Boolean = try {
        core?.deleteCategory(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category delete failed", error)
        false
    }

    fun listRules(): List<SmartRuleRecord> = try {
        core?.listRules().orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule read failed", error)
        emptyList()
    }

    fun insertRule(input: SmartRuleInput): Boolean = try {
        core?.insertRule(input)
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule insert failed", error)
        false
    }

    fun updateRule(input: SmartRuleInput): Boolean = try {
        core?.updateRule(input) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule update failed", error)
        false
    }

    fun deleteRule(id: String): Boolean = try {
        core?.deleteRule(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule delete failed", error)
        false
    }

    fun listReviewQueue(): List<ReviewQueueRecord> = try {
        core?.listReviewQueue(100u).orEmpty()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust review queue read failed", error)
        emptyList()
    }

    fun dismissReviewQueueItem(id: String): Boolean = try {
        core?.dismissReviewQueueItem(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust review queue dismissal failed", error)
        false
    }

    fun convertReviewQueueItem(itemId: String, transaction: TransactionItem): Boolean {
        val rustCore = core ?: return false
        val allAccounts = TransactionRepository.shared.accounts.value
        val exactNameMatches = allAccounts.filter { account ->
            account.name.equals(transaction.paymentMethod, ignoreCase = true)
        }
        val providerMatches = allAccounts.filter { account ->
            account.providerName.equals(transaction.paymentMethod, ignoreCase = true)
        }
        // Do not silently assign a review item to an arbitrary wallet when the
        // user has multiple accounts for the same provider.
        val candidates = exactNameMatches.ifEmpty { providerMatches }
        if (candidates.size > 1) return false
        val account = candidates.singleOrNull()
        val providerHint = canonicalProvider(account?.providerName ?: transaction.paymentMethod)
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
                    accountId = account?.id.orEmpty(),
                    accountProvider = providerHint,
                    accountName = account?.name ?: transaction.paymentMethod.ifBlank { "Cash / Unassigned" },
                    accountLastFour = account?.accountNumber
                        ?.takeLast(4)
                        ?.takeIf { it.all(Char::isDigit) },
                    reference = transaction.reference,
                    balanceAfterMinor = null,
                    feeMinor = null,
                    notes = transaction.note,
                    rawSms = transaction.rawSms,
                    isAutoTracked = false
                )
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Rust review conversion failed", error)
            false
        }
    }

    fun insertAccount(account: com.centwise.data.models.AccountItem): Boolean = try {
        core?.insertAccount(account.toRustInput())
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account insert failed", error)
        false
    }

    fun updateAccount(account: com.centwise.data.models.AccountItem): Boolean = try {
        core?.updateAccount(account.toRustInput()) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account update failed", error)
        false
    }

    fun deleteAccount(id: String): Boolean = try {
        core?.deleteAccount(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account delete failed", error)
        false
    }

    fun insertTransaction(transaction: TransactionItem): Boolean = try {
        core?.insertTransaction(transaction.toRustInput())
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction insert failed", error)
        false
    }

    fun updateTransaction(transaction: TransactionItem): Boolean = try {
        core?.updateTransaction(transaction.toRustInput()) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction update failed", error)
        false
    }

    fun deleteTransaction(id: String): Boolean = try {
        core?.deleteTransaction(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction delete failed", error)
        false
    }

    fun insertBudget(budget: com.centwise.data.models.BudgetItem): Boolean = try {
        core?.insertBudget(budget.toRustInput())
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget insert failed", error)
        false
    }

    fun updateBudget(budget: com.centwise.data.models.BudgetItem): Boolean = try {
        core?.updateBudget(budget.toRustInput()) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget update failed", error)
        false
    }

    fun deleteBudget(id: String): Boolean = try {
        core?.deleteBudget(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget delete failed", error)
        false
    }

    fun insertSubscription(subscription: com.centwise.data.models.SubscriptionItem): Boolean = try {
        core?.insertSubscription(subscription.toRustInput())
        core != null
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription insert failed", error)
        false
    }

    fun updateSubscription(subscription: com.centwise.data.models.SubscriptionItem): Boolean = try {
        core?.updateSubscription(subscription.toRustInput()) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription update failed", error)
        false
    }

    fun deleteSubscription(id: String): Boolean = try {
        core?.deleteSubscription(id) == true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription delete failed", error)
        false
    }

    private fun canonicalProvider(provider: String): String = when {
        provider.contains("bkash", ignoreCase = true) -> "bkash"
        provider.contains("nagad", ignoreCase = true) -> "nagad"
        provider.contains("rocket", ignoreCase = true) -> "rocket"
        provider.contains("upay", ignoreCase = true) -> "upay"
        provider.contains("cellfin", ignoreCase = true) -> "cellfin"
        provider.contains("cash", ignoreCase = true) -> "cash"
        provider.contains("dbbl", ignoreCase = true) -> "dbbl"
        provider.contains("dutch", ignoreCase = true) -> "dbbl"
        provider.contains("city", ignoreCase = true) -> "city-bank"
        provider.contains("brac", ignoreCase = true) -> "brac-bank"
        provider.contains("ebl", ignoreCase = true) -> "ebl"
        provider.contains("eastern", ignoreCase = true) -> "ebl"
        provider.contains("standard chartered", ignoreCase = true) -> "standard-chartered"
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

    private fun com.centwise.data.models.AccountItem.toRustInput() = AccountInput(
        id = id,
        name = name,
        provider = canonicalProvider(providerName),
        lastFour = accountNumber.takeLast(4).takeIf { value -> value.all { it.isDigit() } },
        startingBalanceMinor = (balance * 100).toLong(),
        archived = archived
    )

    private fun com.centwise.data.models.TransactionItem.toRustInput(): TransactionInput {
        val account = TransactionRepository.shared.accounts.value.firstOrNull {
            it.name.equals(paymentMethod, ignoreCase = true)
        }
        val providerHint = canonicalProvider(account?.providerName ?: paymentMethod)
        return TransactionInput(
            id = id,
            title = title,
            amountMinor = (amount * 100).toLong(),
            currency = "BDT",
            kind = type.toRustKind(),
            categoryId = categoryId(category),
            occurredAtEpochMs = timestamp,
            accountId = account?.id.orEmpty(),
            accountProvider = providerHint,
            accountName = account?.name ?: paymentMethod.ifBlank { "Cash / Unassigned" },
            accountLastFour = account?.accountNumber
                ?.takeLast(4)
                ?.takeIf { value -> value.all(Char::isDigit) },
            reference = reference,
            balanceAfterMinor = null,
            feeMinor = null,
            notes = note,
            rawSms = rawSms,
            isAutoTracked = rawSms != null
        )
    }

    private fun com.centwise.data.models.BudgetItem.toRustInput() = BudgetInput(
        id = id,
        categoryId = categoryId.ifBlank { categoryId(categoryName) },
        limitMinor = (allocatedAmount * 100).toLong(),
        period = period.lowercase(),
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs
    )

    private fun com.centwise.data.models.SubscriptionItem.toRustInput() = SubscriptionInput(
        id = id,
        name = name,
        amountMinor = (amount * 100).toLong(),
        billingCycle = billingCycle.lowercase(),
        nextDueEpochMs = nextDueEpochMs.takeIf { it > 0 } ?: parseDate(nextBillingDate),
        isActive = isActive
    )

    private fun parseDate(value: String): Long = runCatching {
        SimpleDateFormat("MMM dd, yyyy", Locale.US).parse(value)?.time ?: 0L
    }.getOrDefault(0L)

    fun toSmartRuleInput(rule: com.centwise.data.models.SmartRule) = SmartRuleInput(
        id = rule.id,
        name = rule.name,
        keyword = rule.keyword,
        matchType = when (rule.matchType) {
            com.centwise.data.models.RuleMatchType.CONTAINS -> "contains"
            com.centwise.data.models.RuleMatchType.STARTS_WITH -> "starts_with"
            com.centwise.data.models.RuleMatchType.EXACTLY_MATCHES -> "exactly_matches"
        },
        categoryId = categoryId(rule.categoryName),
        kind = rule.transactionType.toRustKind(),
        isEnabled = rule.isEnabled
    )
}
