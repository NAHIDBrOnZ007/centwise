import SwiftUI

public struct CategoriesScreen: View {
    @StateObject private var viewModel = CategoriesViewModel()
    @State private var showAddSheet = false
    @State private var editingCategory: TransactionCategory?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        List {
            Section("System Categories") {
                ForEach(viewModel.systemCategories) { category in
                    categoryRow(category, allowEditing: false)
                }
            }

            Section("Custom Categories") {
                if viewModel.customCategories.isEmpty {
                    Label("No custom categories yet", systemImage: "tag")
                        .foregroundStyle(.secondary)
                    Text("Tap + to create your own category.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(viewModel.customCategories) { category in
                        categoryRow(category, allowEditing: true)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.deleteCategory(id: category.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Categories")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            NavigationStack {
                AddEditCategoryScreen { newCategory in
                    viewModel.addCategory(newCategory)
                }
            }
        }
        .sheet(item: $editingCategory) { category in
            NavigationStack {
                AddEditCategoryScreen(editingCategory: category) { updated in
                    viewModel.updateCategory(updated)
                }
            }
        }
    }

    // MARK: - Sections

    private func categoryRow(_ category: TransactionCategory, allowEditing: Bool) -> some View {
        Button {
            if allowEditing {
                editingCategory = category
            }
        } label: {
            HStack(spacing: CentwiseSpacing.md) {
                Image(systemName: category.icon)
                    .font(.system(size: 18, weight: .regular))
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 28, height: 28)

                Text(category.name)
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(.primary)

                Spacer()

                if allowEditing {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(Color(white: 0.7))
                } else {
                    Text("Default")
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, CentwiseSpacing.sm)
                        .padding(.vertical, CentwiseSpacing.xxs)
                        .background(
                            Capsule().fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                        )
                }
            }
            .padding(.vertical, CentwiseSpacing.xxs)
        }
        .tint(themeManager.accentColor)
    }
}
