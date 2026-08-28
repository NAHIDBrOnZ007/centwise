import SwiftUI

public struct AnalyticsDrillDown: Identifiable {
    public let id = UUID()
    public let title: String
    public let transactions: [CentwiseTransaction]

    public init(title: String, transactions: [CentwiseTransaction]) {
        self.title = title
        self.transactions = transactions
    }
}

public struct AnalyticsScreen: View {
    @StateObject private var viewModel = AnalyticsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @State private var drillDown: AnalyticsDrillDown?

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                filterBar

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
            .padding(.horizontal, 16)
            .padding(.top, 8)
            .padding(.bottom, 32)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle("Analytics")
        .onAppear {
            viewModel.recalculateAnalytics()
        }
        .sheet(item: $drillDown) { drill in
            AnalyticsDrillDownSheet(drill: drill)
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

    private var filterBar: some View {
        HStack(spacing: 8) {
            // 1. Period Filter Chip
            Menu {
                Picker("Period", selection: periodBinding) {
                    ForEach(AnalyticsPeriod.allCases, id: \.self) { period in
                        Text(period.rawValue).tag(period)
                    }
                }
            } label: {
                filterChip(
                    title: viewModel.selectedPeriod.rawValue,
                    icon: "calendar",
                    isActive: true
                )
            }

            // 2. Transaction Type Filter Chip
            Menu {
                Picker("Transaction Type", selection: typeBinding) {
                    ForEach(AnalyticsTypeFilter.allCases, id: \.self) { type in
                        Text(type.rawValue).tag(type)
                    }
                }
            } label: {
                filterChip(
                    title: viewModel.selectedTypeFilter == .all ? "Type" : viewModel.selectedTypeFilter.rawValue,
                    icon: "line.3.horizontal.decrease",
                    isActive: viewModel.selectedTypeFilter != .all
                )
            }
        }
        .accessibilityLabel("Analytics filters")
    }

    private func filterChip(title: String, icon: String, isActive: Bool) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 11, weight: .semibold))
            Text(title)
                .font(.system(size: 12, weight: .medium))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Image(systemName: "chevron.down")
                .font(.system(size: 8, weight: .bold))
                .opacity(0.8)
        }
        .foregroundStyle(isActive ? Color.white : Color.primary)
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .frame(maxWidth: .infinity)
        .background(
            isActive
                ? themeManager.accentColor
                : Color(uiColor: .secondarySystemGroupedBackground),
            in: Capsule()
        )
        .overlay {
            if !isActive {
                Capsule()
                    .stroke(Color(uiColor: .separator).opacity(0.45), lineWidth: 1)
            }
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
        CategoryBreakdownList(items: viewModel.categoryBreakdown) { item in
            drillDown = AnalyticsDrillDown(
                title: item.category.name,
                transactions: viewModel.transactions(forCategory: item.category.id)
            )
        }
    }

    private var topMerchantsSection: some View {
        TopMerchantsList(merchants: viewModel.topMerchants) { item in
            drillDown = AnalyticsDrillDown(
                title: item.merchantName,
                transactions: viewModel.transactions(forMerchant: item.merchantName)
            )
        }
    }
}

public struct AnalyticsDrillDownSheet: View {
    public let drill: AnalyticsDrillDown
    @State private var selectedTransaction: CentwiseTransaction?
    @Environment(\.dismiss) private var dismiss

    public init(drill: AnalyticsDrillDown) {
        self.drill = drill
    }

    public var body: some View {
        NavigationStack {
            List {
                if drill.transactions.isEmpty {
                    Text("No transactions found for this period.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, alignment: .center)
                        .padding(.vertical, 24)
                        .listRowBackground(Color.clear)
                } else {
                    Section {
                        ForEach(drill.transactions) { transaction in
                            TransactionRow(
                                transaction: transaction,
                                showChevron: true,
                                onTap: { selectedTransaction = transaction }
                            )
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color(uiColor: .secondarySystemGroupedBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                        }
                    } header: {
                        Text("\(drill.transactions.count) Transactions")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.secondary)
                            .textCase(.uppercase)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(drill.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .sheet(item: $selectedTransaction) { tx in
                TransactionDetailSheet(transaction: tx)
            }
        }
    }
}
