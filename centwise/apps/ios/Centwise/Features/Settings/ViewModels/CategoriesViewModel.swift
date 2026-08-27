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
        TransactionRepository.shared.addCategory(category)
    }

    public func updateCategory(_ category: TransactionCategory) {
        guard !isSystemCategory(category) else { return }
        TransactionRepository.shared.updateCategory(category)
    }

    public func deleteCategory(id: String) {
        guard let category = categories.first(where: { $0.id == id }),
              !isSystemCategory(category) else { return }
        TransactionRepository.shared.deleteCategory(id: id)
    }

    public func isSystemCategory(_ category: TransactionCategory) -> Bool {
        category.isSystem
    }
}
