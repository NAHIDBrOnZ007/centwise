import SwiftUI

public struct TransactionListView: View {
    @StateObject private var viewModel = TransactionsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var showAddTransaction = false

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
                .background(colorScheme == .dark ? Color(white: 0.12) : Color(red: 0.90, green: 0.90, blue: 0.92))
                .cornerRadius(12)

                // Filter Pills (Screenshot 1)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        // 1. Date Filter (Active Mauve)
                        HStack(spacing: 4) {
                            Image(systemName: "calendar")
                            Text("All Time")
                            Image(systemName: "chevron.down").font(.system(size: 10))
                        }
                        .font(.system(size: 13, weight: .medium))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(Color(red: 0.71, green: 0.36, blue: 0.46)) // #B55D75
                        .foregroundColor(.white)
                        .cornerRadius(999)

                        // 2. Type Filter
                        HStack(spacing: 4) {
                            Image(systemName: "line.3.horizontal")
                            Text("Type")
                            Image(systemName: "chevron.down").font(.system(size: 10))
                        }
                        .font(.system(size: 13, weight: .medium))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(colorScheme == .dark ? Color(white: 0.12) : Color(red: 0.90, green: 0.90, blue: 0.92))
                        .foregroundColor(.primary)
                        .cornerRadius(999)

                        // 3. Category Filter
                        HStack(spacing: 4) {
                            Image(systemName: "slider.horizontal.3")
                            Text("Category")
                            Image(systemName: "chevron.down").font(.system(size: 10))
                        }
                        .font(.system(size: 13, weight: .medium))
                        .padding(.horizontal, 14)
                        .padding(.vertical, 7)
                        .background(colorScheme == .dark ? Color(white: 0.12) : Color(red: 0.90, green: 0.90, blue: 0.92))
                        .foregroundColor(.primary)
                        .cornerRadius(999)
                    }
                }

                // Empty State (Screenshot 1)
                if viewModel.filteredTransactions.isEmpty {
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
                            .background(Color(red: 0.71, green: 0.36, blue: 0.46)) // Mauve Accent
                            .cornerRadius(999)
                        }
                        .padding(.top, 14)
                    }
                    .frame(maxWidth: .infinity)
                } else {
                    VStack(spacing: 8) {
                        ForEach(viewModel.filteredTransactions) { tx in
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
        .navigationTitle("Transactions")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                // Action Pill Group (+, ⇅, 📤) (Screenshot 1)
                HStack(spacing: 16) {
                    Button(action: { showAddTransaction = true }) {
                        Image(systemName: "plus")
                    }
                    Button(action: {}) {
                        Image(systemName: "arrow.up.arrow.down")
                    }
                    Button(action: {}) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(Color(red: 0.71, green: 0.36, blue: 0.46))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(colorScheme == .dark ? Color(white: 0.12) : Color(red: 0.95, green: 0.95, blue: 0.97))
                .cornerRadius(999)
            }
        }
        .sheet(isPresented: $showAddTransaction) {
            AddEditTransactionView {
                viewModel.applyFilters()
            }
        }
    }
}
