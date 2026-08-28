import SwiftUI

public struct HomeScreen: View {
    public var onSeeAllTransactions: (() -> Void)?

    @StateObject private var viewModel = HomeViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared

    @State private var showAddTransaction = false
    @State private var selectedTransaction: CentwiseTransaction?
    @State private var editingTransaction: CentwiseTransaction?

    public init(onSeeAllTransactions: (() -> Void)? = nil) {
        self.onSeeAllTransactions = onSeeAllTransactions
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 20) {
                GreetingCard()

                if !profileManager.isShortcutsSetupActive && !profileManager.shortcutsSetupDismissed {
                    shortcutsSetup
                }

                SpendingSummaryCard(
                    monthlyExpense: viewModel.monthlyExpense,
                    monthlyIncome: viewModel.monthlyIncome,
                    monthlySaved: viewModel.monthlyNet
                )
                .accessibilityElement(children: .combine)
                .accessibilityLabel("Monthly financial summary")

                if !viewModel.accounts.isEmpty {
                    AccountCarousel(accounts: viewModel.accounts)
                }

                recentTransactions
            }
            .padding()
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .navigationTitle("Centwise")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showAddTransaction = true
                } label: {
                    Label("Add Transaction", systemImage: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.loadHome()
            }
        }
        .sheet(item: $selectedTransaction) { transaction in
            TransactionDetailSheet(
                transaction: transaction,
                onEdit: { editingTransaction = transaction },
                onDelete: { TransactionRepository.shared.deleteTransaction(id: transaction.id) }
            )
        }
        .sheet(item: $editingTransaction) { transaction in
            AddEditTransactionView(transactionToEdit: transaction) {
                viewModel.loadHome()
            }
        }
    }

    private var shortcutsSetup: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 10) {
                Text("Capture supported financial messages through an Apple Shortcut.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)

                HStack {
                    NavigationLink("Open Setup Guide") {
                        ShortcutsGuideScreen()
                    }
                    Spacer()
                    Button("Not Now", role: .cancel) {
                        profileManager.shortcutsSetupDismissed = true
                    }
                }
                .font(.subheadline)
            }
        } label: {
            Label("Set Up SMS Capture", systemImage: "bolt.badge.clock")
                .font(.headline)
        }
    }

    private var recentTransactions: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Recent Transactions")
                    .font(.headline)
                Spacer()
                Button("See All") {
                    onSeeAllTransactions?()
                }
                .font(.subheadline)
            }

            if viewModel.recentTransactions.isEmpty {
                VStack(spacing: 10) {
                    Image(systemName: "tray")
                        .font(.largeTitle)
                        .foregroundStyle(.secondary)
                        .accessibilityHidden(true)
                    Text("No transactions yet")
                        .font(.headline)
                    Text("Add a transaction or configure SMS capture to get started.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 28)
            } else {
                VStack(spacing: 0) {
                    ForEach(Array(viewModel.recentTransactions.enumerated()), id: \.element.id) { index, transaction in
                        TransactionRow(
                            transaction: transaction,
                            showChevron: true,
                            onTap: { selectedTransaction = transaction }
                        )
                        .contextMenu {
                            Button {
                                editingTransaction = transaction
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                            Button(role: .destructive) {
                                TransactionRepository.shared.deleteTransaction(id: transaction.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }

                        if index < viewModel.recentTransactions.count - 1 {
                            Divider().padding(.leading, 56)
                        }
                    }
                }
                .padding(.horizontal)
                .background(Color(uiColor: .secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
        }
    }
}
