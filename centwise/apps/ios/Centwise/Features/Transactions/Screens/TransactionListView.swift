import SwiftUI

public struct TransactionListView: View {
    @StateObject private var viewModel = TransactionsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    @State private var showAddTransaction = false
    @State private var selectedTransactionDetail: CentwiseTransaction?

    public init() {}

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            VStack(spacing: 0) {
                // 1. Search Bar & Filter Chips Header
                filterHeaderView
                    .padding(.horizontal, CentwiseSpacing.md)
                    .padding(.top, CentwiseSpacing.xs)
                    .padding(.bottom, CentwiseSpacing.sm)

                // 2. Grouped Transaction List
                if viewModel.filteredTransactions.isEmpty {
                    emptyStateView
                } else {
                    List {
                        ForEach(viewModel.groupedTransactions, id: \.0) { sectionTitle, items in
                            Section(header: sectionHeader(sectionTitle)) {
                                ForEach(items) { item in
                                    TransactionRow(transaction: item) {
                                        selectedTransactionDetail = item
                                    }
                                    .listRowBackground(CentwiseColors.surface(for: colorScheme, isAmoled: isAmoled))
                                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                                        Button(role: .destructive) {
                                            viewModel.deleteTransaction(id: item.id)
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .scrollContentBackground(.hidden)
                }
            }
            .background(CentwiseColors.background(for: colorScheme, isAmoled: isAmoled).ignoresSafeArea())

            // Floating Add Button
            Button(action: {
                themeManager.triggerHapticFeedback(.medium)
                showAddTransaction = true
            }) {
                Image(systemName: "plus")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(themeManager.accentColor)
                    .clipShape(Circle())
                    .shadow(
                        color: themeManager.accentColor.opacity(0.4),
                        radius: 10,
                        x: 0,
                        y: 4
                    )
            }
            .padding(.trailing, CentwiseSpacing.md)
            .padding(.bottom, CentwiseSpacing.lg)
        }
        .navigationTitle("Transactions")
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.applyFilters()
            }
        }
        .sheet(item: $selectedTransactionDetail) { item in
            TransactionDetailSheet(transaction: item) {
                // on delete
                viewModel.deleteTransaction(id: item.id)
            }
        }
    }

    // MARK: - Subviews
    private var filterHeaderView: some View {
        VStack(spacing: CentwiseSpacing.sm) {
            // Search Field
            HStack(spacing: CentwiseSpacing.sm) {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField("Search merchant, amount, ID...", text: $viewModel.searchQuery)
                    .font(CentwiseTypography.body)

                if !viewModel.searchQuery.isEmpty {
                    Button(action: { viewModel.searchQuery = "" }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.secondary)
                    }
                }
            }
            .padding(.horizontal, CentwiseSpacing.mdSm)
            .padding(.vertical, CentwiseSpacing.sm)
            .background(CentwiseColors.surfaceSecondary(for: colorScheme))
            .cornerRadius(CentwiseSpacing.radiusMd)

            // Filter Chips Scroll
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: CentwiseSpacing.xs) {
                    StatPill(
                        title: "All",
                        color: themeManager.accentColor,
                        isSelected: viewModel.selectedTypeFilter == nil && viewModel.selectedProviderFilter == nil
                    )
                    .onTapGesture {
                        themeManager.triggerHapticFeedback(.light)
                        viewModel.selectedTypeFilter = nil
                        viewModel.selectedProviderFilter = nil
                    }

                    ForEach(TransactionType.allCases) { type in
                        StatPill(
                            title: type.rawValue,
                            icon: type.icon,
                            color: type.color,
                            isSelected: viewModel.selectedTypeFilter == type
                        )
                        .onTapGesture {
                            themeManager.triggerHapticFeedback(.light)
                            viewModel.selectedTypeFilter = (viewModel.selectedTypeFilter == type) ? nil : type
                        }
                    }

                    // Bangladesh Provider Filters
                    StatPill(
                        title: "bKash",
                        color: CentwiseColors.bKashPink,
                        isSelected: viewModel.selectedProviderFilter == .bkash
                    )
                    .onTapGesture {
                        themeManager.triggerHapticFeedback(.light)
                        viewModel.selectedProviderFilter = (viewModel.selectedProviderFilter == .bkash) ? nil : .bkash
                    }

                    StatPill(
                        title: "Nagad",
                        color: CentwiseColors.nagadOrange,
                        isSelected: viewModel.selectedProviderFilter == .nagad
                    )
                    .onTapGesture {
                        themeManager.triggerHapticFeedback(.light)
                        viewModel.selectedProviderFilter = (viewModel.selectedProviderFilter == .nagad) ? nil : .nagad
                    }
                }
            }
        }
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(CentwiseTypography.caption1)
            .foregroundColor(.secondary)
            .textCase(nil)
    }

    private var emptyStateView: some View {
        VStack(spacing: CentwiseSpacing.md) {
            Spacer()
            Circle()
                .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                .frame(width: 80, height: 80)
                .overlay(
                    Image(systemName: "doc.text.magnifyingglass")
                        .font(.system(size: 36))
                        .foregroundColor(.secondary)
                )

            Text("No transactions found")
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)

            Text("Try clearing your search query or filters")
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.secondary)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}
