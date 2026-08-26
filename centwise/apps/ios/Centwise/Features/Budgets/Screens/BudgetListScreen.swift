import SwiftUI

public struct BudgetListScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    @State private var showAddBudget = false

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // Total Budget Overview Card
                CentwiseCard {
                    let totalBudget = repository.budgets.reduce(0) { $0 + $1.budgetLimit }
                    let totalSpent = repository.budgets.reduce(0) { $0 + $1.currentSpent }
                    let pct = totalBudget > 0 ? min(totalSpent / totalBudget, 1.0) : 0.0

                    VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                        Text("Monthly Total Budget")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)

                        HStack {
                            Text(CurrencyFormatter.shared.formatBDT(totalSpent))
                                .font(CentwiseTypography.amountHero)
                                .foregroundColor(.primary)
                            Spacer()
                            Text("of " + CurrencyFormatter.shared.formatBDT(totalBudget))
                                .font(CentwiseTypography.bodyMedium)
                                .foregroundColor(.secondary)
                        }

                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                                    .frame(height: 10)

                                Capsule()
                                    .fill(totalSpent > totalBudget ? CentwiseColors.expenseRed : themeManager.accentColor)
                                    .frame(width: geo.size.width * CGFloat(pct), height: 10)
                            }
                        }
                        .frame(height: 10)

                        HStack {
                            Text(String(format: "%.0f%% Used", pct * 100))
                                .font(CentwiseTypography.caption1)
                                .foregroundColor(.secondary)
                            Spacer()
                            Text(CurrencyFormatter.shared.formatBDT(max(totalBudget - totalSpent, 0)) + " Remaining")
                                .font(CentwiseTypography.caption1)
                                .foregroundColor(CentwiseColors.incomeGreen)
                        }
                    }
                    .padding(.vertical, CentwiseSpacing.xs)
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.xs)

                // Category Budgets List
                VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                    HStack {
                        Text("Category Budgets")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)
                        Spacer()
                    }
                    .padding(.horizontal, CentwiseSpacing.md)

                    ForEach(repository.budgets) { budget in
                        NavigationLink {
                            BudgetDetailScreen(budgetId: budget.id)
                        } label: {
                            CentwiseCard {
                                VStack(spacing: CentwiseSpacing.sm) {
                                    HStack {
                                        Circle()
                                            .fill((Color(hex: budget.categoryColorHex) ?? themeManager.accentColor).opacity(0.15))
                                            .frame(width: 36, height: 36)
                                            .overlay(
                                                Image(systemName: budget.categoryIcon)
                                                    .foregroundColor(Color(hex: budget.categoryColorHex) ?? themeManager.accentColor)
                                                    .font(.system(size: 15, weight: .semibold))
                                            )

                                        Text(budget.categoryName)
                                            .font(CentwiseTypography.bodyMedium)
                                            .foregroundColor(.primary)

                                        Spacer()

                                        Text(CurrencyFormatter.shared.formatBDT(budget.currentSpent) + " / " + CurrencyFormatter.shared.formatBDT(budget.budgetLimit, compact: true))
                                            .font(CentwiseTypography.amountSmall)
                                            .foregroundColor(.primary)
                                    }

                                    GeometryReader { geo in
                                        ZStack(alignment: .leading) {
                                            Capsule()
                                                .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                                                .frame(height: 8)

                                            Capsule()
                                                .fill(budget.isOverBudget ? CentwiseColors.expenseRed : (Color(hex: budget.categoryColorHex) ?? themeManager.accentColor))
                                                .frame(width: geo.size.width * CGFloat(budget.percentage), height: 8)
                                        }
                                    }
                                    .frame(height: 8)
                                }
                            }
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .padding(.horizontal, CentwiseSpacing.md)
                    }
                }
            }
            .padding(.bottom, 80)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: isAmoled).ignoresSafeArea())
        .navigationTitle("Budgets")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showAddBudget = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }
        }
        .sheet(isPresented: $showAddBudget) {
            NavigationStack {
                AddEditBudgetScreen { budget in
                    repository.addBudget(budget)
                }
            }
        }
    }
}
