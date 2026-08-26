import SwiftUI

public struct BudgetDetailScreen: View {
    private let budgetId: String

    @ObservedObject private var repository = TransactionRepository.shared
    @State private var showingEditSheet = false
    @State private var showingDeleteAlert = false

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(budgetId: String) {
        self.budgetId = budgetId
    }

    private var budget: CategoryBudget? {
        repository.budgets.first { $0.id == budgetId }
    }

    private var categoryTransactions: [CentwiseTransaction] {
        guard let budget = budget else { return [] }
        return repository.transactions
            .filter { $0.category.id == budget.categoryId && $0.type == .expense }
            .sorted { $0.date > $1.date }
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                if let budget = budget {
                    progressCard(budget)
                    allowanceCard(budget)
                    transactionsSection
                }
            }
            .padding(.horizontal, CentwiseSpacing.md)
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle(budget?.categoryName ?? "Budget")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    showingEditSheet = true
                } label: {
                    Image(systemName: "slider.horizontal.3")
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(role: .destructive) {
                    showingDeleteAlert = true
                } label: {
                    Image(systemName: "trash")
                        .foregroundColor(CentwiseColors.expenseRed)
                }
            }
        }
        .sheet(isPresented: $showingEditSheet) {
            NavigationStack {
                if let budget = budget {
                    AddEditBudgetScreen(editingBudget: budget) { updated in
                        updateBudget(updated)
                    }
                }
            }
        }
        .alert("Delete Budget?", isPresented: $showingDeleteAlert) {
            Button("Delete", role: .destructive) { deleteBudget() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently delete this budget. Transactions are not affected.")
        }
    }

    // MARK: - Progress

    private func progressCard(_ budget: CategoryBudget) -> some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                HStack(spacing: CentwiseSpacing.mdSm) {
                    Circle()
                        .fill(budget.colorHexColor.opacity(0.15))
                        .frame(width: 42, height: 42)
                        .overlay(
                            Image(systemName: budget.categoryIcon)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(budget.colorHexColor)
                        )

                    VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                        Text(budget.categoryName)
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)
                        Text(budget.isOverBudget ? "Over budget" : "On track")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(budget.isOverBudget ? CentwiseColors.expenseRed : CentwiseColors.incomeGreen)
                    }

                    Spacer()
                }

                HStack(alignment: .firstTextBaseline) {
                    Text(CurrencyFormatter.shared.formatBDT(budget.currentSpent))
                        .font(CentwiseTypography.amountHero)
                        .foregroundColor(.primary)
                    Text("of " + CurrencyFormatter.shared.formatBDT(budget.budgetLimit))
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                }

                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                            .frame(height: 10)

                        Capsule()
                            .fill(budget.isOverBudget ? CentwiseColors.expenseRed : budget.colorHexColor)
                            .frame(width: geo.size.width * CGFloat(budget.percentage), height: 10)
                    }
                }
                .frame(height: 10)

                HStack {
                    Text(String(format: "%.0f%% used", budget.percentage * 100))
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)

                    Spacer()

                    Text(CurrencyFormatter.shared.formatBDT(budget.remainingAmount) + " remaining")
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(budget.isOverBudget ? CentwiseColors.expenseRed : CentwiseColors.incomeGreen)
                }
            }
        }
    }

    // MARK: - Daily Allowance

    private func allowanceCard(_ budget: CategoryBudget) -> some View {
        let daysLeft = max(daysRemainingInMonth(), 1)
        let remaining = budget.remainingAmount
        let perDay = remaining / Double(daysLeft)

        return CentwiseCard {
            HStack {
                allowanceColumn(
                    title: "Daily Allowance",
                    value: CurrencyFormatter.shared.formatBDT(perDay, compact: true),
                    icon: "sun.max.fill",
                    tint: CentwiseColors.nagadOrange
                )

                Rectangle()
                    .fill(CentwiseColors.border(for: colorScheme))
                    .frame(width: 1, height: 44)

                allowanceColumn(
                    title: "Days Left",
                    value: "\(daysLeft)",
                    icon: "calendar",
                    tint: themeManager.accentColor
                )

                Rectangle()
                    .fill(CentwiseColors.border(for: colorScheme))
                    .frame(width: 1, height: 44)

                allowanceColumn(
                    title: "Spent",
                    value: CurrencyFormatter.shared.formatBDT(budget.currentSpent, compact: true),
                    icon: "arrow.up.right",
                    tint: CentwiseColors.expenseRed
                )
            }
        }
    }

    private func allowanceColumn(title: String, value: String, icon: String, tint: Color) -> some View {
        VStack(spacing: CentwiseSpacing.xs) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(tint)

            Text(value)
                .font(CentwiseTypography.amountMedium)
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Text(title)
                .font(CentwiseTypography.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private func daysRemainingInMonth() -> Int {
        let calendar = Calendar.current
        let today = Date()
        guard let range = calendar.range(of: .day, in: .month, for: today) else { return 30 }
        return range.count - calendar.component(.day, from: today) + 1
    }

    // MARK: - Transactions

    private var transactionsSection: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            Text("Spending in this Category")
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)

            if categoryTransactions.isEmpty {
                CentwiseCard {
                    Text("No spending recorded yet this period")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                }
            } else {
                VStack(spacing: CentwiseSpacing.xs) {
                    ForEach(categoryTransactions.prefix(10)) { transaction in
                        TransactionRow(transaction: transaction)
                    }
                }
            }
        }
    }

    // MARK: - Mutations

    private func updateBudget(_ updated: CategoryBudget) {
        repository.updateBudget(updated)
    }

    private func deleteBudget() {
        repository.deleteBudget(id: budgetId)
        dismiss()
    }
}

// MARK: - Budget Color Helper

extension CategoryBudget {
    var colorHexColor: Color {
        Color(hex: categoryColorHex) ?? CentwiseColors.primaryEmerald
    }
}
