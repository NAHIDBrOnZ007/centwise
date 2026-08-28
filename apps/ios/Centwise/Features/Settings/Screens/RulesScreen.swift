import SwiftUI

public struct RulesScreen: View {
    @StateObject private var viewModel = RulesViewModel()
    @State private var showAddSheet = false
    @State private var editingRule: SmartRule?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        List {
            if viewModel.rules.isEmpty {
                Section {
                    emptyState
                }
            } else {
                Section("\(viewModel.rules.count) rule\(viewModel.rules.count == 1 ? "" : "s")") {
                    ForEach(viewModel.rules) { rule in
                        ruleRow(rule)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button {
                                    editingRule = rule
                                } label: {
                                    Label("Edit", systemImage: "pencil")
                                }
                                .tint(themeManager.accentColor)

                                Button(role: .destructive) {
                                    viewModel.deleteRule(id: rule.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }
                            }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
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

    private func ruleRow(_ rule: SmartRule) -> some View {
        HStack(spacing: CentwiseSpacing.mdSm) {
            Image(systemName: rule.category.icon)
                .foregroundStyle(rule.category.color)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                Text(rule.name)
                    .font(.body)
                Text(rule.summary)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }

            Spacer(minLength: CentwiseSpacing.sm)

            Toggle("Enabled", isOn: Binding(
                get: { rule.isEnabled },
                set: { viewModel.toggleRule(id: rule.id, isEnabled: $0) }
            ))
            .labelsHidden()
            .tint(rule.category.color)
        }
        .padding(.vertical, CentwiseSpacing.xxs)
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 10) {
            Image(systemName: "wand.and.stars")
                .font(.system(size: 34))
                .foregroundColor(themeManager.accentColor)
                .padding(.bottom, 2)

            Text("No rules yet")
                .font(.headline)
                .foregroundColor(.primary)

            Text("Rules auto-categorize transactions when the merchant name matches a keyword.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)

            Button {
                showAddSheet = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "plus")
                        .font(.system(size: 13, weight: .semibold))
                    Text("Create Rule")
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundColor(.white)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(themeManager.accentColor)
                .clipShape(Capsule())
            }
            .buttonStyle(.plain)
            .padding(.top, 10)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }
}
