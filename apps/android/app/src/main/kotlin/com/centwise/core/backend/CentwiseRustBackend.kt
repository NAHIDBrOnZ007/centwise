package com.centwise.core.backend

import android.content.Context
import android.util.Log
import com.centwise.core.uniffi.AccountInput
import com.centwise.core.uniffi.AccountRecord
import com.centwise.core.uniffi.BudgetInput
import com.centwise.core.uniffi.BudgetRecord
import com.centwise.core.uniffi.CategoryInput
import com.centwise.core.uniffi.CategoryRecord
import com.centwise.core.uniffi.CentwiseCore
import com.centwise.core.uniffi.DemoDataSummaryRecord
import com.centwise.core.uniffi.ReviewQueueRecord
import com.centwise.core.uniffi.HomeDashboardRecord
import com.centwise.core.uniffi.SmartRuleInput
import com.centwise.core.uniffi.SmartRuleRecord
import com.centwise.core.uniffi.SmsIngestResult
import com.centwise.core.uniffi.SmsIngestStatus
import com.centwise.core.uniffi.SmsBatchMessage
import com.centwise.core.uniffi.SubscriptionInput
import com.centwise.core.uniffi.SubscriptionRecord
import com.centwise.core.uniffi.TransactionInput
import com.centwise.core.uniffi.TransactionKind
import com.centwise.core.uniffi.TransactionRecord
import com.centwise.core.uniffi.AnalyticsSnapshotRecord
import com.centwise.data.models.TransactionItem
import com.centwise.data.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.Locale

/** Owns the process-wide Rust core handle and non-SMS debug fallback data. */
object CentwiseRustBackend {
    private const val TAG = "CentwiseRustBackend"

    @Volatile
    private var core: CentwiseCore? = null

    // In-process fallback engine used when Rust JNI shared library is unavailable in debug/emulator
    private val fallbackEngine = FallbackBackend()

    @Synchronized
    fun initialize(context: Context) {
        if (core != null) return

        try {
            val databasePath = context.noBackupFilesDir.resolve("centwise.db").absolutePath
            core = CentwiseCore.open(databasePath)
            Log.i(TAG, "Rust core initialized successfully at $databasePath")
        } catch (error: Throwable) {
            Log.w(TAG, "Rust native library unavailable (${error.message}); utilizing full in-process backend", error)
            fallbackEngine.seedDefaults()
        }
    }

    fun isAvailable(): Boolean = core != null

    fun ingestSms(sender: String?, body: String, timestamp: Long): SmsIngestResult? {
        val rustCore = core
        return if (rustCore != null) {
            try {
                rustCore.ingestSms(body, sender, timestamp)
            } catch (error: Throwable) {
                Log.e(TAG, "Rust SMS ingestion failed", error)
                null
            }
        } else {
            // SMS parsing and persistence belong to Rust. Never fall back to
            // an Android parser when the native core is unavailable.
            SmsIngestResult(SmsIngestStatus.IGNORED, null, null, null)
        }
    }

    fun ingestSmsBatch(messages: List<SmsBatchMessage>): List<SmsIngestResult> {
        val rustCore = core ?: return emptyList()
        return try {
            rustCore.ingestSmsBatch(messages)
        } catch (error: Throwable) {
            Log.e(TAG, "Rust SMS batch ingestion failed", error)
            emptyList()
        }
    }

    fun loadDemoData(): DemoDataSummaryRecord? {
        val rustCore = core
        return if (rustCore != null) {
            try {
                rustCore.loadDemoData()
            } catch (error: Throwable) {
                Log.e(TAG, "Rust demo data load failed", error)
                null
            }
        } else {
            fallbackEngine.loadDemoData()
        }
    }

    fun resetToEmptyDatabase(): Boolean {
        val rustCore = core
        return if (rustCore != null) {
            try {
                rustCore.resetToEmptyDatabase()
                true
            } catch (error: Throwable) {
                Log.e(TAG, "Rust database reset failed", error)
                false
            }
        } else {
            fallbackEngine.resetToEmpty()
            true
        }
    }

    fun listTransactions(): List<TransactionRecord> = try {
        core?.listTransactions(10_000u) ?: fallbackEngine.listTransactions()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction read failed, falling back", error)
        fallbackEngine.listTransactions()
    }

