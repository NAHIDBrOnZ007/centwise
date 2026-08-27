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

    private var net: Double {
        income - spent
    }

    private var dailyAverage: Double {
        spent / Double(periodDays)
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Header: Spent This Period & Big Amount
            VStack(alignment: .leading, spacing: 6) {
                Text("SPENT THIS PERIOD")
                    .font(.system(size: 11, weight: .bold))
                    .tracking(0.6)
                    .foregroundColor(.secondary)

                Text(CurrencyFormatter.shared.formatBDT(spent, showSign: false))
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)
            }

            Divider()

            // 3-Column Row 1: Income, Expenses, Net
            HStack(spacing: 0) {
                metricColumn(
                    title: "INCOME",
                    value: CurrencyFormatter.shared.formatBDT(income, showSign: false, compact: true),
                    color: CentwiseColors.incomeGreen
                )

                metricColumn(
                    title: "EXPENSES",
                    value: CurrencyFormatter.shared.formatBDT(spent, showSign: false, compact: true),
                    color: CentwiseColors.expenseRed
                )

                metricColumn(
                    title: "NET",
                    value: "\(net >= 0 ? "+" : "-")\(CurrencyFormatter.shared.formatBDT(abs(net), showSign: false, compact: true))",
                    color: net >= 0 ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed
                )
            }

            Divider()

            // 3-Column Row 2: Transactions, Daily Avg, Top Category
            HStack(spacing: 0) {
                subMetricColumn(
                    title: "TRANSACTIONS",
                    value: "\(transactionCount)"
                )

                subMetricColumn(
                    title: "DAILY AVG",
                    value: CurrencyFormatter.shared.formatBDT(dailyAverage, showSign: false, compact: true)
                )

                subMetricColumn(
                    title: "TOP CATEGORY",
                    value: topCategoryName ?? "Others",
                    showDot: true
                )
            }
        }
        .padding(18)
        .background(colorScheme == .dark ? Color(white: 0.12) : Color.white)
        .cornerRadius(18)
        .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 6, x: 0, y: 2)
    }

    private func metricColumn(title: String, value: String, color: Color) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.system(size: 10, weight: .bold))
                .tracking(0.5)
                .foregroundColor(.secondary)

            Text(value)
                .font(.system(size: 16, weight: .bold, design: .rounded))
                .foregroundColor(color)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func subMetricColumn(title: String, value: String, showDot: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(.system(size: 10, weight: .bold))
                .tracking(0.5)
                .foregroundColor(.secondary)

            HStack(spacing: 4) {
                if showDot {
                    Circle()
                        .fill(Color.gray)
                        .frame(width: 6, height: 6)
                }
                Text(value)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

