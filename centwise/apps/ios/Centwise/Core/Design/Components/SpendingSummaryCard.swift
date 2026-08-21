import SwiftUI

public struct SpendingSummaryCard: View {
    public let monthlyExpense: Double
    public let monthlyIncome: Double
    public let monthlyNet: Double

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    public init(
        monthlyExpense: Double,
        monthlyIncome: Double,
        monthlyNet: Double
    ) {
        self.monthlyExpense = monthlyExpense
        self.monthlyIncome = monthlyIncome
        self.monthlyNet = monthlyNet
    }

    public var body: some View {
        VStack(spacing: CentwiseSpacing.md) {
            // Top: Total Expense Hero
            VStack(spacing: CentwiseSpacing.xs) {
                Text("Total Spending this Month")
                    .font(CentwiseTypography.subheadline)
                    .foregroundColor(.secondary)

                Text(CurrencyFormatter.shared.formatBDT(monthlyExpense, showSign: false))
                    .font(CentwiseTypography.amountHero)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding(.top, CentwiseSpacing.xs)

            Divider()
                .background(CentwiseColors.border(for: colorScheme))

            // Bottom: Income & Net Balance Breakdown
            HStack(spacing: CentwiseSpacing.lg) {
                // Income Column
                HStack(spacing: CentwiseSpacing.sm) {
                    Circle()
                        .fill(CentwiseColors.incomeGreen.opacity(0.15))
                        .frame(width: 36, height: 36)
                        .overlay(
                            Image(systemName: "arrow.down.left")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(CentwiseColors.incomeGreen)
                        )

                    VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                        Text("Income")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)
                        Text(CurrencyFormatter.shared.formatBDT(monthlyIncome, showSign: false, compact: true))
                            .font(CentwiseTypography.amountSmall)
                            .foregroundColor(.primary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                // Net Balance Column
                HStack(spacing: CentwiseSpacing.sm) {
                    Circle()
                        .fill(themeManager.accentColor.opacity(0.15))
                        .frame(width: 36, height: 36)
                        .overlay(
                            Image(systemName: "wallet.pass.fill")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(themeManager.accentColor)
                        )

                    VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                        Text("Net Balance")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)
                        Text(CurrencyFormatter.shared.formatBDT(monthlyNet, showSign: false, compact: true))
                            .font(CentwiseTypography.amountSmall)
                            .foregroundColor(monthlyNet >= 0 ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .padding(CentwiseSpacing.md)
        .glassCard(cornerRadius: CentwiseSpacing.radiusLg)
    }
}
