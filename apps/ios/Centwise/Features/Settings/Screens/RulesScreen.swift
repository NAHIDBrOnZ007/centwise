import SwiftUI

public struct RulesScreen: View {
    @StateObject private var viewModel = RulesViewModel()
    @State private var showAddSheet = false
    @State private var editingRule: SmartRule?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                if viewModel.rules.isEmpty {
                    emptyState
                } else {
                    Text("\(viewModel.rules.count) rule\(viewModel.rules.count == 1 ? "" : "s")")
                        .font(CentwiseTypography.headline)
                        .foregroundColor(.primary)
                        .padding(.horizontal, CentwiseSpacing.md)

                    VStack(spacing: CentwiseSpacing.xs) {
                        ForEach(viewModel.rules) { rule in
                            ruleCard(rule)
                        }
                    }
                    .padding(.horizontal, CentwiseSpacing.md)
                }
            }
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("Smart Rules")
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
                AddEditRuleScreen { rule in
                    viewModel.addRule(rule)
                }
            }
        }
        .sheet(item: $editingRule) { rule in
            NavigationStack {
                AddEditRuleScreen(editingRule: rule) { updated in
                    viewModel.updateRule(updated)
                }
            }
        }
    }

    // MARK: - Rule Card

    private func ruleCard(_ rule: SmartRule) -> some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                HStack(spacing: CentwiseSpacing.mdSm) {
                    Circle()
                        .fill(rule.category.color.opacity(0.15))
                        .frame(width: 38, height: 38)
                        .overlay(
                            Image(systemName: rule.category.icon)
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(rule.category.color)
                        )

                    VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                        Text(rule.name)
                            .font(CentwiseTypography.bodyMedium)
                            .foregroundColor(.primary)

                        Text(rule.summary)
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }

                    Spacer()

                    Toggle("", isOn: Binding(
                        get: { rule.isEnabled },
                        set: { viewModel.toggleRule(id: rule.id, isEnabled: $0) }
                    ))
                    .labelsHidden()
                    .tint(rule.category.color)
                }

                HStack(spacing: CentwiseSpacing.sm) {
                    Label(rule.transactionType.rawValue, systemImage: rule.transactionType.icon)
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(rule.transactionType.color)

                    Spacer()

                    Button {
                        editingRule = rule
                    } label: {
                        Image(systemName: "slider.horizontal.3")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)

                    Button {
                        withAnimation {
                            viewModel.deleteRule(id: rule.id)
                        }
                    } label: {
                        Image(systemName: "trash")
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(CentwiseColors.expenseRed)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Empty State

    private var emptyState: some View {
        CentwiseCard {
            VStack(spacing: CentwiseSpacing.sm) {
                Image(systemName: "wand.and.stars")
                    .font(.system(size: 32))
                    .foregroundColor(themeManager.accentColor)

                Text("No rules yet")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                Text("Rules auto-categorize transactions when the merchant name matches a keyword.")
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                CentwiseButton("Create Rule", icon: "plus", isFullWidth: true) {
                    showAddSheet = true
                }
                .padding(.top, CentwiseSpacing.xs)
            }
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, CentwiseSpacing.md)
    }
}
