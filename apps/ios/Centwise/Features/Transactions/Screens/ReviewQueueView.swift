import SwiftUI

public struct ReviewQueueView: View {
    @ObservedObject private var repository = ReviewQueueRepository.shared
    @State private var itemToConvert: ReviewQueueItem?
    @State private var toastItem: ToastItem?

    public init() {}

    public var body: some View {
        List {
            if repository.items.isEmpty {
                emptyState
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
            } else {
                Section {
                    ForEach(repository.items) { item in
                        queueRow(item)
                    }
                } footer: {
                    Text("Raw SMS stays on this device and is shown only to help you verify the transaction.")
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Review Queue")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if repository.items.isEmpty {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 15))
                        .foregroundColor(CentwiseColors.incomeGreen)
                } else {
                    Text("\(repository.items.count) pending")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .toast(item: $toastItem)
        .sheet(item: $itemToConvert) { item in
            let provider = provider(for: item.sender)
            AddEditTransactionView(
                transactionToEdit: CentwiseTransaction(
                    title: item.candidateParty ?? "\(item.sender) Transaction",
                    amount: item.candidateAmount ?? 0.0,
                    type: item.candidateType ?? .expense,
                    category: TransactionRepository.shared.category(id: "other"),
                    date: item.timestamp,
                    accountId: "",
                    accountName: provider == .cash ? "Cash / Unassigned" : provider.rawValue,
                    provider: provider,
                    rawSmsBody: item.rawSms,
                    transactionReference: item.reference
                ),
                writesToRepository: false,
                onCommit: { transaction in
                    let success = repository.confirmAsTransaction(item: item, transaction: transaction)
                    if success {
                        toastItem = ToastItem("Transaction confirmed successfully", style: .success)
                    }
                    return success
                }
            )
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "checkmark.circle")
                .font(.largeTitle)
                .foregroundStyle(CentwiseColors.incomeGreen)

            Text("All Caught Up!")
                .font(.title2.weight(.semibold))

            Text("No pending SMS messages in your review queue. Financial SMS messages via Shortcuts or Share sheet are automatically converted into transactions.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, minHeight: 320)
        .padding(.vertical, 32)
    }

    private func queueRow(_ item: ReviewQueueItem) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(item.sender)
                    .font(.headline)

                Spacer()

                Text(item.timestamp, format: .dateTime.day().month().hour().minute())
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Text(item.reason)
                .font(.caption)
                .foregroundStyle(.secondary)

            Text(item.rawSms)
                .font(.system(.caption, design: .monospaced))
                .textSelection(.enabled)
                .padding(10)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(uiColor: .secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 8))
                .accessibilityLabel("SMS content")

            HStack {
                Button("Dismiss", role: .destructive) {
                    repository.dismissItem(id: item.id)
                    toastItem = ToastItem("Item dismissed", style: .info)
                }
                .buttonStyle(.bordered)

                Spacer()

                Button {
                    itemToConvert = item
                } label: {
                    Label("Convert", systemImage: "square.and.pencil")
                }
                .buttonStyle(.borderedProminent)
                .tint(CentwiseColors.primaryEmerald)
            }
        }
        .padding(.vertical, 4)
    }

    private func provider(for sender: String) -> FinancialProvider {
        let normalized = sender.lowercased()
        if normalized.contains("bkash") { return .bkash }
        if normalized.contains("nagad") { return .nagad }
        if normalized.contains("rocket") { return .rocket }
        if normalized.contains("upay") { return .upay }
        if normalized.contains("cellfin") { return .cellfin }
        if normalized.contains("dutch") || normalized.contains("dbbl") { return .dutchBangla }
        if normalized.contains("city") { return .cityBank }
        if normalized.contains("brac") { return .bracBank }
        if normalized.contains("eastern") || normalized.contains("ebl") { return .easternBank }
        if normalized.contains("standard chartered") || normalized.contains("scb") { return .standardChartered }
        return normalized.contains("cash") ? .cash : .other
    }
}
