import SwiftUI

public struct AnalyticsSummaryCard: View {
    private let spent: Double
    private let income: Double
    private let transactionCount: Int
    private let topCategoryName: String?
    private let periodDays: Int

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(
        spent: Double,
        income: Double,
        transactionCount: Int,
        topCategoryName: String?,
        periodDays: Int = 30
    ) {
        self.spent = spent
        self.income = income
        self.transactionCount = transactionCount
        self.topCategoryName = topCategoryName
        self.periodDays = max(periodDays, 1)
    }

    private var dailyAverage: Double {
        spent / Double(periodDays)
    }

    private var savingsRate: Double {
        guard income > 0 else { return 0 }
        return max((income - spent) / income, 0)
    }

    public var body: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                Text("SPENT THIS PERIOD")
                    .font(CentwiseTypography.caption2)
                    .tracking(0.8)
                    .foregroundColor(.secondary)

                HStack(alignment: .firstTextBaseline, spacing: CentwiseSpacing.sm) {
                    Text(CurrencyFormatter.shared.formatBDT(spent))
                        .font(CentwiseTypography.amountHero)
                        .foregroundColor(.primary)

                    Spacer()

                    Label(
                        String(format: "%.0f%% saved", savingsRate * 100),
                        systemImage: income >= spent ? "arrow.down.circle.fill" : "exclamationmark.circle.fill"
                    )
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(income >= spent ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed)
                }

                Rectangle()
                    .fill(CentwiseColors.border(for: colorScheme))
                    .frame(height: 1)

                HStack(spacing: 0) {
                    statBlock(
                        title: "TRANSACTIONS",
                        value: "\(transactionCount)",
                        icon: "list.bullet.rectangle"
                    )

                    statBlock(
                        title: "DAILY AVG",
                        value: CurrencyFormatter.shared.formatBDT(dailyAverage, compact: true),
                        icon: "calendar"
                    )

                    statBlock(
                        title: "TOP CATEGORY",
                        value: topCategoryName ?? "—",
                        icon: "chart.pie.fill",
                        isText: true
                    )
                }
            }
        }
    }

    private func statBlock(title: String, value: String, icon: String, isText: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
            HStack(spacing: CentwiseSpacing.xxs) {
                Image(systemName: icon)
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(themeManager.accentColor)
                Text(title)
                    .font(CentwiseTypography.caption2)
                    .tracking(0.5)
                    .foregroundColor(.secondary)
            }

            Text(value)
                .font(isText ? CentwiseTypography.caption1 : CentwiseTypography.amountMedium)
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
