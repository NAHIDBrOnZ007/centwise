import Foundation
import Combine

public final class CategoriesViewModel: ObservableObject {
    @Published public private(set) var categories: [TransactionCategory] = []

    public init() {
        loadCategories()
    }

    public var systemCategories: [TransactionCategory] {
        categories.filter { category in
            TransactionCategory.defaultCategories.contains { $0.id == category.id }
        }
    }

    public var customCategories: [TransactionCategory] {
        categories.filter { category in
            !TransactionCategory.defaultCategories.contains { $0.id == category.id }
        }
    }

    public func loadCategories() {
        if categories.isEmpty {
            categories = TransactionCategory.defaultCategories
        }
    }

    public func addCategory(_ category: TransactionCategory) {
        categories.append(category)
    }

    public func updateCategory(_ category: TransactionCategory) {
        guard let index = categories.firstIndex(where: { $0.id == category.id }),
              !isSystemCategory(category) else { return }
        categories[index] = category
    }

    public func deleteCategory(id: String) {
        guard let category = categories.first(where: { $0.id == id }),
              !isSystemCategory(category) else { return }
        categories.removeAll { $0.id == id }
    }

    public func isSystemCategory(_ category: TransactionCategory) -> Bool {
        TransactionCategory.defaultCategories.contains { $0.id == category.id }
    }
}
