import SwiftUI

public struct AccountListScreen: View {
    @StateObject private var viewModel = AccountsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var showAddAccount = false
    @State private var editingAccount: FinancialAccount?

    public init() {}

    public var body: some View {
        List {
            // 1. Hero Total Balance Card
            Section {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Total Connected Balance")
                        .font(.system(size: 12, weight: .regular))
                        .foregroundColor(.secondary)

                    Text(CurrencyFormatter.shared.formatBDT(viewModel.totalBalance, showSign: false))
                        .font(.system(size: 26, weight: .bold, design: .rounded))
                        .foregroundColor(.primary)

                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.shield.fill")
                            .foregroundColor(CentwiseColors.incomeGreen)
                            .font(.system(size: 11))
                        Text("\(viewModel.accounts.count) account\(viewModel.accounts.count == 1 ? "" : "s") • Stored 100% on device")
                            .font(.system(size: 11, weight: .regular))
                            .foregroundColor(.secondary)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, 12)
                .padding(.horizontal, 14)
                .background(Color(uiColor: .secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 4, trailing: 0))
                .listRowBackground(Color.clear)
            }

            // 2. Wallets & Bank Accounts Section
            Section {
                if viewModel.accounts.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "building.columns")
                            .font(.system(size: 40))
                            .foregroundStyle(.secondary)
                            .accessibilityHidden(true)

                        Text("No accounts yet")
                            .font(.headline)
                            .foregroundColor(.primary)

                        Text("Tap + to add your bank account or mobile wallet.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                    .listRowBackground(Color.clear)
                } else {
                    ForEach(viewModel.accounts) { account in
                        ZStack {
                            NavigationLink {
                                AccountDetailScreen(accountId: account.id)
                            } label: {
                                EmptyView()
                            }
                            .opacity(0)

                            accountCard(account)
                        }
                        .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .contextMenu {
                            Button {
                                editingAccount = account
                            } label: {
                                Label("Edit", systemImage: "pencil")
                            }
                        }
                    }
                }
            } header: {
                Text("Wallets & Bank Accounts")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(.secondary)
                    .textCase(.uppercase)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Accounts")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showAddAccount = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }
        }
        .sheet(isPresented: $showAddAccount) {
            AddEditAccountScreen()
        }
        .sheet(item: $editingAccount) { account in
            AddEditAccountScreen(accountToEdit: account)
        }
    }

    private func accountCard(_ account: FinancialAccount) -> some View {
        HStack(spacing: 12) {
            Image(systemName: account.provider.icon)
                .font(.system(size: 20, weight: .regular))
                .foregroundColor(themeManager.accentColor)
                .frame(width: 28, height: 28)

            VStack(alignment: .leading, spacing: 3) {
                Text(account.name)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.primary)
                    .lineLimit(1)

                HStack(spacing: 6) {
                    if let lastFour = account.lastFourDigits {
                        Text("••\(lastFour)")
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                    }

                    AccountTypeBadge(type: account.type)
                }
            }

            Spacer()

            Text(CurrencyFormatter.shared.formatBDT(account.currentBalance, compact: true))
                .font(.system(size: 14, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundColor(Color(uiColor: .tertiaryLabel))
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }
}

public struct AccountTypeBadge: View {
    public let type: AccountType

    public init(type: AccountType) {
        self.type = type
    }

    public var body: some View {
        Text(type.rawValue)
            .font(.system(size: 10, weight: .medium))
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(Color(uiColor: .tertiarySystemFill))
            .foregroundStyle(.secondary)
            .clipShape(Capsule())
    }
}
