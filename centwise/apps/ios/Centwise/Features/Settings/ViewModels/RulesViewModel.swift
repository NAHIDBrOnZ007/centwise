import Foundation
import Combine

public final class RulesViewModel: ObservableObject {
    @Published public private(set) var rules: [SmartRule] = []

    public init() {
        loadRules()
    }

    public func loadRules() {
        if rules.isEmpty {
            rules = [
                SmartRule(
                    name: "Foodpanda is Food",
                    keyword: "Foodpanda",
                    category: TransactionRepository.shared.category(id: "food")
                ),
                SmartRule(
                    name: "Pathao is Transport",
                    keyword: "Pathao",
                    category: TransactionRepository.shared.category(id: "transport")
                ),
                SmartRule(
                    name: "Daraz is Shopping",
                    keyword: "Daraz",
                    category: TransactionRepository.shared.category(id: "shopping"),
                    isEnabled: false
                )
            ]
        }
    }

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

    public func firstMatch(merchant: String) -> SmartRule? {
        rules.first { $0.isEnabled && $0.matchType.matches(merchant: merchant, keyword: $0.keyword) }
    }
}
