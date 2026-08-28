import SwiftUI

public struct HomeScreen: View {
    public var onSeeAllTransactions: (() -> Void)?

    @StateObject private var viewModel = HomeViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared

    @State private var presentedSheet: TransactionSheet?
    @State private var toastItem: ToastItem?

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
                    presentedSheet = .add
                } label: {
                    Label("Add Transaction", systemImage: "plus")
                }
            }
        }
        .toast(item: $toastItem)
        .sheet(item: $presentedSheet) { sheet in
            switch sheet {
            case .add:
                AddEditTransactionView {
                    viewModel.loadHome()
                    toastItem = ToastItem("Transaction added successfully", style: .success)
                }
            case .detail(let transaction):
                TransactionDetailSheet(
                    transaction: transaction,
                    onEdit: { presentEdit(afterDismissing: transaction) },
                    onDelete: {
                        TransactionRepository.shared.deleteTransaction(id: transaction.id)
                        presentedSheet = nil
                        toastItem = ToastItem("Transaction deleted", style: .success)
                    }
                )
            case .edit(let transaction):
                AddEditTransactionView(transactionToEdit: transaction) {
                    viewModel.loadHome()
                    toastItem = ToastItem("Transaction updated successfully", style: .success)
                }
            case .export:
                EmptyView()
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
                VStack(spacing: 8) {
                    ForEach(viewModel.recentTransactions) { transaction in
                        TransactionRow(
                            transaction: transaction,
                            showChevron: true,
                            onTap: { presentedSheet = .detail(transaction) }
                        )
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(uiColor: .secondarySystemGroupedBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                }
            }
        }
    }

    private func presentEdit(afterDismissing transaction: CentwiseTransaction) {
        presentedSheet = nil
        DispatchQueue.main.async {
            presentedSheet = .edit(transaction)
        }
    }
}
