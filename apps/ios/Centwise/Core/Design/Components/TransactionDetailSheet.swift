import SwiftUI

public struct TransactionDetailSheet: View {
    public let transaction: CentwiseTransaction
    public var onEdit: (() -> Void)? = nil
    public var onDelete: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @State private var showDeleteConfirmation = false

    public init(
        transaction: CentwiseTransaction,
        onEdit: (() -> Void)? = nil,
        onDelete: (() -> Void)? = nil
    ) {
        self.transaction = transaction
        self.onEdit = onEdit
        self.onDelete = onDelete
    }

    public var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(spacing: 8) {
                        Text(formatSignedAmount())
                            .font(.system(size: 38, weight: .bold, design: .rounded))
                            .foregroundColor(transaction.type == .income ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed)

                        // Badge: ↑ EXPENSE / ↓ INCOME
                        HStack(spacing: 4) {
                            Image(systemName: transaction.type == .income ? "arrow.down.circle.fill" : "arrow.up.circle.fill")
                                .font(.system(size: 11, weight: .bold))
                            Text(transaction.type.rawValue.uppercased())
                                .font(.system(size: 11, weight: .bold))
                        }
                        .foregroundColor(transaction.type == .income ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 5)
                        .background((transaction.type == .income ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed).opacity(0.12))
                        .cornerRadius(999)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .listRowBackground(Color.clear)
                }

                Section("Details") {
                    LabeledContent("Merchant", value: transaction.title)
                    LabeledContent("Category", value: transaction.category.name)
                    LabeledContent("Date", value: transaction.date.formatted(date: .abbreviated, time: .shortened))
                    LabeledContent("Bank", value: transaction.provider.rawValue)
                    LabeledContent("Account", value: "••\(transaction.accountId.suffix(4))")

                    if let notes = transaction.notes, !notes.isEmpty {
                        LabeledContent("Notes", value: notes)
                    }
                }

                Section {
                    Button("Delete Transaction", role: .destructive) {
                        showDeleteConfirmation = true
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Close") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    if let onEdit {
                        Button("Edit", action: onEdit)
                    }
                }
            }
            .alert("Delete Transaction", isPresented: $showDeleteConfirmation) {
                Button("Delete", role: .destructive) {
                    onDelete?()
                    dismiss()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Are you sure you want to delete this transaction?")
            }
        }
    }

    private func formatSignedAmount() -> String {
        let prefix = transaction.type == .expense ? "-" : "+"
        return "\(prefix)\(CurrencyFormatter.shared.formatBDT(transaction.amount, showSign: false))"
    }

    private func detailRow(icon: String, title: String, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 15))
                .foregroundColor(.secondary)
                .frame(width: 24)

            Text(title)
                .font(.system(size: 15))
                .foregroundColor(.secondary)

            Spacer()

            Text(value)
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(.primary)
                .multilineTextAlignment(.trailing)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}
