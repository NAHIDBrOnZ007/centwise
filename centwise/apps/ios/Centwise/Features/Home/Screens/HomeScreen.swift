import SwiftUI

public struct HomeScreen: View {
    public var onSeeAllTransactions: (() -> Void)? = nil
    public var onSeeAllAccounts: (() -> Void)? = nil
    public var onSeeAllBudgets: (() -> Void)? = nil

    @StateObject private var viewModel = HomeViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    @State private var showAddTransaction = false
    @State private var selectedTransactionDetail: CentwiseTransaction?
    @State private var appeared = false

    public init(
        onSeeAllTransactions: (() -> Void)? = nil,
        onSeeAllAccounts: (() -> Void)? = nil,
        onSeeAllBudgets: (() -> Void)? = nil
    ) {
        self.onSeeAllTransactions = onSeeAllTransactions
        self.onSeeAllAccounts = onSeeAllAccounts
        self.onSeeAllBudgets = onSeeAllBudgets
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                    // 1. Dynamic Greeting Header
                    GreetingCard(userName: "Tanvir", netCashflow: viewModel.monthlyNet)
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 10)

                    // 2. Spending Summary Card
                    SpendingSummaryCard(
                        monthlyExpense: viewModel.monthlyExpense,
                        monthlyIncome: viewModel.monthlyIncome,
                        monthlyNet: viewModel.monthlyNet
                    )
                    .opacity(appeared ? 1 : 0)
                    .offset(y: appeared ? 0 : 12)

                    // 3. Accounts & Wallets Carousel (bKash, Nagad, Bank accounts)
                    if !viewModel.accounts.isEmpty {
                        AccountCarousel(accounts: viewModel.accounts) { _ in
                            onSeeAllAccounts?()
                        }
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 14)
                    }

                    // 4. Monthly Budget Carousel
                    if !viewModel.budgets.isEmpty {
                        BudgetCarousel(budgets: viewModel.budgets) { _ in
                            onSeeAllBudgets?()
                        }
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 14)
                    }

                    // 5. Recent Transactions Section
                    recentTransactionsSection
                        .opacity(appeared ? 1 : 0)
                        .offset(y: appeared ? 0 : 16)
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.sm)
                .padding(.bottom, 90)
            }
            .background(CentwiseColors.background(for: colorScheme, isAmoled: isAmoled).ignoresSafeArea())
            .refreshable {
                viewModel.loadHome()
            }

            // Floating Add Button
            floatingAddButton
        }
        .onAppear {
            viewModel.loadHome()
            withAnimation(.easeOut(duration: 0.4).delay(0.1)) {
                appeared = true
            }
        }
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.loadHome()
            }
        }
        .sheet(item: $selectedTransactionDetail) { item in
            TransactionDetailSheet(transaction: item)
        }
    }

    // MARK: - Subviews
    private var recentTransactionsSection: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            HStack {
                Text("Recent Transactions")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                Spacer()

                Button("See All") {
                    themeManager.triggerHapticFeedback(.light)
                    onSeeAllTransactions?()
                }
                .font(CentwiseTypography.caption1)
                .foregroundColor(themeManager.accentColor)
            }

            if viewModel.recentTransactions.isEmpty {
                CentwiseCard {
                    HStack {
                        Spacer()
                        VStack(spacing: CentwiseSpacing.xs) {
                            Image(systemName: "tray.fill")
                                .font(.system(size: 28))
                                .foregroundColor(.secondary)
                            Text("No transactions tracked yet")
                                .font(CentwiseTypography.footnote)
                                .foregroundColor(.secondary)
                        }
                        Spacer()
                    }
                    .padding(.vertical, CentwiseSpacing.md)
                }
            } else {
                CentwiseCard {
                    ForEach(Array(viewModel.recentTransactions.enumerated()), id: \.element.id) { index, item in
                        TransactionRow(transaction: item) {
                            selectedTransactionDetail = item
                        }

                        if index < viewModel.recentTransactions.count - 1 {
                            Divider()
                        }
                    }
                }
            }
        }
    }

    private var floatingAddButton: some View {
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
}

#Preview("Home Screen Light") {
    NavigationStack {
        HomeScreen()
            .navigationTitle("Centwise")
    }
}

#Preview("Home Screen Dark") {
    NavigationStack {
        HomeScreen()
            .navigationTitle("Centwise")
    }
    .preferredColorScheme(.dark)
}
