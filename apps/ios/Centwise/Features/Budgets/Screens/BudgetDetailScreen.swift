import SwiftUI

public struct BudgetDetailScreen: View {
    private let budgetId: String

    @ObservedObject private var repository = TransactionRepository.shared
    @State private var showingEditSheet = false
    @State private var showingDeleteAlert = false
    @State private var selectedTransaction: CentwiseTransaction?

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
        List {
            if let budget = budget {
                // 1. Unified Progress & Daily Allowance Hero Card
                Section {
                    unifiedHeroCard(budget)
                        .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 4, trailing: 0))
                        .listRowBackground(Color.clear)
                }

                // 2. Spending Transactions Section
                Section {
                    if categoryTransactions.isEmpty {
                        VStack(spacing: 12) {
                            Image(systemName: "tray")
                                .font(.system(size: 36))
                                .foregroundStyle(.secondary)
                                .accessibilityHidden(true)

                            Text("No spending recorded yet")
                                .font(.headline)
                                .foregroundColor(.primary)

                            Text("Transactions in this category will appear here.")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 28)
                        .listRowBackground(Color.clear)
                    } else {
                        ForEach(categoryTransactions) { transaction in
                            TransactionRow(
                                transaction: transaction,
                                showChevron: true,
                                onTap: {
                                    selectedTransaction = transaction
                                }
                            )
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(Color(uiColor: .secondarySystemGroupedBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                        }
                    }
                } header: {
                    Text("Spending in this Category (\(categoryTransactions.count))")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .textCase(.uppercase)
                }

                // 3. Destructive Action Section
                Section {
                    Button(role: .destructive) {
                        showingDeleteAlert = true
                    } label: {
                        HStack {
                            Spacer()
                            Text("Delete Budget")
                                .font(.system(size: 15, weight: .medium))
                            Spacer()
                        }
                    }
                }
            } else {
                Section {
                    VStack(spacing: CentwiseSpacing.sm) {
                        Image(systemName: "chart.pie")
                            .font(.title2)
                            .foregroundStyle(.secondary)
                        Text("Budget unavailable")
                        Text("This budget may have been removed.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, CentwiseSpacing.lg)
                    .listRowBackground(Color.clear)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(budget?.categoryName ?? "Budget")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showingEditSheet = true
                } label: {
                    Image(systemName: "pencil")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }
        }
        .sheet(item: $selectedTransaction) { transaction in
            TransactionDetailSheet(transaction: transaction)
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

    // MARK: - Unified Hero Card

    private func unifiedHeroCard(_ budget: CategoryBudget) -> some View {
        let daysLeft = max(daysRemainingInMonth(), 1)
        let remaining = budget.remainingAmount
        let perDay = remaining / Double(daysLeft)

        return VStack(alignment: .leading, spacing: 12) {
            // Header: Category Icon, Name & Status Badge
            HStack(spacing: 12) {
                Image(systemName: budget.categoryIcon)
                    .font(.system(size: 24, weight: .regular))
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 32, height: 32)

                VStack(alignment: .leading, spacing: 3) {
                    Text(budget.categoryName)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(budget.isOverBudget ? "Over budget" : "On track")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Color(uiColor: .tertiarySystemFill))
                        .clipShape(Capsule())
                }

                Spacer()
            }

            Divider()

            // Spending & Limit
            HStack(alignment: .firstTextBaseline) {
                Text(CurrencyFormatter.shared.formatBDT(budget.currentSpent))
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundColor(.primary)

                Spacer()

                Text("of " + CurrencyFormatter.shared.formatBDT(budget.budgetLimit))
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.secondary)
            }

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color(uiColor: .systemGray5))
                        .frame(height: 4)

                    Capsule()
                        .fill(themeManager.accentColor)
                        .frame(width: geo.size.width * CGFloat(budget.percentage), height: 4)
                }
            }
            .frame(height: 4)

            HStack {
                Text(String(format: "%.0f%% used", budget.percentage * 100))
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(.secondary)

                Spacer()

                Text(CurrencyFormatter.shared.formatBDT(budget.remainingAmount) + (budget.isOverBudget ? " over budget" : " remaining"))
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(.secondary)
            }

            Divider()

            // 3-Column Daily Allowance Stats Bar
            HStack(spacing: 0) {
                allowanceColumn(
                    title: "Daily Allowance",
                    value: CurrencyFormatter.shared.formatBDT(perDay, compact: true),
                    icon: "sun.max.fill",
                    tint: CentwiseColors.nagadOrange
                )

                Rectangle()
                    .fill(Color(uiColor: .separator).opacity(0.4))
                    .frame(width: 1, height: 32)

                allowanceColumn(
                    title: "Days Left",
                    value: "\(daysLeft)",
                    icon: "calendar",
                    tint: themeManager.accentColor
                )

                Rectangle()
                    .fill(Color(uiColor: .separator).opacity(0.4))
                    .frame(width: 1, height: 32)

                allowanceColumn(
                    title: "Spent",
                    value: CurrencyFormatter.shared.formatBDT(budget.currentSpent, compact: true),
                    icon: "arrow.up.right",
                    tint: CentwiseColors.expenseRed
                )
            }
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func allowanceColumn(title: String, value: String, icon: String, tint: Color) -> some View {
        VStack(spacing: 3) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(tint)

            Text(value)
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)

            Text(title)
                .font(.system(size: 11, weight: .regular))
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
