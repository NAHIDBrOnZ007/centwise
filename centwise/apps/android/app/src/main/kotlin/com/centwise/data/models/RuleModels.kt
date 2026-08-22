package com.centwise.data.models

import androidx.compose.ui.graphics.Color

// MARK: - Category Option (for category management UI)

data class CategoryOption(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: Long
) {
    val color: Color get() = Color(colorHex)

    companion object {
        val defaults = listOf(
            CategoryOption("food", "Food & Dining", "restaurant", 0xFFF97316),
            CategoryOption("transport", "Transport", "directions_car", 0xFF06B6D4),
            CategoryOption("groceries", "Groceries", "shopping_cart", 0xFF22C55E),
            CategoryOption("shopping", "Shopping", "shopping_bag", 0xFFEC4899),
            CategoryOption("bills", "Bills & Utilities", "bolt", 0xFFEAB308),
            CategoryOption("recharge", "Mobile Recharge", "signal_cellular_alt", 0xFF8B5CF6),
            CategoryOption("salary", "Salary", "payments", 0xFF10B981),
            CategoryOption("health", "Healthcare", "medical_services", 0xFFEF4444),
            CategoryOption("entertainment", "Entertainment", "live_tv", 0xFF6366F1),
            CategoryOption("education", "Education", "school", 0xFF14B8A6),
            CategoryOption("other", "Other", "category", 0xFF64748B)
        )
    }
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
