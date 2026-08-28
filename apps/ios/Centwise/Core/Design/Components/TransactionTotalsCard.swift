import SwiftUI

public struct TransactionTotalsCard: View {
    public let income: Double
    public let expense: Double
    public let net: Double

    public init(income: Double, expense: Double, net: Double) {
        self.income = income
        self.expense = expense
        self.net = net
    }

    public var body: some View {
        HStack(spacing: 0) {
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
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
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
            HStack(spacing: 3) {
                Image(systemName: icon)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(color)
                Text(label)
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(.secondary)
            }

            Text(formatAmount(amount))
                .font(.system(size: 16, weight: .semibold, design: .rounded))
                .foregroundColor(color)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .frame(maxWidth: .infinity)
    }

    private func formatAmount(_ value: Double) -> String {
        let prefix = (showSign && value > 0) ? "+" : ""
        return "\(prefix)\(CurrencyFormatter.shared.formatBDT(value, showSign: showSign, compact: true).replacingOccurrences(of: "৳ ", with: "৳"))"
    }
}

