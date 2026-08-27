import SwiftUI

public struct SpendingSummaryCard: View {
    public let monthlyExpense: Double
    public let monthlyIncome: Double
    public let monthlySaved: Double

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    public init(
        monthlyExpense: Double = 0.0,
        monthlyIncome: Double = 0.0,
        monthlySaved: Double = 0.0
    ) {
        self.monthlyExpense = monthlyExpense
        self.monthlyIncome = monthlyIncome
        self.monthlySaved = monthlySaved
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Label & Hero Amount
            VStack(alignment: .leading, spacing: 4) {
                Text("Spent this month")
                    .font(.system(size: 13, weight: .regular))
                    .foregroundColor(.secondary)

                Text(CurrencyFormatter.shared.formatBDT(monthlyExpense, showSign: false))
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
            }

            // Subtle Divider
            Divider()

            // 3-Column Stats Breakdown with Vertical Stripes
            HStack(alignment: .top, spacing: 12) {
                // 1. Income Column
                statColumn(
                    amount: monthlyIncome,
                    label: "Income",
                    stripeColor: Color(red: 0.20, green: 0.78, blue: 0.35) // Green
                )

                // 2. Expenses Column
                statColumn(
                    amount: monthlyExpense,
                    label: "Expenses",
                    stripeColor: Color(red: 1.0, green: 0.23, blue: 0.19) // Red
                )

                // 3. Saved / Net Column
                statColumn(
                    amount: monthlySaved,
                    label: "Saved",
                    stripeColor: Color(red: 0.19, green: 0.69, blue: 0.78) // Teal
                )
            }
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(red: 0.97, green: 0.98, blue: 0.98))
        )
    }

    @ViewBuilder
    private func statColumn(amount: Double, label: String, stripeColor: Color) -> some View {
        HStack(alignment: .center, spacing: 6) {
            RoundedRectangle(cornerRadius: 2)
                .fill(stripeColor)
                .frame(width: 3.5, height: 28)

            VStack(alignment: .leading, spacing: 1) {
                Text(CurrencyFormatter.shared.formatBDT(amount, showSign: false, compact: true))
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Text(label)
                    .font(.system(size: 11, weight: .regular))
                    .foregroundColor(.secondary)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
