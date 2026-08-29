import SwiftUI

public struct BudgetListScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    @State private var showAddBudget = false
    @State private var editingBudget: CategoryBudget?

    public init() {}

    public var body: some View {
        List {
            // 1. Total Monthly Budget Overview Card
            if !repository.budgets.isEmpty {
                Section {
                    let totalBudget = repository.budgets.reduce(0) { $0 + $1.budgetLimit }
                    let totalSpent = repository.budgets.reduce(0) { $0 + $1.currentSpent }
                    let pct = totalBudget > 0 ? min(totalSpent / totalBudget, 1.0) : 0.0

                    VStack(alignment: .leading, spacing: 8) {
                        Text("Monthly Total Budget")
                            .font(.system(size: 12, weight: .regular))
                            .foregroundColor(.secondary)

                        HStack(alignment: .firstTextBaseline) {
                            Text(CurrencyFormatter.shared.formatBDT(totalSpent))
                                .font(.system(size: 24, weight: .bold, design: .rounded))
                                .foregroundColor(.primary)

                            Spacer()

                            Text("of " + CurrencyFormatter.shared.formatBDT(totalBudget))
                                .font(.system(size: 13, weight: .regular))
                                .foregroundColor(.secondary)
                        }

                        ProgressView(value: pct)
                            .progressViewStyle(.linear)
                            .tint(themeManager.accentColor)
                        .frame(height: 4)

                        HStack {
                            Text(String(format: "%.0f%% used", pct * 100))
                                .font(.system(size: 12, weight: .regular))
                                .foregroundColor(.secondary)

                            Spacer()

                            let remaining = max(totalBudget - totalSpent, 0)
                            Text(CurrencyFormatter.shared.formatBDT(remaining) + " remaining")
                                .font(.system(size: 12, weight: .regular))
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(14)
                    .background(Color(uiColor: .secondarySystemGroupedBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 4, trailing: 0))
                    .listRowBackground(Color.clear)
                }
            }

            // 2. Category Budgets (Individual Floating Cards)
            Section {
                if repository.budgets.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "chart.pie")
                            .font(.system(size: 40))
                            .foregroundStyle(.secondary)
                            .accessibilityHidden(true)

                        Text("No Budgets Yet")
                            .font(.headline)
                            .foregroundColor(.primary)

                        Text("Create a budget to track your spending by category.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 16)

                        Button {
                            showAddBudget = true
                        } label: {
                            HStack(spacing: 6) {
                                Image(systemName: "plus")
                                    .font(.system(size: 13, weight: .semibold))
                                Text("Create Budget")
                                    .font(.system(size: 14, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(themeManager.accentColor)
                            .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .padding(.top, 8)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
                    .listRowBackground(Color.clear)
                } else {
                    ForEach(repository.budgets) { budget in
                        ZStack {
                            NavigationLink {
                                BudgetDetailScreen(budgetId: budget.id)
                            } label: {
                                EmptyView()
                            }
                            .opacity(0)

                            budgetCard(budget)
                        }
                        .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            Button(role: .destructive) {
                                repository.deleteBudget(id: budget.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        .contextMenu {
                            Button {
                                editingBudget = budget
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }

                            Button(role: .destructive) {
                                repository.deleteBudget(id: budget.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            } header: {
                if !repository.budgets.isEmpty {
                    Text("Category Budgets")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                }
            }
        }
        .listStyle(.insetGrouped)
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
        .sheet(item: $editingBudget) { budget in
            NavigationStack {
                AddEditBudgetScreen(editingBudget: budget) { updated in
                    repository.updateBudget(updated)
                }
            }
        }
    }

    private func budgetCard(_ budget: CategoryBudget) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Image(systemName: budget.categoryIcon)
                    .font(.system(size: 17, weight: .regular))
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 24, height: 24)

                Text(budget.categoryName)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)

                Spacer()

                Text(CurrencyFormatter.shared.formatBDT(budget.currentSpent) + " / " + CurrencyFormatter.shared.formatBDT(budget.budgetLimit, compact: true))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.secondary)

                Image(systemName: "chevron.right")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(Color(uiColor: .tertiaryLabel))
                    .padding(.leading, 2)
            }

            ProgressView(value: min(max(budget.percentage, 0), 1))
                .progressViewStyle(.linear)
                .tint(themeManager.accentColor)
            .frame(height: 4)

            HStack {
                Text(String(format: "%.0f%% used", budget.percentage * 100))
                    .font(.system(size: 11, weight: .regular))
                    .foregroundColor(.secondary)

                Spacer()

                Text(CurrencyFormatter.shared.formatBDT(budget.remainingAmount) + (budget.isOverBudget ? " over budget" : " remaining"))
                    .font(.system(size: 11, weight: .regular))
                    .foregroundColor(.secondary)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}
