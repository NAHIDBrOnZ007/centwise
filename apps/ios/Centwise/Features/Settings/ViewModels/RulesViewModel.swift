import Foundation
import Combine

/// Rust-backed Smart Rules view model. Published values are a UI cache only.
public final class RulesViewModel: ObservableObject {
    @Published public private(set) var rules: [SmartRule] = []
    private var observers: [NSObjectProtocol] = []
    private let loadQueue = DispatchQueue(label: "com.centwise.rules-load", qos: .userInitiated)
    private var isLoading = false
    private var refreshPending = false

    public init() {
        loadRules()
        setupObservers()
    }

    private func setupObservers() {
        observers.append(
            NotificationCenter.default.addObserver(
                forName: .centwiseTransactionsUpdated,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.loadRules()
            }
        )
    }

    deinit {
        observers.forEach { NotificationCenter.default.removeObserver($0) }
    }

    public func loadRules() {
        let categories = Dictionary(
            uniqueKeysWithValues: TransactionRepository.shared.categories.map { ($0.id, $0) }
        )
        let defaults = Self.builtInDefaultRules

        loadQueue.async { [weak self] in
            guard let self else { return }
            guard !self.isLoading else {
                self.refreshPending = true
                return
            }
            self.isLoading = true
            var records = CentwiseRustBackend.listRules()
            if records.isEmpty {
                for rule in defaults {
                    _ = CentwiseRustBackend.insertRule(rule)
                }
                records = CentwiseRustBackend.listRules()
            }

            let loaded = records.map { record in
                SmartRule(
                    id: record.id,
                    name: record.name,
                    keyword: record.keyword,
                    matchType: self.matchType(record.matchType),
                    category: categories[record.categoryId] ?? TransactionCategory(
                        id: record.categoryId,
                        name: "Other",
                        icon: "square.grid.2x2",
                        colorHex: "#64748B"
                    ),
                    transactionType: self.transactionType(record.kind),
                    isEnabled: record.isEnabled
                )
            }
            DispatchQueue.main.async {
                self.rules = loaded
            }
            self.isLoading = false
            if self.refreshPending {
                self.refreshPending = false
                self.loadRules()
            }
        }
    }

    public func restoreDefaultRules() {
        for rule in Self.builtInDefaultRules {
            _ = CentwiseRustBackend.insertRule(rule)
        }
        loadRules()
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

    public var groupedRules: [(category: TransactionCategory, rules: [SmartRule])] {
        let categories = TransactionRepository.shared.categories
        var result: [(category: TransactionCategory, rules: [SmartRule])] = []
        for cat in categories {
            let catRules = rules.filter { $0.category.id == cat.id }
            if !catRules.isEmpty {
                result.append((category: cat, rules: catRules))
            }
        }
        let knownCatIds = Set(categories.map { $0.id })
        let otherRules = rules.filter { !knownCatIds.contains($0.category.id) }
        if !otherRules.isEmpty {
            let otherCat = TransactionCategory(id: "other", name: "Other", icon: "square.grid.2x2", colorHex: "#64748B")
            result.append((category: otherCat, rules: otherRules))
        }
        return result
    }

    public static let builtInDefaultRules: [SmartRule] = [
        SmartRule(id: "rule-foodpanda", name: "Foodpanda", keyword: "Foodpanda", matchType: .contains, category: TransactionRepository.shared.category(id: "food"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-chaldal", name: "Chaldal", keyword: "Chaldal", matchType: .contains, category: TransactionRepository.shared.category(id: "food"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-shwapno", name: "Shwapno", keyword: "Shwapno", matchType: .contains, category: TransactionRepository.shared.category(id: "food"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-daraz", name: "Daraz", keyword: "Daraz", matchType: .contains, category: TransactionRepository.shared.category(id: "shopping"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-aarong", name: "Aarong", keyword: "Aarong", matchType: .contains, category: TransactionRepository.shared.category(id: "shopping"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-pathao", name: "Pathao", keyword: "Pathao", matchType: .contains, category: TransactionRepository.shared.category(id: "transport"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-shohoz", name: "Shohoz", keyword: "Shohoz", matchType: .contains, category: TransactionRepository.shared.category(id: "transport"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-metro-rail", name: "Metro Rail", keyword: "Metro Rail", matchType: .contains, category: TransactionRepository.shared.category(id: "transport"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-grameenphone", name: "Grameenphone", keyword: "Grameenphone", matchType: .contains, category: TransactionRepository.shared.category(id: "recharge"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-robi", name: "Robi", keyword: "Robi", matchType: .contains, category: TransactionRepository.shared.category(id: "recharge"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-banglalink", name: "Banglalink", keyword: "Banglalink", matchType: .contains, category: TransactionRepository.shared.category(id: "recharge"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-skitto", name: "Skitto", keyword: "Skitto", matchType: .contains, category: TransactionRepository.shared.category(id: "recharge"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-dpdc", name: "DPDC", keyword: "DPDC", matchType: .contains, category: TransactionRepository.shared.category(id: "bills"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-desco", name: "DESCO", keyword: "DESCO", matchType: .contains, category: TransactionRepository.shared.category(id: "bills"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-wasa", name: "Dhaka WASA", keyword: "WASA", matchType: .contains, category: TransactionRepository.shared.category(id: "bills"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-lazz-pharma", name: "Lazz Pharma", keyword: "Lazz Pharma", matchType: .contains, category: TransactionRepository.shared.category(id: "health"), transactionType: .expense, isEnabled: true),
        SmartRule(id: "rule-10ms", name: "10 Minute School", keyword: "10 Minute School", matchType: .contains, category: TransactionRepository.shared.category(id: "education"), transactionType: .expense, isEnabled: true),
    ]

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
