package com.centwise.data.repository

import com.centwise.core.backend.CentwiseRustBackend
import com.centwise.core.uniffi.TransactionKind
import com.centwise.data.models.RuleMatchType
import com.centwise.data.models.SmartRule
import com.centwise.data.models.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Rust-backed Smart Rules repository. The flow is a UI cache, not storage. */
class SmartRulesRepository private constructor() {
    private val _rules = MutableStateFlow<List<SmartRule>>(emptyList())
    val rules: StateFlow<List<SmartRule>> = _rules.asStateFlow()

    fun refresh() {
        _rules.value = CentwiseRustBackend.listRules().map { rule ->
            SmartRule(
                id = rule.id,
                name = rule.name,
                keyword = rule.keyword,
                matchType = when (rule.matchType) {
                    "starts_with" -> RuleMatchType.STARTS_WITH
                    "exactly_matches" -> RuleMatchType.EXACTLY_MATCHES
                    else -> RuleMatchType.CONTAINS
                },
                categoryName = rule.categoryName,
                transactionType = when (rule.kind) {
                    TransactionKind.INCOME -> TransactionType.INCOME
                    TransactionKind.TRANSFER -> TransactionType.TRANSFER
                    TransactionKind.REFUND -> TransactionType.CREDIT
                    TransactionKind.EXPENSE -> TransactionType.EXPENSE
                },
                isEnabled = rule.isEnabled
            )
        }
    }

    fun addRule(rule: SmartRule): Boolean =
        CentwiseRustBackend.insertRule(CentwiseRustBackend.toSmartRuleInput(rule)).also {
            if (it) refresh()
        }

    fun updateRule(rule: SmartRule): Boolean =
        CentwiseRustBackend.updateRule(CentwiseRustBackend.toSmartRuleInput(rule)).also {
            if (it) refresh()
        }

    fun deleteRule(id: String): Boolean =
        CentwiseRustBackend.deleteRule(id).also { if (it) refresh() }

    fun toggleRule(id: String, isEnabled: Boolean): Boolean {
        val current = _rules.value.firstOrNull { it.id == id } ?: return false
        return updateRule(current.copy(isEnabled = isEnabled))
    }

    companion object {
        val shared = SmartRulesRepository()
    }
}
