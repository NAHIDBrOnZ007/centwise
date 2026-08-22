import SwiftUI

public struct CategoryBreakdownList: View {
    private let items: [CategorySpendSummary]

    @Environment(\.colorScheme) private var colorScheme

    public init(items: [CategorySpendSummary]) {
        self.items = items.sorted { $0.totalAmount > $1.totalAmount }
    }

    public var body: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                Text("Categories")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                if items.isEmpty {
                    Text("No categories to show")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, CentwiseSpacing.md)
                } else {
                    ForEach(items) { item in
                        categoryRow(item)
                    }
                }
            }
        }
    }

    private func categoryRow(_ item: CategorySpendSummary) -> some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
            HStack(spacing: CentwiseSpacing.mdSm) {
                Circle()
                    .fill(item.category.color.opacity(0.15))
                    .frame(width: 34, height: 34)
                    .overlay(
                        Image(systemName: item.category.icon)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(item.category.color)
                    )

                Text(item.category.name)
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                Spacer()

                Text(CurrencyFormatter.shared.formatBDT(item.totalAmount, compact: true))
                    .font(CentwiseTypography.amountSmall)
                    .foregroundColor(.primary)

                Text(String(format: "%.0f%%", item.percentage * 100))
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(.secondary)
                    .frame(width: 38, alignment: .trailing)
                    .monospacedDigit()
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                        .frame(height: 5)

                    Capsule()
                        .fill(item.category.color)
                        .frame(width: geo.size.width * CGFloat(min(item.percentage, 1.0)), height: 5)
                }
            }
            .frame(height: 5)
        }
        .padding(.vertical, CentwiseSpacing.xxs)
    }
}
