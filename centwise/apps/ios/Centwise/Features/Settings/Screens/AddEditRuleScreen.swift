import SwiftUI

public struct AddEditRuleScreen: View {
    private let editingRule: SmartRule?
    private let onSave: (SmartRule) -> Void

    @State private var name: String = ""
    @State private var keyword: String = ""
    @State private var matchType: RuleMatchType = .contains
    @State private var selectedCategory: TransactionCategory = .other
    @State private var selectedType: TransactionType = .expense
    @State private var isEnabled: Bool = true

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(
        editingRule: SmartRule? = nil,
        onSave: @escaping (SmartRule) -> Void
    ) {
        self.editingRule = editingRule
        self.onSave = onSave
    }

    public var body: some View {
        Form {
            Section {
                TextField("Rule name", text: $name)
                TextField("Keyword (e.g. Foodpanda, Pathao)", text: $keyword)
                    .autocapitalization(.none)
                    .autocorrectionDisabled()
            } header: {
                Text("Rule Info")
            } footer: {
                Text("Transactions with merchant names containing this keyword will be matched.")
            }

            Section("Match Condition") {
                Picker("Condition", selection: $matchType) {
                    ForEach(RuleMatchType.allCases) { type in
                        Text(type.rawValue).tag(type)
                    }
                }
                .pickerStyle(.segmented)
            }

            Section("Assign To") {
                Picker("Category", selection: $selectedCategory) {
                    ForEach(TransactionCategory.defaultCategories) { category in
                        Text(category.name).tag(category)
                    }
                }

                Picker("Transaction Type", selection: $selectedType) {
                    ForEach(TransactionType.allCases) { type in
                        Label(type.rawValue, systemImage: type.icon).tag(type)
                    }
                }
            }

            Section {
                Toggle("Rule Enabled", isOn: $isEnabled)
                    .tint(themeManager.accentColor)
            } footer: {
                Text("Disabled rules are kept but no longer applied to new transactions.")
            }
        }
        .navigationTitle(editingRule == nil ? "New Rule" : "Edit Rule")
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
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        !keyword.trimmingCharacters(in: .whitespaces).isEmpty
    }

    private func populateFields() {
        guard let rule = editingRule, name.isEmpty else { return }
        name = rule.name
        keyword = rule.keyword
        matchType = rule.matchType
        selectedCategory = rule.category
        selectedType = rule.transactionType
        isEnabled = rule.isEnabled
    }

    private func save() {
        let rule = SmartRule(
            id: editingRule?.id ?? UUID().uuidString,
            name: name.trimmingCharacters(in: .whitespaces),
            keyword: keyword.trimmingCharacters(in: .whitespaces),
            matchType: matchType,
            category: selectedCategory,
            transactionType: selectedType,
            isEnabled: isEnabled
        )
        onSave(rule)
        dismiss()
    }
}
