import SwiftUI

public struct TransactionTotalsCard: View {
    public let income: Double
    public let expense: Double
    public let net: Double

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    public init(income: Double, expense: Double, net: Double) {
        self.income = income
        self.expense = expense
        self.net = net
    }

    public var body: some View {
        HStack(spacing: CentwiseSpacing.xs) {
            TotalColumn(
                icon: "arrow.down",
                label: "Income",
                amount: income,
                color: CentwiseColors.incomeGreen
            )

            TotalColumn(
                icon: "arrow.up",
                label: "Expenses",
                amount: expense,
                color: CentwiseColors.expenseRed
            )

            TotalColumn(
                icon: "plus.forwardslash.minus",
                label: "Net",
                amount: net,
                color: net >= 0 ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed,
                showSign: true
            )
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 14)
        .background(colorScheme == .dark ? Color(white: 0.12) : Color(white: 0.96))
        .cornerRadius(16)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 6, x: 0, y: 2)
    }
}

private struct TotalColumn: View {
    let icon: String
    let label: String
    let amount: Double
    let color: Color
    var showSign: Bool = false

    var body: some View {
        VStack(spacing: 4) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(color)
                Text(label)
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
            }

            Text(formatAmount(amount))
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundColor(color)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity)
    }

    private func formatAmount(_ value: Double) -> String {
        let prefix = (showSign && value > 0) ? "+" : ""
        return "\(prefix)\(CurrencyFormatter.shared.formatBDT(value, showSign: showSign, compact: true))"
    }
}