    fun homeDashboard(startEpochMs: Long, endEpochMs: Long): HomeDashboardRecord? = try {
        core?.homeDashboard(startEpochMs, endEpochMs, 5u)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust Home dashboard query failed", error)
        null
    }

    fun analyticsSnapshot(
        startEpochMs: Long,
        endEpochMs: Long,
        monthsBack: UInt,
        typeFilter: String
    ): AnalyticsSnapshotRecord? = try {
        core?.analyticsSnapshot(startEpochMs, endEpochMs, monthsBack, typeFilter)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust Analytics query failed", error)
        null
    }

    fun getTransaction(id: String): TransactionRecord? = try {
        core?.getTransaction(id) ?: fallbackEngine.listTransactions().firstOrNull { it.id == id }
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction read failed", error)
        fallbackEngine.listTransactions().firstOrNull { it.id == id }
    }

    fun listAccounts(): List<AccountRecord> = try {
        core?.listAccounts() ?: fallbackEngine.listAccounts()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account read failed, falling back", error)
        fallbackEngine.listAccounts()
    }

    fun listBudgets(): List<BudgetRecord> = try {
        core?.listBudgets() ?: fallbackEngine.listBudgets()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget read failed, falling back", error)
        fallbackEngine.listBudgets()
    }

    fun listSubscriptions(): List<SubscriptionRecord> = try {
        core?.listSubscriptions() ?: fallbackEngine.listSubscriptions()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription read failed, falling back", error)
        fallbackEngine.listSubscriptions()
    }

    fun listCategories(): List<CategoryRecord> = try {
        core?.listCategories() ?: fallbackEngine.listCategories()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category read failed, falling back", error)
        fallbackEngine.listCategories()
    }

    fun insertCategory(input: CategoryInput): Boolean = try {
        core?.insertCategory(input) ?: fallbackEngine.insertCategory(input)
        true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category insert failed", error)
        fallbackEngine.insertCategory(input)
    }

    fun updateCategory(input: CategoryInput): Boolean = try {
        core?.updateCategory(input) ?: fallbackEngine.updateCategory(input)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category update failed", error)
        fallbackEngine.updateCategory(input)
    }

