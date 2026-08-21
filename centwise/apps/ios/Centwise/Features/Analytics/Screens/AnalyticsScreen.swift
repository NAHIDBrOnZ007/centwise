import SwiftUI

public struct AnalyticsScreen: View {
    @StateObject private var viewModel = AnalyticsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // 1. Time Frame Picker
                Picker("Timeframe", selection: $viewModel.selectedTimeFrame) {
                    ForEach(viewModel.timeFrames, id: \.self) { tf in
                        Text(tf).tag(tf)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.xs)

                // 2. Cash Flow Comparison Hero Card
                cashFlowComparisonCard
                    .padding(.horizontal, CentwiseSpacing.md)

                // 3. Category Spending Breakdown
                categoryBreakdownSection
                    .padding(.horizontal, CentwiseSpacing.md)

                // 4. Top Spending Merchants (Foodpanda, Pathao, Unimart, etc.)
                topMerchantsSection
                    .padding(.horizontal, CentwiseSpacing.md)
            }
            .padding(.bottom, 80)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: isAmoled).ignoresSafeArea())
        .navigationTitle("Analytics & Trends")
        .onAppear {
            viewModel.recalculateAnalytics()
        }
    }

    // MARK: - Subviews
    private var cashFlowComparisonCard: some View {
        CentwiseCard {
            VStack(spacing: CentwiseSpacing.md) {
                HStack {
                    Text("Cash Flow Overview")
                        .font(CentwiseTypography.headline)
                        .foregroundColor(.primary)
                    Spacer()
                    Text(viewModel.selectedTimeFrame)
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                }

                // Bar Visualizer
                GeometryReader { geo in
                    let total = max(viewModel.totalIncome + viewModel.totalExpense, 1.0)
                    let incomeWidth = geo.size.width * CGFloat(viewModel.totalIncome / total)
                    let expenseWidth = geo.size.width * CGFloat(viewModel.totalExpense / total)

                    HStack(spacing: 4) {
                        Capsule()
                            .fill(CentwiseColors.incomeGreen)
                            .frame(width: max(incomeWidth - 2, 4), height: 12)

                        Capsule()
                            .fill(CentwiseColors.expenseRed)
                            .frame(width: max(expenseWidth - 2, 4), height: 12)
                    }
                }
                .frame(height: 12)

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 4) {
                            Circle().fill(CentwiseColors.incomeGreen).frame(width: 8, height: 8)
                            Text("Total Inflow")
                                .font(CentwiseTypography.caption1)
                                .foregroundColor(.secondary)
                        }
                        Text(CurrencyFormatter.shared.formatBDT(viewModel.totalIncome, compact: true))
                            .font(CentwiseTypography.amountMedium)
                            .foregroundColor(CentwiseColors.incomeGreen)
                    }

                    Spacer()

                    VStack(alignment: .trailing, spacing: 2) {
                        HStack(spacing: 4) {
                            Circle().fill(CentwiseColors.expenseRed).frame(width: 8, height: 8)
                            Text("Total Outflow")
                                .font(CentwiseTypography.caption1)
                                .foregroundColor(.secondary)
                        }
                        Text(CurrencyFormatter.shared.formatBDT(viewModel.totalExpense, compact: true))
                            .font(CentwiseTypography.amountMedium)
                            .foregroundColor(CentwiseColors.expenseRed)
                    }
                }
            }
        }
    }

    private var categoryBreakdownSection: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            Text("Spending by Category")
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)

            if viewModel.categoryBreakdown.isEmpty {
                Text("No expense data recorded.")
                    .font(CentwiseTypography.subheadline)
                    .foregroundColor(.secondary)
            } else {
                CentwiseCard {
                    VStack(spacing: CentwiseSpacing.mdSm) {
                        ForEach(viewModel.categoryBreakdown) { item in
                            VStack(spacing: CentwiseSpacing.xs) {
                                HStack {
                                    Circle()
                                        .fill(item.category.color.opacity(0.15))
                                        .frame(width: 28, height: 28)
                                        .overlay(
                                            Image(systemName: item.category.icon)
                                                .font(.system(size: 12, weight: .semibold))
                                                .foregroundColor(item.category.color)
                                        )

                                    Text(item.category.name)
                                        .font(CentwiseTypography.bodyMedium)
                                        .foregroundColor(.primary)

                                    Spacer()

                                    Text(CurrencyFormatter.shared.formatBDT(item.totalAmount, compact: true))
                                        .font(CentwiseTypography.amountSmall)
                                        .foregroundColor(.primary)

                                    Text(String(format: "%.0f%%", item.percentage * 100))
                                        .font(CentwiseTypography.caption1)
                                        .foregroundColor(.secondary)
                                        .frame(width: 36, alignment: .trailing)
                                }

                                GeometryReader { geo in
                                    ZStack(alignment: .leading) {
                                        Capsule()
                                            .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                                            .frame(height: 5)

                                        Capsule()
                                            .fill(item.category.color)
                                            .frame(width: geo.size.width * CGFloat(item.percentage), height: 5)
                                    }
                                }
                                .frame(height: 5)
                            }
                            .padding(.vertical, 2)
                        }
                    }
                }
            }
        }
    }

    private var topMerchantsSection: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            Text("Top Merchants")
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)

            CentwiseCard {
                ForEach(Array(viewModel.topMerchants.prefix(5).enumerated()), id: \.element.id) { idx, merchant in
                    HStack(spacing: CentwiseSpacing.sm) {
                        Text("\(idx + 1)")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)
                            .frame(width: 16)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(merchant.merchantName)
                                .font(CentwiseTypography.bodyMedium)
                                .foregroundColor(.primary)
                            Text("\(merchant.transactionCount) transactions")
                                .font(CentwiseTypography.caption2)
                                .foregroundColor(.secondary)
                        }

                        Spacer()

                        Text(CurrencyFormatter.shared.formatBDT(merchant.totalAmount, compact: true))
                            .font(CentwiseTypography.amountSmall)
                            .foregroundColor(.primary)
                    }
                    .padding(.vertical, CentwiseSpacing.xxs)

                    if idx < min(viewModel.topMerchants.count, 5) - 1 {
                        Divider()
                    }
                }
            }
        }
    }
}
