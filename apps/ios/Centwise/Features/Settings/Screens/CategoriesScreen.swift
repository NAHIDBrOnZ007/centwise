import SwiftUI

public struct CategoriesScreen: View {
    @StateObject private var viewModel = CategoriesViewModel()
    @State private var showAddSheet = false
    @State private var editingCategory: TransactionCategory?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                categorySection("System Categories", viewModel.systemCategories, allowEditing: false)

                if viewModel.customCategories.isEmpty {
                    CentwiseCard {
                        VStack(spacing: CentwiseSpacing.sm) {
                            Image(systemName: "tag")
                                .font(.system(size: 28))
                                .foregroundColor(.secondary)
                            Text("No custom categories yet")
                                .font(CentwiseTypography.subheadline)
                                .foregroundColor(.secondary)
                            Text("Tap + to create your own category")
                                .font(CentwiseTypography.caption1)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .padding(.horizontal, CentwiseSpacing.md)
                } else {
                    categorySection("Custom Categories", viewModel.customCategories, allowEditing: true)
                }
            }
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
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

    @ViewBuilder
    private func categorySection(_ title: String, _ categories: [TransactionCategory], allowEditing: Bool) -> some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            Text(title)
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)
                .padding(.horizontal, CentwiseSpacing.md)

            VStack(spacing: CentwiseSpacing.xs) {
                ForEach(categories) { category in
                    categoryRow(category, allowEditing: allowEditing)
                        .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                            if allowEditing {
                                Button(role: .destructive) {
                                    viewModel.deleteCategory(id: category.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                        }
                }
            }
            .padding(CentwiseSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusLg, style: .continuous)
                    .fill(CentwiseColors.surface(for: colorScheme, isAmoled: themeManager.isAmoledActive))
            )
            .overlay(
                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusLg, style: .continuous)
                    .strokeBorder(CentwiseColors.border(for: colorScheme), lineWidth: 1)
            )
            .padding(.horizontal, CentwiseSpacing.md)
        }
    }

    private func categoryRow(_ category: TransactionCategory, allowEditing: Bool) -> some View {
        Button {
            if allowEditing {
                editingCategory = category
            }
        } label: {
            HStack(spacing: CentwiseSpacing.md) {
                Circle()
                    .fill(category.color.opacity(0.15))
                    .frame(width: 38, height: 38)
                    .overlay(
                        Image(systemName: category.icon)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(category.color)
                    )

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
            .padding(.horizontal, CentwiseSpacing.mdSm)
            .padding(.vertical, CentwiseSpacing.xs)
        }
        .buttonStyle(.plain)
    }
}
