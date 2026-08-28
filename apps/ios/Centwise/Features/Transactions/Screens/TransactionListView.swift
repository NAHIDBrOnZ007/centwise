import SwiftUI

enum TransactionSheet: Identifiable {
    case add
    case detail(CentwiseTransaction)
    case edit(CentwiseTransaction)
    case export

    var id: String {
        switch self {
        case .add: return "add"
        case .detail(let transaction): return "detail-\(transaction.id)"
        case .edit(let transaction): return "edit-\(transaction.id)"
        case .export: return "export"
        }
    }
}

public struct TransactionListView: View {
    @StateObject private var viewModel = TransactionsViewModel()
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var presentedSheet: TransactionSheet?

    public init() {}

    public var body: some View {
        List {
            Section {
                VStack(spacing: 10) {
                    filterBar
                    if !viewModel.filteredTransactions.isEmpty {
                        TransactionTotalsCard(
                            income: viewModel.totalIncome,
                            expense: viewModel.totalExpense,
                            net: viewModel.totalNet
                        )
                    }
                }
                .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 2, trailing: 0))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

                if viewModel.filteredTransactions.isEmpty {
                    emptyState
                        .listRowBackground(Color.clear)
                } else {
                    ForEach(Array(viewModel.groupedByMonth.enumerated()), id: \.element.key) { index, monthGroup in
                        Text(monthGroup.key)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(.secondary)
                            .textCase(.uppercase)
                            .padding(.top, index == 0 ? 8 : 14)
                            .padding(.bottom, 2)
                            .padding(.leading, 4)
                            .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)

                        ForEach(monthGroup.items) { transaction in
                            TransactionRow(
                                transaction: transaction,
                                showChevron: true,
                                onTap: { presentedSheet = .detail(transaction) }
                            )
                            .padding(.horizontal, 14)
                            .padding(.vertical, 10)
                            .background(Color(uiColor: .secondarySystemGroupedBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .listRowInsets(EdgeInsets(top: 3, leading: 0, bottom: 3, trailing: 0))
                            .listRowBackground(Color.clear)
                            .listRowSeparator(.hidden)
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    viewModel.deleteTransaction(id: transaction.id)
                                } label: {
                                    Label("Delete", systemImage: "trash")
                                }

                                Button {
                                    presentedSheet = .edit(transaction)
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
                        presentedSheet = .export
                    } label: {
                        Label("Export CSV", systemImage: "square.and.arrow.up")
                    }
                    .disabled(viewModel.filteredTransactions.isEmpty)
                } label: {
                    Label("Transaction actions", systemImage: "ellipsis.circle")
                }

                Button {
                    presentedSheet = .add
                } label: {
                    Label("Add Transaction", systemImage: "plus")
                }
            }
        }
        .onChange(of: viewModel.sortOrder) { _ in
            viewModel.applyFilters()
        }
        .sheet(item: $presentedSheet) { sheet in
            switch sheet {
            case .add:
                AddEditTransactionView {
                    viewModel.applyFilters()
                }
            case .detail(let transaction):
                TransactionDetailSheet(
                    transaction: transaction,
                    onEdit: { presentEdit(afterDismissing: transaction) },
                    onDelete: {
                        viewModel.deleteTransaction(id: transaction.id)
                        presentedSheet = nil
                    }
                )
            case .edit(let transaction):
                AddEditTransactionView(transactionToEdit: transaction) {
                    viewModel.applyFilters()
                }
            case .export:
                CsvExportSheet(transactions: viewModel.filteredTransactions)
            }
        }
    }

    private var filterBar: some View {
        HStack(spacing: 8) {
            // 1. Period Filter
            Menu {
                Picker("Period", selection: $viewModel.selectedPeriod) {
                    ForEach(DatePeriodFilter.allCases, id: \.self) { period in
                        Text(period.rawValue).tag(period)
                    }
                }
            } label: {
                filterChip(
                    title: viewModel.selectedPeriod.rawValue,
                    icon: "calendar",
                    isActive: true
                )
            }

            // 2. Type Filter
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
                filterChip(
                    title: viewModel.selectedTypeFilter?.rawValue ?? "Type",
                    icon: "line.3.horizontal.decrease",
                    isActive: viewModel.selectedTypeFilter != nil
                )
            }

            // 3. Category Filter
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
                filterChip(
                    title: selectedCategoryName,
                    icon: "slider.horizontal.3",
                    isActive: viewModel.selectedCategoryFilter != nil
                )
            }
        }
        .accessibilityLabel("Transaction filters")
    }

    private func filterChip(title: String, icon: String, isActive: Bool) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 11, weight: .semibold))
            Text(title)
                .font(.system(size: 12, weight: .medium))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Image(systemName: "chevron.down")
                .font(.system(size: 8, weight: .bold))
                .opacity(0.8)
        }
        .foregroundStyle(isActive ? Color.white : Color.primary)
        .padding(.horizontal, 8)
        .padding(.vertical, 6)
        .frame(maxWidth: .infinity)
        .background(
            isActive
                ? themeManager.accentColor
                : Color(uiColor: .secondarySystemGroupedBackground),
            in: Capsule()
        )
        .overlay {
            if !isActive {
                Capsule()
                    .stroke(Color(uiColor: .separator).opacity(0.45), lineWidth: 1)
            }
        }
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
                presentedSheet = .add
            } label: {
                Label("Add Transaction", systemImage: "plus")
            }
            .buttonStyle(.borderedProminent)
            .tint(themeManager.accentColor)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 36)
    }

    private func presentEdit(afterDismissing transaction: CentwiseTransaction) {
        presentedSheet = nil
        DispatchQueue.main.async {
            presentedSheet = .edit(transaction)
        }
    }
}

