import SwiftUI

public struct TopMerchantsList: View {
    private let merchants: [MerchantSpendSummary]

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(merchants: [MerchantSpendSummary]) {
        self.merchants = Array(merchants.sorted { $0.totalAmount > $1.totalAmount }.prefix(5))
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Top Merchants")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.primary)

            if merchants.isEmpty {
                Text("No merchant data to show")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(merchants.enumerated()), id: \.element.id) { index, merchant in
                        merchantRow(rank: index + 1, merchant: merchant)

                        if index < merchants.count - 1 {
                            Divider()
                                .padding(.leading, 40)
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(colorScheme == .dark ? Color(white: 0.12) : Color.white)
                .cornerRadius(18)
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 6, x: 0, y: 2)
            }
        }
    }

    private func merchantRow(rank: Int, merchant: MerchantSpendSummary) -> some View {
        HStack(spacing: 12) {
            Text("\(rank)")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(rank <= 3 ? themeManager.accentColor : .secondary)
                .frame(width: 24, height: 24)
                .background(
                    Circle().fill(rank <= 3 ? themeManager.accentColor.opacity(0.12) : (colorScheme == .dark ? Color(white: 0.18) : Color(white: 0.92)))
                )

            VStack(alignment: .leading, spacing: 2) {
                Text(merchant.merchantName)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)
                    .lineLimit(1)

                Text("\(merchant.transactionCount) transaction\(merchant.transactionCount == 1 ? "" : "s")")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Spacer()

            Text(CurrencyFormatter.shared.formatBDT(merchant.totalAmount, showSign: false, compact: true))
                .font(.system(size: 15, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Color(white: 0.7))
                .padding(.leading, 4)
        }
        .padding(.vertical, 10)
    }
}

