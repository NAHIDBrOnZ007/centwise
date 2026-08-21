import SwiftUI

public struct HomeScreen: View {
    public var onSeeAllTransactions: (() -> Void)? = nil

    @StateObject private var viewModel = HomeViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var showAddTransaction = false

    public init(onSeeAllTransactions: (() -> Void)? = nil) {
        self.onSeeAllTransactions = onSeeAllTransactions
    }

    public var body: some View {
        ZStack(alignment: .bottomTrailing) {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // 1. User Greeting Card
                    GreetingCard(userName: "User", greeting: "Good night")

                    // 2. Spent this month Card
                    SpendingSummaryCard(
                        monthlyExpense: viewModel.monthlyExpense,
                        monthlyIncome: viewModel.monthlyIncome,
                        monthlySaved: viewModel.monthlyNet
                    )

                    // 3. Recent Transactions Header
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
                        .foregroundColor(Color(red: 0.71, green: 0.36, blue: 0.46)) // Mauve Accent
                    }
                    .padding(.top, 8)

                    // 4. Empty State
                    if viewModel.recentTransactions.isEmpty {
                        emptyStateView
                    } else {
                        VStack(spacing: 8) {
                            ForEach(viewModel.recentTransactions) { tx in
                                TransactionRow(transaction: tx)
                            }
                        }
                    }
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 100)
            }
            .background(colorScheme == .dark ? Color.black : Color.white)

            // Floating Blue (+) Button (Screenshot 2)
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
        .navigationTitle("PennyWise")
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
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
