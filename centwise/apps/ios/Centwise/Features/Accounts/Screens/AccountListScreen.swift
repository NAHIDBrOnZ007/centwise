import SwiftUI

public struct AccountListScreen: View {
    @StateObject private var viewModel = AccountsViewModel()
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    @State private var showAddAccount = false

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // Total Net Balance Hero
                CentwiseCard {
                    VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                        Text("Total Connected Balance")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)

                        Text(CurrencyFormatter.shared.formatBDT(viewModel.totalBalance, showSign: false))
                            .font(CentwiseTypography.amountHero)
                            .foregroundColor(.primary)

                        HStack(spacing: CentwiseSpacing.xs) {
                            Image(systemName: "checkmark.shield.fill")
                                .foregroundColor(CentwiseColors.incomeGreen)
                                .font(.system(size: 12))
                            Text("Stored 100% on device")
                                .font(CentwiseTypography.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, CentwiseSpacing.xs)
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.xs)

                // Accounts & MFS Wallets List
                VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                    HStack {
                        Text("Wallets & Bank Accounts")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)
                        Spacer()
                        Button("+ Add") {
                            themeManager.triggerHapticFeedback(.light)
                            showAddAccount = true
                        }
                        .font(CentwiseTypography.bodyMedium)
                        .foregroundColor(themeManager.accentColor)
                    }
                    .padding(.horizontal, CentwiseSpacing.md)

                    CentwiseCard {
                        ForEach(Array(viewModel.accounts.enumerated()), id: \.element.id) { index, account in
                            NavigationLink {
                                AccountDetailScreen(accountId: account.id)
                            } label: {
                                HStack(spacing: CentwiseSpacing.mdSm) {
                                    Circle()
                                        .fill(account.provider.brandColor.opacity(0.15))
                                        .frame(width: 44, height: 44)
                                        .overlay(
                                            Image(systemName: account.provider.icon)
                                                .font(.system(size: 18, weight: .bold))
                                                .foregroundColor(account.provider.brandColor)
                                        )

                                    VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                                        Text(account.name)
                                            .font(CentwiseTypography.bodyMedium)
                                            .foregroundColor(.primary)

                                        HStack(spacing: 4) {
                                            Text(account.type.rawValue)
                                                .font(CentwiseTypography.caption2)
                                                .foregroundColor(.secondary)

                                            if let lastFour = account.lastFourDigits {
                                                Text("• Ending in \(lastFour)")
                                                    .font(CentwiseTypography.caption2)
                                                    .foregroundColor(.secondary)
                                            }
                                        }
                                    }

                                    Spacer()

                                    Text(CurrencyFormatter.shared.formatBDT(account.currentBalance, compact: true))
                                        .font(CentwiseTypography.amountMedium)
                                        .foregroundColor(.primary)
                                }
                                .padding(.vertical, CentwiseSpacing.xs)
                                .contentShape(Rectangle())
                            }
                            .buttonStyle(.plain)

                            if index < viewModel.accounts.count - 1 {
                                Divider()
                            }
                        }
                    }
                    .padding(.horizontal, CentwiseSpacing.md)
                }
            }
            .padding(.bottom, 80)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: isAmoled).ignoresSafeArea())
        .navigationTitle("Accounts & Wallets")
        .sheet(isPresented: $showAddAccount) {
            AddEditAccountScreen()
        }
    }
}
