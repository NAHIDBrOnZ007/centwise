package com.centwise.data.models

import androidx.compose.ui.graphics.Color

// MARK: - Category Option (for category management UI)

data class CategoryOption(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: Long,
    val isSystem: Boolean = false,
) {
    val color: Color get() = Color(colorHex)
}

// MARK: - Match Condition

enum class RuleMatchType(val displayName: String) {
    CONTAINS("Contains"),
    STARTS_WITH("Starts With"),
    EXACTLY_MATCHES("Exactly Matches");

    fun matches(merchant: String, keyword: String): Boolean {
        val m = merchant.lowercase()
        val k = keyword.trim().lowercase()
        if (k.isEmpty()) return false
        return when (this) {
            CONTAINS -> m.contains(k)
            STARTS_WITH -> m.startsWith(k)
            EXACTLY_MATCHES -> m == k
        }
    }
}

// MARK: - Smart Rule

data class SmartRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val keyword: String,
    val matchType: RuleMatchType = RuleMatchType.CONTAINS,
    val categoryName: String,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isEnabled: Boolean = true
) {
    val summary: String
        get() = "If merchant ${matchType.displayName.lowercase()} \"$keyword\" → $categoryName"
}
