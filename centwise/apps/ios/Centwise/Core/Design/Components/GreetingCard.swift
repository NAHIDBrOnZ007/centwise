import SwiftUI

public struct GreetingCard: View {
    public var userName: String = "Siam"
    public var netCashflow: Double = 62890.0

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(userName: String = "Siam", netCashflow: Double = 62890.0) {
        self.userName = userName
        self.netCashflow = netCashflow
    }

    private var greetingTime: String {
        let hour = Calendar.current.component(.hour, from: Date())
        if hour < 12 {
            return "Good morning,"
        } else if hour < 17 {
            return "Good afternoon,"
        } else {
            return "Good evening,"
        }
    }

    public var body: some View {
        HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                Text(greetingTime)
                    .font(CentwiseTypography.subheadline)
                    .foregroundColor(.secondary)
                Text(userName)
                    .font(CentwiseTypography.title2)
                    .foregroundColor(.primary)
            }

            Spacer()

            // Net Cashflow Pill
            VStack(alignment: .trailing, spacing: CentwiseSpacing.xxs) {
                Text("This Month Net")
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
                HStack(spacing: CentwiseSpacing.xs) {
                    Image(systemName: netCashflow >= 0 ? "arrow.up.right" : "arrow.down.right")
                        .font(.system(size: 11, weight: .bold))
                    Text(CurrencyFormatter.shared.formatBDT(netCashflow, showSign: false, compact: true))
                        .font(CentwiseTypography.amountSmall)
                }
                .foregroundColor(netCashflow >= 0 ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed)
                .padding(.horizontal, CentwiseSpacing.sm)
                .padding(.vertical, CentwiseSpacing.xs)
                .background(
                    (netCashflow >= 0 ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed).opacity(0.12)
                )
                .cornerRadius(CentwiseSpacing.radiusFull)
            }
        }
        .padding(.vertical, CentwiseSpacing.xs)
    }
}
