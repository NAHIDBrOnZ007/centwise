import SwiftUI

public struct RulesScreen: View {
    @StateObject private var viewModel = RulesViewModel()
    @State private var searchText = ""
    @State private var showAddSheet = false
    @State private var editingRule: SmartRule?
    @State private var toastItem: ToastItem?

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    private var filteredGroups: [(category: TransactionCategory, rules: [SmartRule])] {
        let query = searchText.trimmingCharacters(in: .whitespaces).lowercased()
        if query.isEmpty {
            return viewModel.groupedRules
        }
        return viewModel.groupedRules.compactMap { group in
            let matching = group.rules.filter {
                $0.name.lowercased().contains(query) ||
                $0.keyword.lowercased().contains(query) ||
                group.category.name.lowercased().contains(query)
            }
            return matching.isEmpty ? nil : (category: group.category, rules: matching)
        }
    }

    public var body: some View {
        List {
            if viewModel.rules.isEmpty {
                Section {
                    emptyState
                }
            } else if filteredGroups.isEmpty {
                Section {
                    HStack {
                        Spacer()
                        VStack(spacing: 8) {
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 24))
                                .foregroundColor(.secondary)
                            Text("No rules found")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 20)
                        Spacer()
                    }
                }
            } else {
                ForEach(filteredGroups, id: \.category.id) { group in
                    Section(group.category.name) {
                        ForEach(group.rules) { rule in
                            ruleRow(rule)
                                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                    Button(role: .destructive) {
                                        viewModel.deleteRule(id: rule.id)
                                        toastItem = ToastItem("Rule deleted", style: .success)
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }

                                    Button {
                                        editingRule = rule
                                    } label: {
                                        Label("Edit", systemImage: "pencil")
                                    }
                                    .tint(themeManager.accentColor)
                                }
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .searchable(text: $searchText, prompt: "Search rules")
        .navigationTitle("Smart Rules")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button {
                        showAddSheet = true
                    } label: {
                        Label("New Rule", systemImage: "plus")
                    }

                    Button {
                        viewModel.restoreDefaultRules()
                        toastItem = ToastItem("Default smart rules restored", style: .success)
                    } label: {
                        Label("Restore Default Rules", systemImage: "arrow.clockwise")
                    }
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }
        }
        .toast(item: $toastItem)
        .sheet(isPresented: $showAddSheet) {
            NavigationStack {
                AddEditRuleScreen { rule in
                    viewModel.addRule(rule)
                    toastItem = ToastItem("Rule created successfully", style: .success)
                }
            }
        }
        .sheet(item: $editingRule) { rule in
            NavigationStack {
                AddEditRuleScreen(editingRule: rule) { updated in
                    viewModel.updateRule(updated)
                    toastItem = ToastItem("Rule updated successfully", style: .success)
                }
            }
        }
        .onAppear {
            viewModel.loadRules()
        }
    }

    // MARK: - Rule Row

    private func ruleRow(_ rule: SmartRule) -> some View {
        Button {
            editingRule = rule
        } label: {
            HStack(spacing: CentwiseSpacing.md) {
                // Category Icon with selected theme accent color
                Image(systemName: rule.category.icon)
                    .font(.system(size: 18, weight: .regular))
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 28, height: 28)

                // Title & Keyword
                VStack(alignment: .leading, spacing: 2) {
                    Text(rule.name)
                        .font(CentwiseTypography.bodyMedium)
                        .foregroundColor(.primary)

                    Text("Keyword: \(rule.keyword)")
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                }

                Spacer()

                // Enable/Disable Toggle
                Toggle("", isOn: Binding(
                    get: { rule.isEnabled },
                    set: { viewModel.toggleRule(id: rule.id, isEnabled: $0) }
                ))
                .labelsHidden()
                .tint(themeManager.accentColor)
            }
            .padding(.vertical, CentwiseSpacing.xxs)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "wand.and.stars")
                .font(.system(size: 36))
                .foregroundColor(themeManager.accentColor)
                .padding(.bottom, 2)

            Text("No rules yet")
                .font(.headline)
                .foregroundColor(.primary)

            Text("Rules auto-categorize transactions when the SMS merchant name matches a keyword.")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 16)

            VStack(spacing: 10) {
                Button {
                    viewModel.restoreDefaultRules()
                    toastItem = ToastItem("Default smart rules loaded", style: .success)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "sparkles")
                            .font(.system(size: 13, weight: .semibold))
                        Text("Load Starter Rules")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(.white)
                    .padding(.horizontal, 20)
                    .padding(.vertical, 10)
                    .background(themeManager.accentColor)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)

                Button {
                    showAddSheet = true
                } label: {
                    Text("Create Custom Rule")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundColor(themeManager.accentColor)
                }
            }
            .padding(.top, 8)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
    }
}
