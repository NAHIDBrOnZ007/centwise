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
    public let reference: String?
    public let reason: String

    public init(
        id: String = UUID().uuidString,
        sender: String,
        rawSms: String,
        timestamp: Date = Date(),
        candidateAmount: Double? = nil,
        candidateParty: String? = nil,
        candidateType: TransactionType? = nil,
        reference: String? = nil,
        reason: String = "Format needs confirmation"
    ) {
        self.id = id
        self.sender = sender
        self.rawSms = rawSms
        self.timestamp = timestamp
        self.candidateAmount = candidateAmount
        self.candidateParty = candidateParty
        self.candidateType = candidateType
        self.reference = reference
        self.reason = reason
    }
}

public final class ReviewQueueRepository: ObservableObject {
    public static let shared = ReviewQueueRepository()

    @Published public private(set) var items: [ReviewQueueItem] = []

    public init() {
        refresh()
    }

    public func refresh() {
        items = CentwiseRustBackend.listReviewQueue().map { item in
            ReviewQueueItem(
                id: item.id,
                sender: item.sender ?? "Financial SMS",
                rawSms: item.rawSms,
                timestamp: Date(timeIntervalSince1970: TimeInterval(item.receivedAtEpochMs) / 1000),
                candidateAmount: item.candidateAmountMinor.map { Double($0) / 100 },
                candidateParty: item.party ?? item.merchant,
                candidateType: item.candidateKind.map { kind in
                    switch kind {
                    case .expense: return .expense
                    case .income: return .income
                    case .transfer: return .transfer
                    case .refund: return .refund
                    }
                },
                reference: item.reference,
                reason: item.reason
            )
        }
    }

    public func dismissItem(id: String) {
        if CentwiseRustBackend.dismissReviewQueueItem(id: id) {
            refresh()
        }
    }

    public func confirmAsTransaction(item: ReviewQueueItem, transaction: CentwiseTransaction) {
        if CentwiseRustBackend.convertReviewQueueItem(id: item.id, transaction: transaction) {
            refresh()
        }
    }
}