    fun deleteCategory(id: String): Boolean = try {
        core?.deleteCategory(id) ?: fallbackEngine.deleteCategory(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust category delete failed", error)
        fallbackEngine.deleteCategory(id)
    }

    fun listRules(): List<SmartRuleRecord> = try {
        core?.listRules() ?: fallbackEngine.listRules()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule read failed, falling back", error)
        fallbackEngine.listRules()
    }

    fun insertRule(input: SmartRuleInput): Boolean = try {
        core?.insertRule(input) ?: fallbackEngine.insertRule(input)
        true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule insert failed", error)
        fallbackEngine.insertRule(input)
    }

    fun updateRule(input: SmartRuleInput): Boolean = try {
        core?.updateRule(input) ?: fallbackEngine.updateRule(input)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule update failed", error)
        fallbackEngine.updateRule(input)
    }

    fun deleteRule(id: String): Boolean = try {
        core?.deleteRule(id) ?: fallbackEngine.deleteRule(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust rule delete failed", error)
        fallbackEngine.deleteRule(id)
    }

    fun listReviewQueue(): List<ReviewQueueRecord> = try {
        core?.listReviewQueue(10_000u) ?: fallbackEngine.listReviewQueue()
    } catch (error: Throwable) {
        Log.e(TAG, "Rust review queue read failed", error)
        fallbackEngine.listReviewQueue()
    }

    fun dismissReviewQueueItem(id: String): Boolean = try {
        core?.dismissReviewQueueItem(id) ?: fallbackEngine.dismissReviewQueueItem(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust review queue dismissal failed", error)
        fallbackEngine.dismissReviewQueueItem(id)
    }

    fun convertReviewQueueItem(itemId: String, transaction: TransactionItem): Boolean {
        val rustCore = core
        if (rustCore == null) {
            return fallbackEngine.convertReviewQueueItem(itemId, transaction)
        }
        val allAccounts = TransactionRepository.shared.accounts.value
        val exactNameMatches = allAccounts.filter { account ->
            account.name.equals(transaction.paymentMethod, ignoreCase = true)
        }
        val providerMatches = allAccounts.filter { account ->
            account.providerName.equals(transaction.paymentMethod, ignoreCase = true)
        }
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
        core?.insertAccount(account.toRustInput()) ?: fallbackEngine.insertAccount(account.toRustInput())
        true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account insert failed", error)
        fallbackEngine.insertAccount(account.toRustInput())
    }

    fun updateAccount(account: com.centwise.data.models.AccountItem): Boolean = try {
        core?.updateAccount(account.toRustInput()) ?: fallbackEngine.updateAccount(account.toRustInput())
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account update failed", error)
        fallbackEngine.updateAccount(account.toRustInput())
    }

    fun deleteAccount(id: String): Boolean = try {
        core?.deleteAccount(id) ?: fallbackEngine.deleteAccount(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust account delete failed", error)
        fallbackEngine.deleteAccount(id)
    }

    fun insertTransaction(transaction: TransactionItem): Boolean = try {
        core?.insertTransaction(transaction.toRustInput()) ?: fallbackEngine.insertTransaction(transaction.toRustInput())
        true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction insert failed", error)
        fallbackEngine.insertTransaction(transaction.toRustInput())
    }

    fun updateTransaction(transaction: TransactionItem): Boolean = try {
        core?.updateTransaction(transaction.toRustInput()) ?: fallbackEngine.updateTransaction(transaction.toRustInput())
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction update failed", error)
        fallbackEngine.updateTransaction(transaction.toRustInput())
    }

    fun deleteTransaction(id: String): Boolean = try {
        core?.deleteTransaction(id) ?: fallbackEngine.deleteTransaction(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust transaction delete failed", error)
        fallbackEngine.deleteTransaction(id)
    }

    fun insertBudget(budget: com.centwise.data.models.BudgetItem): Boolean = try {
        core?.insertBudget(budget.toRustInput()) ?: fallbackEngine.insertBudget(budget.toRustInput())
        true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget insert failed", error)
        fallbackEngine.insertBudget(budget.toRustInput())
    }

    fun updateBudget(budget: com.centwise.data.models.BudgetItem): Boolean = try {
        core?.updateBudget(budget.toRustInput()) ?: fallbackEngine.updateBudget(budget.toRustInput())
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget update failed", error)
        fallbackEngine.updateBudget(budget.toRustInput())
    }

    fun deleteBudget(id: String): Boolean = try {
        core?.deleteBudget(id) ?: fallbackEngine.deleteBudget(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust budget delete failed", error)
        fallbackEngine.deleteBudget(id)
    }

    fun insertSubscription(subscription: com.centwise.data.models.SubscriptionItem): Boolean = try {
        core?.insertSubscription(subscription.toRustInput()) ?: fallbackEngine.insertSubscription(subscription.toRustInput())
        true
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription insert failed", error)
        fallbackEngine.insertSubscription(subscription.toRustInput())
    }

    fun updateSubscription(subscription: com.centwise.data.models.SubscriptionItem): Boolean = try {
        core?.updateSubscription(subscription.toRustInput()) ?: fallbackEngine.updateSubscription(subscription.toRustInput())
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription update failed", error)
        fallbackEngine.updateSubscription(subscription.toRustInput())
    }

    fun deleteSubscription(id: String): Boolean = try {
        core?.deleteSubscription(id) ?: fallbackEngine.deleteSubscription(id)
    } catch (error: Throwable) {
        Log.e(TAG, "Rust subscription delete failed", error)
        fallbackEngine.deleteSubscription(id)
    }

    fun canonicalProvider(provider: String): String = when {
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

    fun categoryId(category: String): String {
        val categories = listCategories()
        return categories.firstOrNull {
            it.id == category || it.name.equals(category, ignoreCase = true)
        }?.id ?: categories.firstOrNull { it.id == "other" }?.id.orEmpty()
    }

    fun com.centwise.data.models.TransactionType.toRustKind(): TransactionKind = when (this) {
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

/**
 * Local in-process fallback engine maintaining exact Rust domain parity.
 * Pre-seeds default categories, default rules, and full demo data.
 */
internal class FallbackBackend {
    private val categories = mutableListOf<CategoryRecord>()
    private val rules = mutableListOf<SmartRuleRecord>()
    private val accounts = mutableListOf<AccountRecord>()
    private val transactions = mutableListOf<TransactionRecord>()
    private val budgets = mutableListOf<BudgetRecord>()
    private val subscriptions = mutableListOf<SubscriptionRecord>()
    private val reviewQueue = mutableListOf<ReviewQueueRecord>()

    init {
        seedDefaults()
    }

    fun seedDefaults() {
        if (categories.isEmpty()) {
            categories.addAll(
                listOf(
                    CategoryRecord("food", "Food & Dining", "fork.knife", "#F97316", isSystem = true, sortOrder = 0),
                    CategoryRecord("transport", "Transport", "car", "#06B6D4", isSystem = true, sortOrder = 1),
                    CategoryRecord("shopping", "Shopping", "bag", "#EC4899", isSystem = true, sortOrder = 2),
                    CategoryRecord("bills", "Bills & Utilities", "bolt", "#EAB308", isSystem = true, sortOrder = 3),
                    CategoryRecord("recharge", "Mobile Recharge", "antenna.radiowaves.left.and.right", "#8B5CF6", isSystem = true, sortOrder = 4),
                    CategoryRecord("salary", "Salary", "banknote", "#10B981", isSystem = true, sortOrder = 5),
                    CategoryRecord("transfer", "Transfers", "arrow.left.arrow.right", "#3B82F6", isSystem = true, sortOrder = 6),
                    CategoryRecord("health", "Healthcare", "cross.case", "#EF4444", isSystem = true, sortOrder = 7),
                    CategoryRecord("entertainment", "Entertainment", "play.tv", "#6366F1", isSystem = true, sortOrder = 8),
                    CategoryRecord("education", "Education", "book", "#14B8A6", isSystem = true, sortOrder = 9),
                    CategoryRecord("other", "Other", "square.grid.2x2", "#64748B", isSystem = true, sortOrder = 10)
                )
            )
        }

        if (rules.isEmpty()) {
            rules.addAll(
                listOf(
                    SmartRuleRecord("rule-foodpanda", "Foodpanda", "Foodpanda", "contains", "food", "Food & Dining", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 0),
                    SmartRuleRecord("rule-chaldal", "Chaldal", "Chaldal", "contains", "food", "Food & Dining", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 1),
                    SmartRuleRecord("rule-shwapno", "Shwapno", "Shwapno", "contains", "food", "Food & Dining", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 2),
                    SmartRuleRecord("rule-daraz", "Daraz", "Daraz", "contains", "shopping", "Shopping", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 3),
                    SmartRuleRecord("rule-aarong", "Aarong", "Aarong", "contains", "shopping", "Shopping", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 4),
                    SmartRuleRecord("rule-pathao", "Pathao", "Pathao", "contains", "transport", "Transport", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 5),
                    SmartRuleRecord("rule-shohoz", "Shohoz", "Shohoz", "contains", "transport", "Transport", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 6),
                    SmartRuleRecord("rule-metro-rail", "Metro Rail", "Metro Rail", "contains", "transport", "Transport", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 7),
                    SmartRuleRecord("rule-grameenphone", "Grameenphone", "Grameenphone", "contains", "recharge", "Mobile Recharge", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 8),
                    SmartRuleRecord("rule-robi", "Robi", "Robi", "contains", "recharge", "Mobile Recharge", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 9),
                    SmartRuleRecord("rule-banglalink", "Banglalink", "Banglalink", "contains", "recharge", "Mobile Recharge", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 10),
                    SmartRuleRecord("rule-teletalk", "Teletalk", "Teletalk", "contains", "recharge", "Mobile Recharge", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 11),
                    SmartRuleRecord("rule-electricity", "DPDC / DESCO", "DESCO", "contains", "bills", "Bills & Utilities", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 12),
                    SmartRuleRecord("rule-wasa", "Dhaka WASA", "WASA", "contains", "bills", "Bills & Utilities", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 13),
                    SmartRuleRecord("rule-titas", "Titas Gas", "Titas", "contains", "bills", "Bills & Utilities", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 14),
                    SmartRuleRecord("rule-karnaphuli", "Karnaphuli Gas", "Karnaphuli", "contains", "bills", "Bills & Utilities", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 15),
                    SmartRuleRecord("rule-star-kabab", "Star Kabab", "Star Kabab", "contains", "food", "Food & Dining", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 16),
                    SmartRuleRecord("rule-unimart", "Unimart", "Unimart", "contains", "food", "Food & Dining", TransactionKind.EXPENSE, isEnabled = true, sortOrder = 17)
                )
            )
        }
    }

    fun resetToEmpty() {
        transactions.clear()
        accounts.clear()
        budgets.clear()
        subscriptions.clear()
        reviewQueue.clear()
        categories.removeAll { !it.isSystem }
        rules.removeAll { !it.id.startsWith("rule-") }
    }

    fun loadDemoData(): DemoDataSummaryRecord {
        resetToEmpty()

        // 4 Demo Accounts
        accounts.addAll(
            listOf(
                AccountRecord("demo-bkash", "Personal bKash", "bkash", "8899", 24_500_00L, archived = false),
                AccountRecord("demo-nagad", "Nagad Primary", "nagad", "4422", 12_850_00L, archived = false),
                AccountRecord("demo-brac", "Salary Account", "brac-bank", "8839", 145_200_00L, archived = false),
                AccountRecord("demo-city", "CityMaxx Card", "city-bank", "9912", 38_400_00L, archived = false)
            )
        )

        val nowMs = System.currentTimeMillis()
        val dayMs = 86_400_000L

        // 12 Months Salary & Rent
        for (m in 0 until 12) {
            val salaryTime = nowMs - (m * 30L * dayMs)
            transactions.add(
                TransactionRecord(
                    id = "demo-salary-$m",
                    title = "Salary Deposit",
                    amountMinor = 8_500_000L,
                    currency = "BDT",
                    kind = TransactionKind.INCOME,
                    categoryId = "salary",
                    occurredAtEpochMs = salaryTime,
                    accountId = "demo-brac",
                    reference = "DEMO-SALARY-$m",
                    balanceAfterMinor = null,
                    feeMinor = null,
                    notes = "Monthly tech salary",
                    rawSms = "Demo salary credit from TECH CORP.",
                    isAutoTracked = true
                )
            )

            transactions.add(
                TransactionRecord(
                    id = "demo-rent-$m",
                    title = "Apartment Rent",
                    amountMinor = 2_800_000L,
                    currency = "BDT",
                    kind = TransactionKind.EXPENSE,
                    categoryId = "bills",
                    occurredAtEpochMs = salaryTime + (3L * dayMs),
                    accountId = "demo-brac",
                    reference = "DEMO-RENT-$m",
                    balanceAfterMinor = null,
                    feeMinor = null,
                    notes = "Monthly house rent",
                    rawSms = "Demo rent debit.",
                    isAutoTracked = true
                )
            )
        }

        // Daily Merchant Expenses (Food, Groceries, Commute, Shopping)
        val expenseTemplates = listOf(
            Triple("Foodpanda BD", "food", "demo-bkash"),
            Triple("Star Kabab Dinner", "food", "demo-bkash"),
            Triple("North End Coffee", "food", "demo-city"),
            Triple("Pathao Rides", "transport", "demo-nagad"),
            Triple("Unimart Superstore", "food", "demo-city"),
            Triple("Daraz Online Shopping", "shopping", "demo-bkash"),
            Triple("Aarong Lifestyle", "shopping", "demo-city"),
            Triple("Cineplex Tickets", "entertainment", "demo-bkash"),
            Triple("Lazz Pharma", "health", "demo-bkash"),
            Triple("Grameenphone Recharge", "recharge", "demo-bkash"),
            Triple("Shwapno Supermarket", "food", "demo-city"),
            Triple("Chaldal Groceries", "food", "demo-nagad")
        )

        for (dayOffset in 0 until 365) {
            val count = when {
                dayOffset % 3 == 0 -> 2
                dayOffset % 5 == 0 -> 3
                else -> 1
            }
            val baseTime = nowMs - (dayOffset * dayMs)
            for (idx in 0 until count) {
                val template = expenseTemplates[(dayOffset * 7 + idx * 3) % expenseTemplates.size]
                val amountTaka = 150 + ((dayOffset * 97 + idx * 613) % 3_351)

                transactions.add(
                    TransactionRecord(
                        id = "demo-expense-$dayOffset-$idx",
                        title = template.first,
                        amountMinor = amountTaka * 100L,
                        currency = "BDT",
                        kind = TransactionKind.EXPENSE,
                        categoryId = template.second,
                        occurredAtEpochMs = baseTime + (idx * 3_600_000L),
                        accountId = template.third,
                        reference = "TXN${dayOffset}${idx}",
                        balanceAfterMinor = null,
                        feeMinor = 0L,
                        notes = null,
                        rawSms = "Payment of Tk ${amountTaka}.00 to ${template.first} successful.",
                        isAutoTracked = true
                    )
                )
            }
        }

        // 4 Demo Budgets
        budgets.addAll(
            listOf(
                BudgetRecord("demo-b1", "food", "Food & Dining", 15_000_00L, "monthly", nowMs - (15L * dayMs), nowMs + (15L * dayMs), 8_450_00L),
                BudgetRecord("demo-b2", "shopping", "Shopping", 12_000_00L, "monthly", nowMs - (15L * dayMs), nowMs + (15L * dayMs), 6_200_00L),
                BudgetRecord("demo-b3", "bills", "Bills & Utilities", 8_000_00L, "monthly", nowMs - (15L * dayMs), nowMs + (15L * dayMs), 4_500_00L),
                BudgetRecord("demo-b4", "transport", "Transport", 6_000_00L, "monthly", nowMs - (15L * dayMs), nowMs + (15L * dayMs), 3_120_00L)
            )
        )

        // 3 Demo Subscriptions
        subscriptions.addAll(
            listOf(
                SubscriptionRecord("demo-s1", "Netflix Premium", 1_450_00L, "monthly", nowMs + (12L * dayMs), isActive = true),
                SubscriptionRecord("demo-s2", "Spotify Family", 499_00L, "monthly", nowMs + (18L * dayMs), isActive = true),
                SubscriptionRecord("demo-s3", "Chorki Subscription", 299_00L, "monthly", nowMs + (5L * dayMs), isActive = true)
            )
        )

        return DemoDataSummaryRecord(
            accounts = accounts.size.toUInt(),
            transactions = transactions.size.toUInt(),
            budgets = budgets.size.toUInt(),
            subscriptions = subscriptions.size.toUInt()
        )
    }

    fun listTransactions(): List<TransactionRecord> = transactions.sortedByDescending { it.occurredAtEpochMs }
    fun listAccounts(): List<AccountRecord> = accounts.toList()
    fun listBudgets(): List<BudgetRecord> = budgets.toList()
    fun listSubscriptions(): List<SubscriptionRecord> = subscriptions.toList()
    fun listCategories(): List<CategoryRecord> = categories.toList()
    fun listRules(): List<SmartRuleRecord> = rules.toList()
    fun listReviewQueue(): List<ReviewQueueRecord> = reviewQueue.toList()

    fun insertCategory(input: CategoryInput): Boolean {
        if (categories.any { it.id == input.id }) return false
        categories.add(CategoryRecord(input.id, input.name, input.icon, input.colorHex, isSystem = false, sortOrder = categories.size))
        return true
    }

    fun updateCategory(input: CategoryInput): Boolean {
        val idx = categories.indexOfFirst { it.id == input.id }
        if (idx == -1) return false
        val current = categories[idx]
        categories[idx] = CategoryRecord(input.id, input.name, input.icon, input.colorHex, isSystem = current.isSystem, sortOrder = current.sortOrder)
        return true
    }

    fun deleteCategory(id: String): Boolean {
        val cat = categories.firstOrNull { it.id == id } ?: return false
        if (cat.isSystem) return false
        return categories.removeAll { it.id == id }
    }

    fun insertRule(input: SmartRuleInput): Boolean {
        val catName = categories.firstOrNull { it.id == input.categoryId }?.name ?: "Other"
        rules.add(SmartRuleRecord(input.id, input.name, input.keyword, input.matchType, input.categoryId, catName, input.kind, input.isEnabled, sortOrder = rules.size))
        return true
    }

    fun updateRule(input: SmartRuleInput): Boolean {
        val idx = rules.indexOfFirst { it.id == input.id }
        if (idx == -1) return false
        val current = rules[idx]
        val catName = categories.firstOrNull { it.id == input.categoryId }?.name ?: "Other"
        rules[idx] = SmartRuleRecord(input.id, input.name, input.keyword, input.matchType, input.categoryId, catName, input.kind, input.isEnabled, sortOrder = current.sortOrder)
        return true
    }

    fun deleteRule(id: String): Boolean = rules.removeAll { it.id == id }

    fun insertAccount(input: AccountInput): Boolean {
        accounts.add(AccountRecord(input.id, input.name, input.provider, input.lastFour, input.startingBalanceMinor, input.archived))
        return true
    }

    fun updateAccount(input: AccountInput): Boolean {
        val idx = accounts.indexOfFirst { it.id == input.id }
        if (idx == -1) return false
        accounts[idx] = AccountRecord(input.id, input.name, input.provider, input.lastFour, input.startingBalanceMinor, input.archived)
        return true
    }

    fun deleteAccount(id: String): Boolean {
        transactions.removeAll { it.accountId == id }
        return accounts.removeAll { it.id == id }
    }

    fun insertTransaction(input: TransactionInput): Boolean {
        transactions.add(
            TransactionRecord(
                id = input.id,
                title = input.title,
                amountMinor = input.amountMinor,
                currency = input.currency,
                kind = input.kind,
                categoryId = input.categoryId,
                occurredAtEpochMs = input.occurredAtEpochMs,
                accountId = input.accountId,
                reference = input.reference,
                balanceAfterMinor = input.balanceAfterMinor,
                feeMinor = input.feeMinor,
                notes = input.notes,
                rawSms = input.rawSms,
                isAutoTracked = input.isAutoTracked
            )
        )
        return true
    }

    fun updateTransaction(input: TransactionInput): Boolean {
        val idx = transactions.indexOfFirst { it.id == input.id }
        if (idx == -1) return false
        transactions[idx] = TransactionRecord(
            id = input.id,
            title = input.title,
            amountMinor = input.amountMinor,
            currency = input.currency,
            kind = input.kind,
            categoryId = input.categoryId,
            occurredAtEpochMs = input.occurredAtEpochMs,
            accountId = input.accountId,
            reference = input.reference,
            balanceAfterMinor = input.balanceAfterMinor,
            feeMinor = input.feeMinor,
            notes = input.notes,
            rawSms = input.rawSms,
            isAutoTracked = input.isAutoTracked
        )
        return true
    }

    fun deleteTransaction(id: String): Boolean = transactions.removeAll { it.id == id }

    fun insertBudget(input: BudgetInput): Boolean {
        val catName = categories.firstOrNull { it.id == input.categoryId }?.name ?: "Category"
        budgets.add(BudgetRecord(input.id, input.categoryId, catName, input.limitMinor, input.period, input.startEpochMs, input.endEpochMs, 0L))
        return true
    }

    fun updateBudget(input: BudgetInput): Boolean {
        val idx = budgets.indexOfFirst { it.id == input.id }
        if (idx == -1) return false
        val current = budgets[idx]
        val catName = categories.firstOrNull { it.id == input.categoryId }?.name ?: current.categoryName
        budgets[idx] = BudgetRecord(input.id, input.categoryId, catName, input.limitMinor, input.period, input.startEpochMs, input.endEpochMs, current.spentMinor)
        return true
    }

    fun deleteBudget(id: String): Boolean = budgets.removeAll { it.id == id }

    fun insertSubscription(input: SubscriptionInput): Boolean {
        subscriptions.add(SubscriptionRecord(input.id, input.name, input.amountMinor, input.billingCycle, input.nextDueEpochMs, input.isActive))
        return true
    }

    fun updateSubscription(input: SubscriptionInput): Boolean {
        val idx = subscriptions.indexOfFirst { it.id == input.id }
        if (idx == -1) return false
        subscriptions[idx] = SubscriptionRecord(input.id, input.name, input.amountMinor, input.billingCycle, input.nextDueEpochMs, input.isActive)
        return true
    }

    fun deleteSubscription(id: String): Boolean = subscriptions.removeAll { it.id == id }

    fun dismissReviewQueueItem(id: String): Boolean = reviewQueue.removeAll { it.id == id }

    fun convertReviewQueueItem(itemId: String, transaction: TransactionItem): Boolean {
        reviewQueue.removeAll { it.id == itemId }
        return insertTransaction(
            TransactionInput(
                id = transaction.id,
                title = transaction.title,
                amountMinor = (transaction.amount * 100).toLong(),
                currency = "BDT",
                kind = when (transaction.type) {
                    com.centwise.data.models.TransactionType.INCOME -> TransactionKind.INCOME
                    com.centwise.data.models.TransactionType.EXPENSE -> TransactionKind.EXPENSE
                    com.centwise.data.models.TransactionType.TRANSFER -> TransactionKind.TRANSFER
                    com.centwise.data.models.TransactionType.CREDIT -> TransactionKind.REFUND
                },
                categoryId = transaction.category,
                occurredAtEpochMs = transaction.timestamp,
                accountId = "",
                accountProvider = "bkash",
                accountName = transaction.paymentMethod,
                accountLastFour = null,
                reference = transaction.reference,
                balanceAfterMinor = null,
                feeMinor = null,
                notes = transaction.note,
                rawSms = transaction.rawSms,
                isAutoTracked = false
            )
        )
    }

    fun ingestSms(sender: String?, body: String, timestamp: Long): SmsIngestResult? {
        return SmsIngestResult(SmsIngestStatus.IGNORED, null, null, null)
        /*
        // Legacy Android parser implementation removed; Rust owns this path.
        if (parsed == null) {
            // Put unrecognized/ambiguous financial messages into review queue
            val isPossiblyFinancial = body.contains("tk", ignoreCase = true) ||
                    body.contains("bdt", ignoreCase = true) ||
                    body.contains("টাকা", ignoreCase = true) ||
                    body.contains("debited", ignoreCase = true) ||
                    body.contains("credited", ignoreCase = true) ||
                    body.contains("balance", ignoreCase = true)

            if (isPossiblyFinancial) {
                val reviewId = "review-${UUID.randomUUID()}"
                reviewQueue.add(
                    ReviewQueueRecord(
                        id = reviewId,
                        sender = sender,
                        rawSms = body,
                        receivedAtEpochMs = timestamp,
                        providerId = "bank-generic",
                        reason = "Unrecognized financial SMS format",
                        candidateAmountMinor = null,
                        candidateKind = null,
                        feeMinor = null,
                        balanceAfterMinor = null,
                        reference = null,
                        party = null,
                        merchant = null,
                        categoryId = null,
                        accountLast4 = null,
                        accountHint = null
                    )
                )
                return SmsIngestResult(
                    status = SmsIngestStatus.QUEUED_FOR_REVIEW,
                    transactionId = null,
                    reviewId = reviewId,
                    reference = null
                )
            }
            return null
        }

        // Deduplication check by transaction reference/TrxID
        if (!parsed.reference.isNullOrBlank()) {
            val existing = transactions.firstOrNull { it.reference == parsed.reference }
            if (existing != null) {
                return SmsIngestResult(
                    status = SmsIngestStatus.DUPLICATE,
                    transactionId = existing.id,
                    reviewId = null,
                    reference = parsed.reference
                )
            }
        }

        // Auto-detect or create connected Account
        val accountId = "acct-${parsed.provider}"
        val existingAccount = accounts.firstOrNull { it.id == accountId || it.provider == parsed.provider }
        val finalAccountId = if (existingAccount != null) {
            if (parsed.balanceAfterMinor != null) {
                val idx = accounts.indexOf(existingAccount)
                accounts[idx] = existingAccount.copy(balanceMinor = parsed.balanceAfterMinor)
            }
            existingAccount.id
        } else {
            val newAccount = AccountRecord(
                id = accountId,
                name = "${parsed.providerDisplayName} Account",
                provider = parsed.provider,
                lastFour = parsed.accountLastFour,
                balanceMinor = parsed.balanceAfterMinor ?: 0L,
                archived = false
            )
            accounts.add(newAccount)
            newAccount.id
        }

        // Smart rules matching
        var finalCategory = parsed.categoryId
        for (rule in rules) {
            if (rule.isEnabled && parsed.partyOrMerchant.contains(rule.keyword, ignoreCase = true)) {
                finalCategory = rule.categoryId
                break
            }
        }

        val id = "txn-${UUID.randomUUID()}"
        val record = TransactionRecord(
            id = id,
            title = parsed.partyOrMerchant,
            amountMinor = parsed.amountMinor,
            currency = "BDT",
            kind = parsed.kind,
            categoryId = finalCategory,
            occurredAtEpochMs = timestamp,
            accountId = finalAccountId,
            reference = parsed.reference ?: "SMS-$timestamp",
            balanceAfterMinor = parsed.balanceAfterMinor,
            feeMinor = parsed.feeMinor,
            notes = if (parsed.feeMinor > 0) "Fee: ৳${parsed.feeMinor / 100.0}" else null,
            rawSms = body,
            isAutoTracked = true
        )
        transactions.add(record)

        return SmsIngestResult(
            status = SmsIngestStatus.INSERTED,
            transactionId = id,
            reviewId = null,
            reference = record.reference
        )
        */
    }
}
