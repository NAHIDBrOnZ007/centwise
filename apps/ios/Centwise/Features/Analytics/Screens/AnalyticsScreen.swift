import SwiftUI

public struct AnalyticsScreen: View {
    @StateObject private var viewModel = AnalyticsViewModel()
    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack {
                    Picker("Period", selection: periodBinding) {
                        ForEach(AnalyticsPeriod.allCases, id: \.self) { period in
                            Text(period.rawValue).tag(period)
                        }
                    }

                    Spacer()

                    Picker("Transaction Type", selection: typeBinding) {
                        ForEach(AnalyticsTypeFilter.allCases, id: \.self) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                }
                .pickerStyle(.menu)
                .font(.subheadline)

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
            .padding(.bottom, 32)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle("Analytics")
        .onAppear {
            viewModel.recalculateAnalytics()
        }
    }

    private var periodBinding: Binding<AnalyticsPeriod> {
        Binding(
            get: { viewModel.selectedPeriod },
            set: { viewModel.setPeriod($0) }
        )
    }

    private var typeBinding: Binding<AnalyticsTypeFilter> {
        Binding(
            get: { viewModel.selectedTypeFilter },
            set: { viewModel.setTypeFilter($0) }
        )
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
