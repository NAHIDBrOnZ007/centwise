import SwiftUI

public struct TransactionListView: View {
    @StateObject private var viewModel = TransactionsViewModel()
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var showAddTransaction = false
    @State private var showExportSheet = false
    @State private var selectedTransaction: CentwiseTransaction?
    @State private var editingTransaction: CentwiseTransaction?

    public init() {}

    public var body: some View {
        List {
            if !viewModel.filteredTransactions.isEmpty {
                Section {
                    TransactionTotalsCard(
                        income: viewModel.totalIncome,
                        expense: viewModel.totalExpense,
                        net: viewModel.totalNet
                    )
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }
            }

            Section {
                filterBar
            }

            if viewModel.filteredTransactions.isEmpty {
                Section {
                    emptyState
                        .listRowBackground(Color.clear)
                }
            } else {
                ForEach(viewModel.groupedByMonth, id: \.key) { monthGroup in
                    Section(monthGroup.key) {
                        ForEach(monthGroup.items) { transaction in
                            TransactionRow(
                                transaction: transaction,
                                showChevron: true,
                                onTap: { selectedTransaction = transaction }
                            )
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.deleteTransaction(id: transaction.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }

                                Button {
                                    editingTransaction = transaction
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
        .navigationTitle("Transactions")
        .searchable(text: $viewModel.searchQuery, prompt: "Merchant, account, or reference")
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Menu {
                    Picker("Sort", selection: $viewModel.sortOrder) {
                        ForEach(TransactionSortOrder.allCases, id: \.self) { order in
                            Text(order.rawValue).tag(order)
                        }
                    }

                    Button {
                        showExportSheet = true
                    } label: {
                        Label("Export CSV", systemImage: "square.and.arrow.up")
                    }
                    .disabled(viewModel.filteredTransactions.isEmpty)
                } label: {
                    Label("Transaction actions", systemImage: "ellipsis.circle")
                }

                Button {
                    showAddTransaction = true
                } label: {
                    Label("Add Transaction", systemImage: "plus")
                }
            }
        }
        .onChange(of: viewModel.sortOrder) { _ in
            viewModel.applyFilters()
        }
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.applyFilters()
            }
        }
        .sheet(item: $selectedTransaction) { transaction in
            TransactionDetailSheet(
                transaction: transaction,
                onEdit: { editingTransaction = transaction },
                onDelete: { viewModel.deleteTransaction(id: transaction.id) }
            )
        }
        .sheet(item: $editingTransaction) { transaction in
            AddEditTransactionView(transactionToEdit: transaction) {
                viewModel.applyFilters()
            }
        }
        .sheet(isPresented: $showExportSheet) {
            CsvExportSheet(transactions: viewModel.filteredTransactions)
        }
    }

    private var filterBar: some View {
        HStack(spacing: 12) {
            Menu {
                Picker("Period", selection: $viewModel.selectedPeriod) {
                    ForEach(DatePeriodFilter.allCases, id: \.self) { period in
                        Text(period.rawValue).tag(period)
                    }
                }
            } label: {
                Label(viewModel.selectedPeriod.rawValue, systemImage: "calendar")
            }

            Menu {
                Button("All Types") {
                    viewModel.selectedTypeFilter = nil
                    viewModel.applyFilters()
                }
                ForEach(TransactionType.allCases) { type in
                    Button(type.rawValue) {
                        viewModel.selectedTypeFilter = type
                        viewModel.applyFilters()
                    }
                }
            } label: {
                Label(viewModel.selectedTypeFilter?.rawValue ?? "Type", systemImage: "arrow.up.arrow.down")
            }

            Menu {
                Button("All Categories") {
                    viewModel.selectedCategoryFilter = nil
                    viewModel.applyFilters()
                }
                ForEach(repository.categories) { category in
                    Button(category.name) {
                        viewModel.selectedCategoryFilter = category.id
                        viewModel.applyFilters()
                    }
                }
            } label: {
                Label(selectedCategoryName, systemImage: "square.grid.2x2")
            }
        }
        .font(.subheadline)
        .buttonStyle(.bordered)
        .labelStyle(.titleAndIcon)
    }

    private var selectedCategoryName: String {
        guard let id = viewModel.selectedCategoryFilter else { return "Category" }
        return repository.categories.first { $0.id == id }?.name ?? "Category"
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "tray")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text("No Transactions")
                .font(.headline)
            Text("Change the filters or add a transaction to begin tracking your money.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button {
                showAddTransaction = true
            } label: {
                Label("Add Transaction", systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
            .tint(themeManager.accentColor)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 36)
    }
}
