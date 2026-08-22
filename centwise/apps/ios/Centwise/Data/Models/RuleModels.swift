import Foundation
import SwiftUI

// MARK: - Match Condition
public enum RuleMatchType: String, CaseIterable, Identifiable {
    case contains = "Contains"
    case startsWith = "Starts With"
    case equals = "Exactly Matches"

    public var id: String { rawValue }

    public func matches(merchant: String, keyword: String) -> Bool {
        let m = merchant.lowercased()
        let k = keyword.trimmingCharacters(in: .whitespaces).lowercased()
        guard !k.isEmpty else { return false }
        switch self {
        case .contains: return m.contains(k)
        case .startsWith: return m.hasPrefix(k)
        case .equals: return m == k
        }
    }
}

// MARK: - Smart Rule
public struct SmartRule: Identifiable, Hashable {
    public let id: String
    public var name: String
    public var keyword: String
    public var matchType: RuleMatchType
    public var category: TransactionCategory
    public var transactionType: TransactionType
    public var isEnabled: Bool

    public init(
        id: String = UUID().uuidString,
        name: String,
        keyword: String,
        matchType: RuleMatchType = .contains,
        category: TransactionCategory,
        transactionType: TransactionType = .expense,
        isEnabled: Bool = true
    ) {
        self.id = id
        self.name = name
        self.keyword = keyword
        self.matchType = matchType
        self.category = category
        self.transactionType = transactionType
        self.isEnabled = isEnabled
    }

    public var summary: String {
        "If merchant \(matchType.rawValue.lowercased()) \"\(keyword)\" → \(category.name)"
    }
}
