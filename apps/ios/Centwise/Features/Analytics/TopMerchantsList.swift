import SwiftUI

public struct TopMerchantsList: View {
    private let merchants: [MerchantSpendSummary]
    public var onSelectMerchant: ((MerchantSpendSummary) -> Void)?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(
        merchants: [MerchantSpendSummary],
        onSelectMerchant: ((MerchantSpendSummary) -> Void)? = nil
    ) {
        self.merchants = Array(merchants.sorted { $0.totalAmount > $1.totalAmount }.prefix(5))
        self.onSelectMerchant = onSelectMerchant
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Top Merchants")
                .font(.headline)
                .foregroundColor(.primary)

            if merchants.isEmpty {
                Text("No merchant data to show")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else {
                VStack(spacing: 8) {
                    ForEach(Array(merchants.enumerated()), id: \.element.id) { index, merchant in
                        Button {
                            themeManager.triggerHapticFeedback(.light)
                            onSelectMerchant?(merchant)
                        } label: {
                            merchantRow(rank: index + 1, merchant: merchant)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func merchantRow(rank: Int, merchant: MerchantSpendSummary) -> some View {
        HStack(spacing: 12) {
            Text("\(rank)")
                .font(.system(size: 12, weight: .bold))
                .foregroundColor(rank <= 3 ? themeManager.accentColor : .secondary)
                .frame(width: 24, height: 24)
                .background(
                    Circle().fill(rank <= 3 ? themeManager.accentColor.opacity(0.12) : Color.secondary.opacity(0.12))
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(merchant.merchantName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)
                    .lineLimit(1)

                Text("\(merchant.transactionCount) transaction\(merchant.transactionCount == 1 ? "" : "s")")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Spacer()

            Text(CurrencyFormatter.shared.formatBDT(merchant.totalAmount, showSign: false, compact: true))
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Color(white: 0.7))
                .padding(.leading, 2)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

