import SwiftUI

public struct HomeScreen: View {
    public var onSeeAllTransactions: (() -> Void)? = nil

    @StateObject private var viewModel = HomeViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var showAddTransaction = false
    @State private var selectedTransaction: CentwiseTransaction? = nil
    @State private var editingTransaction: CentwiseTransaction? = nil

    public init(onSeeAllTransactions: (() -> Void)? = nil) {
        self.onSeeAllTransactions = onSeeAllTransactions
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    // 1. User Greeting Card
                    GreetingCard()

                    // Quick Shortcuts Setup Banner (Notifies user if not configured)
                    if !profileManager.isShortcutsSetupActive && !profileManager.shortcutsSetupDismissed {
                        CentwiseCard {
                            VStack(alignment: .leading, spacing: 10) {
                                HStack {
                                    HStack(spacing: 8) {
                                        Image(systemName: "bolt.badge.automatic.fill")
                                            .foregroundColor(themeManager.accentColor)
                                        Text("Set Up Instant SMS Sync")
                                            .font(CentwiseTypography.headline)
                                            .foregroundColor(.primary)
                                    }
                                    Spacer()
                                    Button(action: {
                                        profileManager.shortcutsSetupDismissed = true
                                    }) {
                                        Image(systemName: "xmark")
                                            .font(.system(size: 12, weight: .bold))
                                            .foregroundColor(.secondary)
                                            .padding(4)
                                    }
                                    .buttonStyle(.plain)
                                }

                                Text("Centwise can instantly log your bKash, Nagad, Rocket, and bank receipts with Apple Shortcuts.")
                                    .font(CentwiseTypography.footnote)
                                    .foregroundColor(.secondary)

                                NavigationLink(destination: ShortcutsGuideScreen()) {
                                    HStack {
                                        Text("View 3-Step Setup & Test Parser")
                                            .font(CentwiseTypography.caption1)
                                            .fontWeight(.semibold)
                                        Image(systemName: "arrow.right")
                                            .font(.system(size: 12, weight: .semibold))
                                    }
                                    .foregroundColor(themeManager.accentColor)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }

                    // 2. Spent this month Card
                    SpendingSummaryCard(
                        monthlyExpense: viewModel.monthlyExpense,
                        monthlyIncome: viewModel.monthlyIncome,
                        monthlySaved: viewModel.monthlyNet
                    )

                    // 3. Accounts Section
                    if !viewModel.accounts.isEmpty {
                        AccountCarousel(accounts: viewModel.accounts)
                    }

                    // 4. Recent Transactions Header
                    HStack {
                        Text("Recent Transactions")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.primary)

                        Spacer()

                        Button("See All") {
                            themeManager.triggerHapticFeedback(.light)
                            onSeeAllTransactions?()
                        }
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                    }
                    .padding(.top, 4)

                    // 5. Recent Transactions List
                    if viewModel.recentTransactions.isEmpty {
                        emptyStateView
                    } else {
                        VStack(spacing: 10) {
                            ForEach(viewModel.recentTransactions) { tx in
                                TransactionRow(
                                    transaction: tx,
                                    showChevron: false,
                                    showMenu: true,
                                    onTap: {
                                        selectedTransaction = tx
                                    },
                                    onEdit: {
                                        editingTransaction = tx
                                    },
                                    onDelete: {
                                        TransactionRepository.shared.deleteTransaction(id: tx.id)
                                    }
                                )
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(colorScheme == .dark ? Color(white: 0.12) : Color.white)
                                .cornerRadius(16)
                                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 4, x: 0, y: 1)
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 120)
            }
            .background(colorScheme == .dark ? Color.black : Color(red: 0.98, green: 0.98, blue: 0.99))

            // Floating Blue (+) Button
            Button(action: {
                themeManager.triggerHapticFeedback(.medium)
                showAddTransaction = true
            }) {
                Image(systemName: "plus")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundColor(.white)
                    .frame(width: 56, height: 56)
                    .background(Color(red: 0.0, green: 0.48, blue: 1.0)) // iOS Blue
                    .clipShape(Circle())
                    .shadow(color: Color.blue.opacity(0.35), radius: 10, x: 0, y: 5)
            }
            .padding(.trailing, 20)
            .padding(.bottom, 90)
        }
        .navigationTitle("Centwise")
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.loadHome()
            }
        }
        .sheet(item: $selectedTransaction) { tx in
            TransactionDetailSheet(
                transaction: tx,
                onEdit: {
                    editingTransaction = tx
                },
                onDelete: {
                    TransactionRepository.shared.deleteTransaction(id: tx.id)
                }
            )
        }
        .sheet(item: $editingTransaction) { tx in
            AddEditTransactionView(transactionToEdit: tx) {
                viewModel.loadHome()
            }
        }
    }

    private var emptyStateView: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "tray")
                .font(.system(size: 56, weight: .light))
                .foregroundColor(Color(white: 0.75))
                .padding(.top, 40)

            Text("No transactions yet")
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(Color(white: 0.5))

            Text("Tap + to add your first transaction")
                .font(.system(size: 13))
                .foregroundColor(Color(white: 0.65))
            Spacer()
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
    }
}

