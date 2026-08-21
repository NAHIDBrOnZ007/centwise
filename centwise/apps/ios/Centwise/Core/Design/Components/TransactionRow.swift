import SwiftUI

public struct TransactionRow: View {
    public let transaction: CentwiseTransaction
    public var onTap: (() -> Void)? = nil

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(
        transaction: CentwiseTransaction,
        onTap: (() -> Void)? = nil
    ) {
        self.transaction = transaction
        self.onTap = onTap
    }

    public var body: some View {
        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            onTap?()
        }) {
            HStack(spacing: CentwiseSpacing.mdSm) {
                // Category / Provider Icon
                Circle()
                    .fill(transaction.category.color.opacity(0.15))
                    .frame(width: 42, height: 42)
                    .overlay(
                        Image(systemName: transaction.category.icon)
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(transaction.category.color)
                    )

                // Title & Subtitle
                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text(transaction.title)
                        .font(CentwiseTypography.bodyMedium)
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    HStack(spacing: CentwiseSpacing.xs) {
                        // Account Badge
                        Text(transaction.accountName)
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)

                        Text("•")
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)

                        Text(DateFormatterHelper.shared.formatRelativeOrDate(transaction.date))
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)
                    }
                }

                Spacer()

                // Amount
                VStack(alignment: .trailing, spacing: CentwiseSpacing.xxs) {
                    AmountText(
                        amount: transaction.amount,
                        type: transaction.type,
                        font: CentwiseTypography.amountMedium,
                        showSign: true
                    )

                    if transaction.isAutoTracked {
                        HStack(spacing: 2) {
                            Image(systemName: "bolt.fill")
                                .font(.system(size: 9))
                            Text("SMS")
                                .font(CentwiseTypography.caption2)
                        }
                        .foregroundColor(themeManager.accentColor)
                    }
                }
            }
            .padding(.vertical, CentwiseSpacing.xs)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
