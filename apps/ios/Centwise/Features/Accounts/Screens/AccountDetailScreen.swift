import SwiftUI

public struct AccountDetailScreen: View {
    private let accountId: String

    @ObservedObject private var repository = TransactionRepository.shared
    @State private var selectedTransaction: CentwiseTransaction?
    @State private var showEditSheet = false
    @State private var showingDeleteAlert = false

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    public init(accountId: String) {
        self.accountId = accountId
    }

    private var account: FinancialAccount? {
        repository.accounts.first { $0.id == accountId }
    }

    private var accountTransactions: [CentwiseTransaction] {
        repository.transactions
            .filter { $0.accountId == accountId }
            .sorted { $0.date > $1.date }
    }

    public var body: some View {
        List {
            // 1. Unified Hero Account & Stats Card
            Section {
                heroCard
                    .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 4, trailing: 0))
                    .listRowBackground(Color.clear)
            }

            // 2. Transactions Section
            Section {
                if accountTransactions.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "tray")
                            .font(.system(size: 36))
                            .foregroundStyle(.secondary)
                            .accessibilityHidden(true)

                        Text("No transactions yet")
                            .font(.headline)
                            .foregroundColor(.primary)

                        Text("Transactions associated with this account will appear here.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 28)
                    .listRowBackground(Color.clear)
                } else {
                    ForEach(accountTransactions) { transaction in
                        TransactionRow(
                            transaction: transaction,
                            showChevron: true,
                            onTap: {
                                selectedTransaction = transaction
                            }
                        )
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color(uiColor: .secondarySystemGroupedBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                        .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                    }
                }
            } header: {
                Text("Transactions (\(accountTransactions.count))")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
            }

            // 3. Destructive Action Section
            Section {
                Button(role: .destructive) {
                    showingDeleteAlert = true
                } label: {
                    HStack {
                        Spacer()
                        Text("Delete Account")
                            .font(.system(size: 15, weight: .medium))
                        Spacer()
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(account?.name ?? "Account")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showEditSheet = true
                } label: {
                    Image(systemName: "pencil")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }
        }
        .sheet(item: $selectedTransaction) { transaction in
            TransactionDetailSheet(transaction: transaction)
        }
        .sheet(isPresented: $showEditSheet) {
            if let account = account {
                AddEditAccountScreen(accountToEdit: account)
            }
        }
        .alert("Delete Account?", isPresented: $showingDeleteAlert) {
            Button("Delete", role: .destructive) {
                // Delete logic
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to remove this account?")
        }
    }

    // MARK: - Hero Account & Stats Card

    private var heroCard: some View {
        let transactions = accountTransactions
        let moneyIn = transactions.filter { $0.type == .income }.reduce(0) { $0 + $1.amount }
        let moneyOut = transactions.filter { $0.type == .expense }.reduce(0) { $0 + $1.amount }

        return VStack(alignment: .leading, spacing: 12) {
            // Header: Icon, Name & Type Badge
            HStack(spacing: 12) {
                Image(systemName: account?.provider.icon ?? "building.columns")
                    .font(.system(size: 24, weight: .regular))
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 32, height: 32)

                VStack(alignment: .leading, spacing: 3) {
                    Text(account?.name ?? "Account")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.primary)

                    HStack(spacing: 6) {
                        if let lastFour = account?.lastFourDigits {
                            Text("••\(lastFour)")
                                .font(.system(size: 12))
                                .foregroundColor(.secondary)
                        }

                        if let type = account?.type {
                            AccountTypeBadge(type: type)
                        }
                    }
                }

                Spacer()
            }

            Divider()

            // Balance Section
            VStack(alignment: .leading, spacing: 3) {
                Text("Current Balance")
                    .font(.system(size: 12, weight: .regular))
                    .foregroundColor(.secondary)

                Text(CurrencyFormatter.shared.formatBDT(account?.currentBalance ?? 0))
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundColor((account?.currentBalance ?? 0) < 0 ? CentwiseColors.expenseRed : .primary)
            }

            Divider()

            // 3-Column Stats Row
            HStack(spacing: 0) {
                statColumn(
                    title: "Money In",
                    value: CurrencyFormatter.shared.formatBDT(moneyIn, compact: true),
                    color: CentwiseColors.incomeGreen,
                    icon: "arrow.down.left"
                )

                Rectangle()
                    .fill(Color(uiColor: .separator).opacity(0.4))
                    .frame(width: 1, height: 32)

                statColumn(
                    title: "Money Out",
                    value: CurrencyFormatter.shared.formatBDT(moneyOut, compact: true),
                    color: CentwiseColors.expenseRed,
                    icon: "arrow.up.right"
                )

                Rectangle()
                    .fill(Color(uiColor: .separator).opacity(0.4))
                    .frame(width: 1, height: 32)

                statColumn(
                    title: "Transactions",
                    value: "\(transactions.count)",
                    color: themeManager.accentColor,
                    icon: "list.bullet"
                )
            }
        }
        .padding(14)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func statColumn(title: String, value: String, color: Color, icon: String) -> some View {
        VStack(spacing: 3) {
            Image(systemName: icon)
                .font(.system(size: 12, weight: .semibold))
                .foregroundColor(color)

            Text(value)
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.75)

            Text(title)
                .font(.system(size: 11, weight: .regular))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}
