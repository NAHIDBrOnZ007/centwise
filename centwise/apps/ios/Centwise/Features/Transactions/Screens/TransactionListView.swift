import SwiftUI

public struct TransactionListView: View {
    @StateObject private var viewModel = TransactionsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled
    @State private var showAddTransaction = false
    @State private var showExportSheet = false
    @State private var selectedTransaction: CentwiseTransaction? = nil
    @State private var editingTransaction: CentwiseTransaction? = nil

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                // Search Bar Capsule
                HStack(spacing: 8) {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search transactions", text: $viewModel.searchQuery)
                        .font(.system(size: 15))
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(colorScheme == .dark ? Color(white: 0.12) : Color(white: 0.94))
                .cornerRadius(12)

                // Filter Dropdown Chips (Period, Type, Category)
                filterBar

                // Totals Summary Card (3 Columns: Income, Expenses, Net)
                if !viewModel.filteredTransactions.isEmpty {
                    TransactionTotalsCard(
                        income: viewModel.totalIncome,
                        expense: viewModel.totalExpense,
                        net: viewModel.totalNet
                    )
                }

                // Empty State or Grouped Monthly Transactions
                if viewModel.filteredTransactions.isEmpty {
                    emptyState
                } else {
                    LazyVStack(alignment: .leading, spacing: 20) {
                        ForEach(viewModel.groupedByMonth, id: \.key) { monthGroup in
                            VStack(alignment: .leading, spacing: 8) {
                                Text(monthGroup.key)
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundColor(.secondary)
                                    .padding(.top, 4)

                                ForEach(Array(monthGroup.items.enumerated()), id: \.element.id) { index, tx in
                                    TransactionRow(
                                        transaction: tx,
                                        showChevron: true,
                                        onTap: {
                                            selectedTransaction = tx
                                        }
                                    )

                                    if index < monthGroup.items.count - 1 {
                                        Divider()
                                            .padding(.leading, 56)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 120)
        }
        .background(colorScheme == .dark ? Color.black : Color(red: 0.98, green: 0.98, blue: 0.99))
        .navigationTitle("Transactions")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showAddTransaction = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }

            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    ForEach(TransactionSortOrder.allCases, id: \.self) { order in
                        Button {
                            themeManager.triggerHapticFeedback(.selection)
                            viewModel.sortOrder = order
                            viewModel.applyFilters()
                        } label: {
                            if viewModel.sortOrder == order {
                                Label(order.rawValue, systemImage: "checkmark")
                            } else {
                                Text(order.rawValue)
                            }
                        }
                    }
                } label: {
                    Image(systemName: "arrow.up.arrow.down")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(themeManager.accentColor)
                }
            }

            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showExportSheet = true
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(themeManager.accentColor)
                }
                .disabled(viewModel.filteredTransactions.isEmpty)
            }
        }
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.applyFilters()
            }
        }
        .sheet(item: $selectedTransaction) { tx in
            TransactionDetailSheet(
                transaction: tx,
                onEdit: {
                    editingTransaction = tx
                },
                onDelete: {
                    viewModel.deleteTransaction(id: tx.id)
                }
            )
        }
        .sheet(item: $editingTransaction) { tx in
            AddEditTransactionView(transactionToEdit: tx) {
                viewModel.applyFilters()
            }
        }
        .sheet(isPresented: $showExportSheet) {
            CsvExportSheet(transactions: viewModel.filteredTransactions)
        }
    }

    // MARK: - Filter Bar
    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                // 1. Period Dropdown Menu
                Menu {
                    ForEach(DatePeriodFilter.allCases, id: \.self) { period in
                        Button {
                            themeManager.triggerHapticFeedback(.selection)
                            viewModel.selectedPeriod = period
                            viewModel.applyFilters()
                        } label: {
                            if viewModel.selectedPeriod == period {
                                Label(period.rawValue, systemImage: "checkmark")
                            } else {
                                Text(period.rawValue)
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "calendar")
                        Text(viewModel.selectedPeriod.rawValue)
                        Image(systemName: "chevron.down").font(.system(size: 10))
                    }
                    .font(.system(size: 13, weight: .medium))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(themeManager.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(999)
                }

                // 2. Type Dropdown Menu
                Menu {
                    Button("All Types") {
                        themeManager.triggerHapticFeedback(.selection)
                        viewModel.selectedTypeFilter = nil
                        viewModel.applyFilters()
                    }
                    ForEach(TransactionType.allCases) { type in
                        Button {
                            themeManager.triggerHapticFeedback(.selection)
                            viewModel.selectedTypeFilter = type
                            viewModel.applyFilters()
                        } label: {
                            if viewModel.selectedTypeFilter == type {
                                Label(type.rawValue, systemImage: "checkmark")
                            } else {
                                Text(type.rawValue)
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "line.3.horizontal")
                        Text(viewModel.selectedTypeFilter?.rawValue ?? "Type")
                        Image(systemName: "chevron.down").font(.system(size: 10))
                    }
                    .font(.system(size: 13, weight: .medium))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(viewModel.selectedTypeFilter != nil ? themeManager.accentColor : (colorScheme == .dark ? Color(white: 0.14) : Color(white: 0.94)))
                    .foregroundColor(viewModel.selectedTypeFilter != nil ? .white : .primary)
                    .cornerRadius(999)
                }

                // 3. Category Dropdown Menu
                Menu {
                    Button("All Categories") {
                        themeManager.triggerHapticFeedback(.selection)
                        viewModel.selectedCategoryFilter = nil
                        viewModel.applyFilters()
                    }
                    ForEach(TransactionCategory.defaultCategories) { cat in
                        Button {
                            themeManager.triggerHapticFeedback(.selection)
                            viewModel.selectedCategoryFilter = cat.id
                            viewModel.applyFilters()
                        } label: {
                            if viewModel.selectedCategoryFilter == cat.id {
                                Label(cat.name, systemImage: "checkmark")
                            } else {
                                Text(cat.name)
                            }
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Image(systemName: "slider.horizontal.3")
                        Text(viewModel.selectedCategoryFilter != nil ? (TransactionCategory.defaultCategories.first { $0.id == viewModel.selectedCategoryFilter }?.name ?? "Category") : "Category")
                        Image(systemName: "chevron.down").font(.system(size: 10))
                    }
                    .font(.system(size: 13, weight: .medium))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(viewModel.selectedCategoryFilter != nil ? themeManager.accentColor : (colorScheme == .dark ? Color(white: 0.14) : Color(white: 0.94)))
                    .foregroundColor(viewModel.selectedCategoryFilter != nil ? .white : .primary)
                    .cornerRadius(999)
                }
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "tray")
                .font(.system(size: 64, weight: .light))
                .foregroundColor(Color(white: 0.75))
                .padding(.top, 60)

            Text("No Transactions Yet")
                .font(.system(size: 22, weight: .bold))
                .foregroundColor(.primary)

            Text("Add your first transaction to start tracking your finances.")
                .font(.system(size: 15))
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 24)

            Button(action: {
                themeManager.triggerHapticFeedback(.medium)
                showAddTransaction = true
            }) {
                HStack(spacing: 6) {
                    Image(systemName: "plus.circle.fill")
                    Text("Add Transaction")
                }
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.white)
                .padding(.horizontal, 28)
                .padding(.vertical, 14)
                .background(themeManager.accentColor)
                .cornerRadius(999)
            }
            .padding(.top, 14)
        }
        .frame(maxWidth: .infinity)
    }
}

