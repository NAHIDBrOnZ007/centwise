import Foundation
import Combine

public final class SmartRulesRepository: ObservableObject {
    public static let shared = SmartRulesRepository()

    @Published public private(set) var rules: [SmartRule] = [
        SmartRule(
            name: "Foodpanda is Food",
            keyword: "Foodpanda",
            matchType: .contains,
            category: TransactionRepository.shared.category(id: "food"),
            transactionType: .expense,
            isEnabled: true
        ),
        SmartRule(
            name: "Pathao is Transport",
            keyword: "Pathao",
            matchType: .contains,
            category: TransactionRepository.shared.category(id: "transport"),
            transactionType: .expense,
            isEnabled: true
        ),
        SmartRule(
            name: "Daraz is Shopping",
            keyword: "Daraz",
            matchType: .contains,
            category: TransactionRepository.shared.category(id: "shopping"),
            transactionType: .expense,
            isEnabled: true
        ),
        SmartRule(
            name: "Chaldal is Groceries",
            keyword: "Chaldal",
            matchType: .contains,
            category: TransactionRepository.shared.category(id: "food"),
            transactionType: .expense,
            isEnabled: true
        )
    ]

    public init() {}

    public func addRule(_ rule: SmartRule) {
        rules.insert(rule, at: 0)
    }

    public func updateRule(_ rule: SmartRule) {
        if let index = rules.firstIndex(where: { $0.id == rule.id }) {
            rules[index] = rule
        }
    }

    public func deleteRule(id: String) {
        rules.removeAll { $0.id == id }
    }

    public func toggleRule(id: String, isEnabled: Bool) {
        if let index = rules.firstIndex(where: { $0.id == id }) {
            rules[index].isEnabled = isEnabled
        }
    }

    public func applyRules(merchantOrParty: String) -> SmartRule? {
        let enabled = rules.filter { $0.isEnabled }
        for rule in enabled {
            if rule.matchType.matches(merchant: merchantOrParty, keyword: rule.keyword) {
                return rule
            }
        }
        return nil
    }
}
