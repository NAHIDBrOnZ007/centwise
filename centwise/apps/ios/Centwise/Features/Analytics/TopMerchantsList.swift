import SwiftUI

public struct TopMerchantsList: View {
    private let merchants: [MerchantSpendSummary]

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(merchants: [MerchantSpendSummary]) {
        self.merchants = Array(merchants.sorted { $0.totalAmount > $1.totalAmount }.prefix(5))
    }

    public var body: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                Text("Top Merchants")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                if merchants.isEmpty {
                    Text("No merchant data to show")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, CentwiseSpacing.md)
                } else {
                    ForEach(Array(merchants.enumerated()), id: \.element.id) { index, merchant in
                        merchantRow(rank: index + 1, merchant: merchant)
                    }
                }
            }
        }
    }

    private func merchantRow(rank: Int, merchant: MerchantSpendSummary) -> some View {
        HStack(spacing: CentwiseSpacing.md) {
            Text("\(rank)")
                .font(CentwiseTypography.amountSmall)
                .foregroundColor(rank <= 3 ? themeManager.accentColor : .secondary)
                .frame(width: 22, height: 22)
                .background(
                    Circle().fill(rank <= 3 ? themeManager.accentColor.opacity(0.12) : Color.clear)
                )

            VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                Text(merchant.merchantName)
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                Text("\(merchant.transactionCount) transaction\(merchant.transactionCount == 1 ? "" : "s")")
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Text(CurrencyFormatter.shared.formatBDT(merchant.totalAmount, compact: true))
                .font(CentwiseTypography.amountSmall)
                .foregroundColor(.primary)
        }
        .padding(.vertical, CentwiseSpacing.xxs)
    }
}
