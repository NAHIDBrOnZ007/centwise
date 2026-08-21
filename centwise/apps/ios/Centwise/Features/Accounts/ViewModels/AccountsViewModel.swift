import Foundation
import Combine
import SwiftUI

public final class AccountsViewModel: ObservableObject {
    @Published public var accounts: [FinancialAccount] = []
    @Published public var totalBalance: Double = 0.0

    private var cancellables = Set<AnyCancellable>()
    private let repository: FakeTransactionRepository

    public init(repository: FakeTransactionRepository = .shared) {
        self.repository = repository
        bindRepository()
    }

    private func bindRepository() {
        repository.$accounts
            .receive(on: DispatchQueue.main)
            .sink { [weak self] items in
                guard let self = self else { return }
                self.accounts = items
                self.totalBalance = items.filter { !$0.isArchived }.reduce(0) { $0 + $1.currentBalance }
            }
            .store(in: &cancellables)
    }

    public func addAccount(_ account: FinancialAccount) {
        repository.addAccount(account)
    }
}
