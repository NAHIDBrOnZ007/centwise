import Foundation
import Combine

public final class CategoriesViewModel: ObservableObject {
    @Published public private(set) var categories: [TransactionCategory] = []
    private var cancellables = Set<AnyCancellable>()

    public init() {
        loadCategories()
        TransactionRepository.shared.$categories
            .receive(on: DispatchQueue.main)
            .sink { [weak self] categories in
                self?.categories = categories
            }
            .store(in: &cancellables)
    }

    public var systemCategories: [TransactionCategory] {
        categories.filter { category in
            category.isSystem
        }
    }

    public var customCategories: [TransactionCategory] {
        categories.filter { category in
            !category.isSystem
        }
    }

    public func loadCategories() {
        categories = TransactionRepository.shared.categories
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
        category.isSystem
    }
}
