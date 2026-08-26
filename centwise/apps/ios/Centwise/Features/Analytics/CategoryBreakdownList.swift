import SwiftUI

public struct CategoryBreakdownList: View {
    private let items: [CategorySpendSummary]

    @Environment(\.colorScheme) private var colorScheme

    public init(items: [CategorySpendSummary]) {
        self.items = items.sorted { $0.totalAmount > $1.totalAmount }
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Categories")
                .font(.system(size: 18, weight: .bold))
                .foregroundColor(.primary)

            if items.isEmpty {
                Text("No categories to show")
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 20)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                        categoryRow(item)

                        if index < items.count - 1 {
                            Divider()
                                .padding(.leading, 32)
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

    private func categoryRow(_ item: CategorySpendSummary) -> some View {
        HStack(spacing: 12) {
            // Category Color Dot
            Circle()
                .fill(item.category.color)
                .frame(width: 10, height: 10)

            // Category Name & Tx Count
            VStack(alignment: .leading, spacing: 2) {
                Text(item.category.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)

                Text("\(TransactionRepository.shared.transactions.filter { $0.category.id == item.category.id }.count) transactions")
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Spacer()

            // Amount & Percentage
            VStack(alignment: .trailing, spacing: 2) {
                Text(CurrencyFormatter.shared.formatBDT(item.totalAmount, showSign: false))
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Text(String(format: "%.1f%%", item.percentage * 100))
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
            }

            Image(systemName: "chevron.right")
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(Color(white: 0.7))
                .padding(.leading, 4)
        }
        .padding(.vertical, 10)
    }
}

