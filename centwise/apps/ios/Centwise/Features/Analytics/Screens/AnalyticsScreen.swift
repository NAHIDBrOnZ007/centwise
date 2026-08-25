import SwiftUI

public struct AnalyticsScreen: View {
    @StateObject private var viewModel = AnalyticsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // 1. Period Filter Pills (Horizontal Scroll)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(AnalyticsPeriod.allCases, id: \.self) { period in
                            Button(action: {
                                themeManager.triggerHapticFeedback(.selection)
                                viewModel.setPeriod(period)
                            }) {
                                Text(period.rawValue)
                                    .font(.system(size: 13, weight: viewModel.selectedPeriod == period ? .semibold : .medium))
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 7)
                                    .background(viewModel.selectedPeriod == period ? themeManager.accentColor : (colorScheme == .dark ? Color(white: 0.14) : Color(white: 0.94)))
                                    .foregroundColor(viewModel.selectedPeriod == period ? .white : .primary)
                                    .cornerRadius(999)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                // 2. Type Filter Pills (All, Debit, Credit)
                HStack(spacing: 8) {
                    ForEach(AnalyticsTypeFilter.allCases, id: \.self) { type in
                        Button(action: {
                            themeManager.triggerHapticFeedback(.selection)
                            viewModel.setTypeFilter(type)
                        }) {
                            Text(type.rawValue)
                                .font(.system(size: 13, weight: viewModel.selectedTypeFilter == type ? .semibold : .medium))
                                .padding(.horizontal, 16)
                                .padding(.vertical, 7)
                                .background(viewModel.selectedTypeFilter == type ? themeManager.accentColor : (colorScheme == .dark ? Color(white: 0.14) : Color(white: 0.94)))
                                .foregroundColor(viewModel.selectedTypeFilter == type ? .white : .primary)
                                .cornerRadius(999)
                        }
                        .buttonStyle(.plain)
                    }
                }

                // 3. Period Summary Hero Card
                AnalyticsSummaryCard(
                    spent: viewModel.totalExpense,
                    income: viewModel.totalIncome,
                    transactionCount: viewModel.transactionCount,
                    topCategoryName: viewModel.topCategoryName,
                    periodDays: viewModel.selectedPeriod.daysCount
                )

                // 4. Spending Trends (last 6 months)
                if !viewModel.trendPoints.isEmpty {
                    SpendingTrendsChart(points: viewModel.trendPoints)
                }

                // 5. Category Pie / Donut Chart
                if !pieSlices.isEmpty {
                    CategoryPieChart(slices: pieSlices)
                }

                // 6. Category Spending Breakdown List
                categoryBreakdownSection

                // 7. Top Spending Merchants List
                topMerchantsSection
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 120)
        }
        .background(colorScheme == .dark ? Color.black : Color(red: 0.98, green: 0.98, blue: 0.99))
        .navigationTitle("Analytics")
        .onAppear {
            viewModel.recalculateAnalytics()
        }
    }

    // MARK: - Derived Data

    private var pieSlices: [CategorySlice] {
        viewModel.categoryBreakdown.prefix(6).map { item in
            CategorySlice(
                id: item.category.id,
                name: item.category.name,
                value: item.totalAmount,
                color: item.category.color
            )
        }
    }

    private var categoryBreakdownSection: some View {
        CategoryBreakdownList(items: viewModel.categoryBreakdown)
    }

    private var topMerchantsSection: some View {
        TopMerchantsList(merchants: viewModel.topMerchants)
    }
}
