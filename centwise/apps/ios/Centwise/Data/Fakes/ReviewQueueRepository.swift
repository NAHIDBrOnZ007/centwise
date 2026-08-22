import Foundation
import Combine

public struct ReviewQueueItem: Identifiable, Equatable {
    public let id: String
    public let sender: String
    public let rawSms: String
    public let timestamp: Date
    public let candidateAmount: Double?
    public let candidateParty: String?
    public let candidateType: TransactionType?
    public let reason: String

    public init(
        id: String = UUID().uuidString,
        sender: String,
        rawSms: String,
        timestamp: Date = Date(),
        candidateAmount: Double? = nil,
        candidateParty: String? = nil,
        candidateType: TransactionType? = nil,
        reason: String = "Format needs confirmation"
    ) {
        self.id = id
        self.sender = sender
        self.rawSms = rawSms
        self.timestamp = timestamp
        self.candidateAmount = candidateAmount
        self.candidateParty = candidateParty
        self.candidateType = candidateType
        self.reason = reason
    }
}

public final class ReviewQueueRepository: ObservableObject {
    public static let shared = ReviewQueueRepository()

    @Published public private(set) var items: [ReviewQueueItem] = []

    public init() {}

    public func addItem(_ item: ReviewQueueItem) {
        if !items.contains(where: { $0.rawSms == item.rawSms }) {
            items.insert(item, at: 0)
        }
    }

    public func dismissItem(id: String) {
        items.removeAll { $0.id == id }
    }

    public func confirmAsTransaction(item: ReviewQueueItem, transaction: CentwiseTransaction) {
        FakeTransactionRepository.shared.addTransaction(transaction)
        dismissItem(id: item.id)
    }
}
