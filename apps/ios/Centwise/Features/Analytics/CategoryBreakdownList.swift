import SwiftUI

public struct CategoryBreakdownList: View {
    private let items: [CategorySpendSummary]
    public var onSelectCategory: ((CategorySpendSummary) -> Void)?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(
        items: [CategorySpendSummary],
        onSelectCategory: ((CategorySpendSummary) -> Void)? = nil
    ) {
        self.items = items.sorted { $0.totalAmount > $1.totalAmount }
        self.onSelectCategory = onSelectCategory
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Categories")
                .font(.headline)
                .foregroundColor(.primary)

            if items.isEmpty {
                Text("No categories to show")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else {
                VStack(spacing: 8) {
                    ForEach(items) { item in
                        Button {
                            themeManager.triggerHapticFeedback(.light)
                            onSelectCategory?(item)
                        } label: {
                            categoryRow(item)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }

    private func categoryRow(_ item: CategorySpendSummary) -> some View {
        HStack(spacing: 12) {
            Image(systemName: item.category.icon)
                .font(.system(size: 18, weight: .regular))
                .foregroundColor(themeManager.accentColor)
                .frame(width: 28, height: 28)

            // Category Name & Tx Count
            VStack(alignment: .leading, spacing: 2) {
                Text(item.category.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)

                Text("\(item.count) transaction\(item.count == 1 ? "" : "s")")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Amount & Percentage
            VStack(alignment: .trailing, spacing: 2) {
                Text(CurrencyFormatter.shared.formatBDT(item.totalAmount, showSign: false))
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Text(String(format: "%.1f%%", item.percentage * 100))
                    .font(.system(size: 11))
                    .foregroundColor(.secondary)
            }

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

