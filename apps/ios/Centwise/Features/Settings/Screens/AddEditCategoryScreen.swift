import SwiftUI

public struct AddEditCategoryScreen: View {
    private let editingCategory: TransactionCategory?
    private let onSave: (TransactionCategory) -> Void

    @State private var name: String = ""
    @State private var selectedIcon: String = "tag.fill"
    @State private var selectedColorHex: String = "#00A86B"
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    private static let iconChoices = [
        "fork.knife", "car.fill", "bag.fill", "bolt.fill", "antenna.radiowaves.left.and.right",
        "banknote.fill", "arrow.left.arrow.right", "cross.case.fill", "play.tv.fill",
        "book.closed.fill", "airplane", "house.fill", "cart.fill", "gift.fill",
        "creditcard.fill", "fuelpump.fill", "tshirt.fill", "tag.fill"
    ]

    private static let colorChoices = [
        "#00A86B", "#F97316", "#06B6D4", "#EC4899", "#EAB308",
        "#8B5CF6", "#10B981", "#3B82F6", "#EF4444", "#6366F1",
        "#14B8A6", "#64748B"
    ]

    public init(
        editingCategory: TransactionCategory? = nil,
        onSave: @escaping (TransactionCategory) -> Void
    ) {
        self.editingCategory = editingCategory
        self.onSave = onSave
    }

    public var body: some View {
        Form {
            Section("Name") {
                TextField("Category name", text: $name)
            }

            Section("Icon") {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: CentwiseSpacing.sm), count: 6), spacing: CentwiseSpacing.sm) {
                    ForEach(Self.iconChoices, id: \.self) { icon in
                        iconCell(icon)
                    }
                }
                .padding(.vertical, CentwiseSpacing.xs)
            }

            Section("Color") {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: CentwiseSpacing.sm), count: 6), spacing: CentwiseSpacing.sm) {
                    ForEach(Self.colorChoices, id: \.self) { hex in
                        colorCell(hex)
                    }
                }
                .padding(.vertical, CentwiseSpacing.xs)
            }

            Section {
                previewRow
            } header: {
                Text("Preview")
            } footer: {
                Text("Custom categories can be assigned to any transaction.")
            }
        }
        .navigationTitle(editingCategory == nil ? "New Category" : "Edit Category")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Save") { save() }
                    .font(CentwiseTypography.bodyMedium)
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
        .onAppear(perform: populateFields)
    }

    // MARK: - Actions

    private func populateFields() {
        guard let category = editingCategory, name.isEmpty else { return }
        name = category.name
        selectedIcon = category.icon
        selectedColorHex = category.colorHex
    }

    private func save() {
        let category = TransactionCategory(
            id: editingCategory?.id ?? UUID().uuidString,
            name: name.trimmingCharacters(in: .whitespaces),
            icon: selectedIcon,
            colorHex: selectedColorHex
        )
        onSave(category)
        dismiss()
    }

    // MARK: - Cells

    private func iconCell(_ icon: String) -> some View {
        let isSelected = icon == selectedIcon
        return Button {
            selectedIcon = icon
        } label: {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(isSelected ? .white : .primary)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(
                    RoundedRectangle(cornerRadius: CentwiseSpacing.radiusSm, style: .continuous)
                        .fill(isSelected ? themeManager.accentColor : CentwiseColors.surfaceSecondary(for: colorScheme))
                )
        }
        .buttonStyle(.plain)
    }

    private func colorCell(_ hex: String) -> some View {
        let isSelected = hex == selectedColorHex
        return Button {
            selectedColorHex = hex
        } label: {
            ZStack {
                Circle()
                    .fill(Color(hex: hex) ?? .gray)
                    .frame(height: 34)

                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }

    private var previewRow: some View {
        HStack(spacing: CentwiseSpacing.md) {
            Circle()
                .fill((Color(hex: selectedColorHex) ?? themeManager.accentColor).opacity(0.15))
                .frame(width: 40, height: 40)
                .overlay(
                    Image(systemName: selectedIcon)
                        .foregroundColor(Color(hex: selectedColorHex) ?? themeManager.accentColor)
                )

            Text(name.isEmpty ? "Category name" : name)
                .font(CentwiseTypography.bodyMedium)
                .foregroundColor(name.isEmpty ? .secondary : .primary)
        }
    }
}
