import SwiftUI

public struct TransactionDetailSheet: View {
    public let transaction: CentwiseTransaction
    public var onEdit: (() -> Void)? = nil
    public var onDelete: (() -> Void)? = nil

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

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
                VStack(spacing: CentwiseSpacing.lg) {
                    // Hero Amount Header
                    VStack(spacing: CentwiseSpacing.xs) {
                        Circle()
                            .fill(transaction.category.color.opacity(0.15))
                            .frame(width: 64, height: 64)
                            .overlay(
                                Image(systemName: transaction.category.icon)
                                    .font(.system(size: 28, weight: .semibold))
                                    .foregroundColor(transaction.category.color)
                            )

                        Text(transaction.title)
                            .font(CentwiseTypography.title3)
                            .foregroundColor(.primary)
                            .multilineTextAlignment(.center)

                        AmountText(
                            amount: transaction.amount,
                            type: transaction.type,
                            font: CentwiseTypography.amountHero,
                            showSign: true
                        )

                        Text(transaction.type.rawValue)
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(transaction.type.color)
                            .padding(.horizontal, CentwiseSpacing.sm)
                            .padding(.vertical, CentwiseSpacing.xs)
                            .background(transaction.type.color.opacity(0.12))
                            .cornerRadius(CentwiseSpacing.radiusFull)
                    }
                    .padding(.top, CentwiseSpacing.md)

                    // Details Card
                    CentwiseCard {
                        detailRow(title: "Date & Time", value: DateFormatterHelper.shared.formatFullDate(transaction.date))
                        Divider()
                        detailRow(title: "Account / Wallet", value: transaction.accountName)
                        Divider()
                        detailRow(title: "Provider", value: transaction.provider.rawValue)
                        Divider()
                        detailRow(title: "Category", value: transaction.category.name)

                        if let ref = transaction.transactionReference {
                            Divider()
                            detailRow(title: "Transaction ID", value: ref)
                        }

                        if let balance = transaction.balanceAfter {
                            Divider()
                            detailRow(title: "Balance After", value: CurrencyFormatter.shared.formatBDT(balance))
                        }

                        if let notes = transaction.notes, !notes.isEmpty {
                            Divider()
                            detailRow(title: "Notes", value: notes)
                        }
                    }

                    // Raw SMS Ingestion Card (if available)
                    if let rawSms = transaction.rawSmsBody {
                        VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                            HStack(spacing: CentwiseSpacing.xs) {
                                Image(systemName: "message.fill")
                                    .font(.system(size: 13))
                                Text("Original Transaction SMS")
                                    .font(CentwiseTypography.caption1)
                            }
                            .foregroundColor(.secondary)

                            Text(rawSms)
                                .font(CentwiseTypography.footnote)
                                .foregroundColor(.secondary)
                                .padding(CentwiseSpacing.mdSm)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(CentwiseColors.surfaceSecondary(for: colorScheme))
                                .cornerRadius(CentwiseSpacing.radiusMd)
                        }
                        .padding(.horizontal, CentwiseSpacing.xs)
                    }

                    // Actions
                    HStack(spacing: CentwiseSpacing.md) {
                        CentwiseButton("Edit", icon: "pencil", variant: .secondary, isFullWidth: true) {
                            dismiss()
                            onEdit?()
                        }

                        CentwiseButton("Delete", icon: "trash", variant: .destructive, isFullWidth: true) {
                            dismiss()
                            onDelete?()
                        }
                    }
                    .padding(.top, CentwiseSpacing.xs)
                }
                .padding(CentwiseSpacing.md)
            }
            .background(CentwiseColors.background(for: colorScheme).ignoresSafeArea())
            .navigationTitle("Transaction Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(themeManager.accentColor)
                }
            }
        }
    }

    @ViewBuilder
    private func detailRow(title: String, value: String) -> some View {
        HStack {
            Text(title)
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(CentwiseTypography.bodyMedium)
                .foregroundColor(.primary)
        }
        .padding(.vertical, CentwiseSpacing.xs)
    }
}
