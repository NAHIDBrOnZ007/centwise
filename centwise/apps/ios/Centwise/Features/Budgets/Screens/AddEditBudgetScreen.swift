import SwiftUI

public struct AddEditBudgetScreen: View {
    private let editingBudget: CategoryBudget?
    private let onSave: (CategoryBudget) -> Void

    @State private var selectedCategoryId: String = TransactionCategory.food.id
    @State private var limitText: String = ""
    @State private var period: BudgetPeriod = .monthly

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    public enum BudgetPeriod: String, CaseIterable, Identifiable {
        case monthly = "Monthly"
        case weekly = "Weekly"
        case yearly = "Yearly"

        public var id: String { rawValue }
    }

    public init(
        editingBudget: CategoryBudget? = nil,
        onSave: @escaping (CategoryBudget) -> Void
    ) {
        self.editingBudget = editingBudget
        self.onSave = onSave
    }

    private var selectedCategory: TransactionCategory {
        TransactionCategory.defaultCategories.first { $0.id == selectedCategoryId } ?? .other
    }

    public var body: some View {
        Form {
            Section {
                Picker("Category", selection: $selectedCategoryId) {
                    ForEach(TransactionCategory.defaultCategories) { category in
                        Label(category.name, systemImage: category.icon)
                            .tag(category.id)
                    }
                }

                TextField("Monthly limit (৳)", text: $limitText)
                    .keyboardType(.decimalPad)
            } header: {
                Text("Budget Info")
            } footer: {
                Text("Spending in this category is tracked against the limit automatically.")
            }

            Section("Period") {
                Picker("Period", selection: $period) {
                    ForEach(BudgetPeriod.allCases) { period in
                        Text(period.rawValue).tag(period)
                    }
                }
                .pickerStyle(.segmented)
            }

            Section {
                previewRow
            } header: {
                Text("Preview")
            }
        }
        .navigationTitle(editingBudget == nil ? "New Budget" : "Edit Budget")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Save") { save() }
                    .font(CentwiseTypography.bodyMedium)
                    .disabled(!isValid)
            }
        }
        .onAppear(perform: populateFields)
    }

    // MARK: - Actions

    private var isValid: Bool {
        Double(limitText.replacingOccurrences(of: ",", with: ".")) != nil
    }

    private func populateFields() {
        guard let budget = editingBudget, limitText.isEmpty else { return }
        selectedCategoryId = budget.categoryId
        limitText = String(format: "%.0f", budget.budgetLimit)
    }

    private func save() {
        guard let limit = Double(limitText.replacingOccurrences(of: ",", with: ".")) else { return }

        let budget = CategoryBudget(
            id: editingBudget?.id ?? UUID().uuidString,
            categoryId: selectedCategory.id,
            categoryName: selectedCategory.name,
            categoryIcon: selectedCategory.icon,
            categoryColorHex: selectedCategory.colorHex,
            budgetLimit: limit,
            currentSpent: editingBudget?.currentSpent ?? 0
        )
        onSave(budget)
        dismiss()
    }

    // MARK: - Preview

    private var previewRow: some View {
        HStack(spacing: CentwiseSpacing.md) {
            Circle()
                .fill(selectedCategory.color.opacity(0.15))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: selectedCategory.icon)
                        .foregroundColor(selectedCategory.color)
                )

            VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                Text(selectedCategory.name)
                    .font(CentwiseTypography.bodyMedium)
                Text("\(period.rawValue) limit: \(CurrencyFormatter.shared.formatBDT(Double(limitText) ?? 0))")
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(.secondary)
            }
        }
    }
}
