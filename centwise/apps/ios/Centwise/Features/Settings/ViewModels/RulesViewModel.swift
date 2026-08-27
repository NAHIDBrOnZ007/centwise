import Foundation
import Combine

/// Rust-backed Smart Rules view model. Published values are a UI cache only.
public final class RulesViewModel: ObservableObject {
    @Published public private(set) var rules: [SmartRule] = []

    public init() {
        loadRules()
    }

    public func loadRules() {
        rules = CentwiseRustBackend.listRules().map { record in
            SmartRule(
                id: record.id,
                name: record.name,
                keyword: record.keyword,
                matchType: matchType(record.matchType),
                category: TransactionRepository.shared.category(id: record.categoryId),
                transactionType: transactionType(record.kind),
                isEnabled: record.isEnabled
            )
        }
    }

    public func addRule(_ rule: SmartRule) {
        guard CentwiseRustBackend.insertRule(rule) else { return }
        loadRules()
    }

    public func updateRule(_ rule: SmartRule) {
        guard CentwiseRustBackend.updateRule(rule) else { return }
        loadRules()
    }

    public func deleteRule(id: String) {
        guard CentwiseRustBackend.deleteRule(id: id) else { return }
        loadRules()
    }

    public func toggleRule(id: String, isEnabled: Bool) {
        guard var rule = rules.first(where: { $0.id == id }) else { return }
        rule.isEnabled = isEnabled
        updateRule(rule)
    }

    private func matchType(_ value: String) -> RuleMatchType {
        switch value {
        case "starts_with": return .startsWith
        case "exactly_matches": return .equals
        default: return .contains
        }
    }

    private func transactionType(_ kind: TransactionKind) -> TransactionType {
        switch kind {
        case .expense: return .expense
        case .income: return .income
        case .transfer: return .transfer
        case .refund: return .refund
        }
    }
}
