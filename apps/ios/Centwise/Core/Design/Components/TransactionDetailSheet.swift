import SwiftUI

public struct TransactionDetailSheet: View {
    public let transaction: CentwiseTransaction
    public var onEdit: (() -> Void)? = nil
    public var onDelete: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared
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
            ScrollView {
                VStack(spacing: 20) {
                    // 1. Hero Amount & Badge
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
                    .padding(.top, 12)

                    // 2. Details Card
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Details")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.secondary)
                            .padding(.leading, 4)

                        VStack(spacing: 0) {
                            detailRow(icon: "storefront", title: "Merchant", value: transaction.title)
                            Divider().padding(.leading, 44)
                            detailRow(icon: "tag", title: "Category", value: transaction.category.name)
                            Divider().padding(.leading, 44)
                            detailRow(icon: "calendar", title: "Date", value: transaction.date.formatted(date: .abbreviated, time: .shortened))
                            Divider().padding(.leading, 44)
                            detailRow(icon: "building.columns", title: "Bank", value: transaction.provider.rawValue)
                            Divider().padding(.leading, 44)
                            detailRow(icon: "creditcard", title: "Account", value: "••\(transaction.accountId.suffix(4))")

                            if let notes = transaction.notes, !notes.isEmpty {
                                Divider().padding(.leading, 44)
                                detailRow(icon: "note.text", title: "Notes", value: notes)
                            }
                        }
                        .background(colorScheme == .dark ? Color(white: 0.12) : Color.white)
                        .cornerRadius(18)
                        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 8, x: 0, y: 2)
                    }
                    .padding(.horizontal, 20)

                    // 3. Delete Button Card
                    Button(action: {
                        showDeleteConfirmation = true
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "trash")
                                .font(.system(size: 15, weight: .medium))
                            Text("Delete Transaction")
                                .font(.system(size: 15, weight: .semibold))
                        }
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(colorScheme == .dark ? Color(white: 0.12) : Color.white)
                        .cornerRadius(18)
                        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 6, x: 0, y: 2)
                    }
                    .buttonStyle(.plain)
                    .padding(.horizontal, 20)
                    .padding(.top, 8)
                }
                .padding(.bottom, 40)
            }
            .background(colorScheme == .dark ? Color.black : Color(red: 0.97, green: 0.97, blue: 0.98))
            .navigationTitle("Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        onEdit?()
                    } label: {
                        Text("Edit")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(themeManager.accentColor)
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
