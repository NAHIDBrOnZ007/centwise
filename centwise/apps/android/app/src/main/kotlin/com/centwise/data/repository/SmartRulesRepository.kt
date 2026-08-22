package com.centwise.data.repository

import com.centwise.data.models.RuleMatchType
import com.centwise.data.models.SmartRule
import com.centwise.data.models.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing user-defined Smart Rules.
 * Smart Rules allow automatic categorization of transactions based on merchant keywords.
 */
class SmartRulesRepository private constructor() {

    private val _rules = MutableStateFlow<List<SmartRule>>(
        listOf(
            SmartRule(
                name = "Foodpanda is Food",
                keyword = "Foodpanda",
                matchType = RuleMatchType.CONTAINS,
                categoryName = "Food & Dining",
                transactionType = TransactionType.EXPENSE,
                isEnabled = true
            ),
            SmartRule(
                name = "Pathao is Transport",
                keyword = "Pathao",
                matchType = RuleMatchType.CONTAINS,
                categoryName = "Transport",
                transactionType = TransactionType.EXPENSE,
                isEnabled = true
            ),
            SmartRule(
                name = "Daraz is Shopping",
                keyword = "Daraz",
                matchType = RuleMatchType.CONTAINS,
                categoryName = "Shopping",
                transactionType = TransactionType.EXPENSE,
                isEnabled = true
            ),
            SmartRule(
                name = "Chaldal is Groceries",
                keyword = "Chaldal",
                matchType = RuleMatchType.CONTAINS,
                categoryName = "Groceries",
                transactionType = TransactionType.EXPENSE,
                isEnabled = true
            ),
            SmartRule(
                name = "Skitto is Mobile Recharge",
                keyword = "Skitto",
                matchType = RuleMatchType.CONTAINS,
                categoryName = "Mobile Recharge",
                transactionType = TransactionType.EXPENSE,
                isEnabled = true
            )
        )
    )

    val rules: StateFlow<List<SmartRule>> = _rules.asStateFlow()

    fun addRule(rule: SmartRule) {
        _rules.value = listOf(rule) + _rules.value
    }

    fun updateRule(rule: SmartRule) {
        _rules.value = _rules.value.map { if (it.id == rule.id) rule else it }
    }

    fun deleteRule(id: String) {
        _rules.value = _rules.value.filter { it.id != id }
    }

    fun toggleRule(id: String, isEnabled: Boolean) {
        _rules.value = _rules.value.map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
    }

    /**
     * Checks all enabled Smart Rules against the merchant/party string.
     * Returns the first matching rule, or null if no rule applies.
     */
    fun applyRules(merchantOrParty: String): SmartRule? {
        val enabledRules = _rules.value.filter { it.isEnabled }
        for (rule in enabledRules) {
            if (rule.matchType.matches(merchantOrParty, rule.keyword)) {
                return rule
            }
        }
        return null
    }

    companion object {
        val shared = SmartRulesRepository()
    }
}
