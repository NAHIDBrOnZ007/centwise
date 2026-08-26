import SwiftUI

public struct AccountDetailScreen: View {
    private let accountId: String

    @ObservedObject private var repository = TransactionRepository.shared
    @State private var selectedTransaction: CentwiseTransaction?

    @Environment(\.colorScheme) private var colorScheme
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
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                balanceCard

                statsCard

                transactionsSection
            }
            .padding(.horizontal, CentwiseSpacing.md)
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle(account?.name ?? "Account")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $selectedTransaction) { transaction in
            TransactionDetailSheet(transaction: transaction)
        }
    }

    // MARK: - Balance Card

    private var balanceCard: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                HStack(spacing: CentwiseSpacing.md) {
                    Image(systemName: account?.provider.icon ?? "building.columns.fill")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 46, height: 46)
                        .background(
                            Circle().fill(account?.provider.brandColor ?? themeManager.accentColor)
                        )

                    VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                        Text(account?.name ?? "Account")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)

                        HStack(spacing: CentwiseSpacing.sm) {
                            if let lastFour = account?.lastFourDigits {
                                Text("••\(lastFour)")
                                    .font(CentwiseTypography.caption1)
                                    .foregroundColor(.secondary)
                            }
                            Text(account?.type.rawValue ?? "")
                                .font(CentwiseTypography.caption2)
                                .foregroundColor(account?.provider.brandColor ?? themeManager.accentColor)
                                .padding(.horizontal, CentwiseSpacing.sm)
                                .padding(.vertical, CentwiseSpacing.xxs)
                                .background(
                                    Capsule().fill((account?.provider.brandColor ?? themeManager.accentColor).opacity(0.12))
                                )
                        }
                    }

                    Spacer()
                }

                Divider()

                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text("Current Balance")
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                    Text(CurrencyFormatter.shared.formatBDT(account?.currentBalance ?? 0))
                        .font(CentwiseTypography.amountHero)
                        .foregroundColor((account?.currentBalance ?? 0) < 0 ? CentwiseColors.expenseRed : .primary)
                }
            }
        }
    }

    // MARK: - Stats Card

    private var statsCard: some View {
        let transactions = accountTransactions
        let moneyIn = transactions.filter { $0.type == .income }.reduce(0) { $0 + $1.amount }
        let moneyOut = transactions.filter { $0.type == .expense }.reduce(0) { $0 + $1.amount }

        return CentwiseCard {
            HStack(spacing: 0) {
                statColumn(
                    title: "Money In",
                    value: CurrencyFormatter.shared.formatBDT(moneyIn, compact: true),
                    color: CentwiseColors.incomeGreen,
                    icon: "arrow.down.left"
                )

                Rectangle()
                    .fill(CentwiseColors.border(for: colorScheme))
                    .frame(width: 1, height: 40)

                statColumn(
                    title: "Money Out",
                    value: CurrencyFormatter.shared.formatBDT(moneyOut, compact: true),
                    color: CentwiseColors.expenseRed,
                    icon: "arrow.up.right"
                )

                Rectangle()
                    .fill(CentwiseColors.border(for: colorScheme))
                    .frame(width: 1, height: 40)

                statColumn(
                    title: "Transactions",
                    value: "\(transactions.count)",
                    color: themeManager.accentColor,
                    icon: "list.bullet"
                )
            }
        }
    }

    private func statColumn(title: String, value: String, color: Color, icon: String) -> some View {
        VStack(spacing: CentwiseSpacing.xs) {
            Image(systemName: icon)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(color)

            Text(value)
                .font(CentwiseTypography.amountMedium)
                .foregroundColor(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Text(title)
                .font(CentwiseTypography.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Transactions

    private var transactionsSection: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            Text("Transactions")
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)

            if accountTransactions.isEmpty {
                CentwiseCard {
                    VStack(spacing: CentwiseSpacing.sm) {
                        Image(systemName: "tray")
                            .font(.system(size: 26))
                            .foregroundColor(.secondary)
                        Text("No transactions yet")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                }
            } else {
                VStack(spacing: CentwiseSpacing.xs) {
                    ForEach(accountTransactions) { transaction in
                        Button {
                            selectedTransaction = transaction
                        } label: {
                            TransactionRow(transaction: transaction)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
    }
}
