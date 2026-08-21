import SwiftUI

public struct AccountCarousel: View {
    public let accounts: [FinancialAccount]
    public var onSelectAccount: ((FinancialAccount) -> Void)? = nil

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(
        accounts: [FinancialAccount],
        onSelectAccount: ((FinancialAccount) -> Void)? = nil
    ) {
        self.accounts = accounts
        self.onSelectAccount = onSelectAccount
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            HStack {
                Text("Accounts & Wallets")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)
                Spacer()
                Text("\(accounts.count) Active")
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(.secondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: CentwiseSpacing.mdSm) {
                    ForEach(accounts) { account in
                        accountCard(account)
                    }
                }
                .padding(.vertical, CentwiseSpacing.xs)
            }
        }
    }

    @ViewBuilder
    private func accountCard(_ account: FinancialAccount) -> some View {
        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            onSelectAccount?(account)
        }) {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                // Top: Provider Icon & Badge
                HStack {
                    Circle()
                        .fill(account.provider.brandColor.opacity(0.15))
                        .frame(width: 32, height: 32)
                        .overlay(
                            Image(systemName: account.provider.icon)
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(account.provider.brandColor)
                        )

                    Spacer()

                    if let lastFour = account.lastFourDigits {
                        Text("••\(lastFour)")
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, CentwiseSpacing.xs)
                            .padding(.vertical, 2)
                            .background(CentwiseColors.surfaceSecondary(for: colorScheme))
                            .cornerRadius(CentwiseSpacing.radiusSm)
                    }
                }

                Spacer(minLength: 4)

                // Bottom: Name & Balance
                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text(account.name)
                        .font(CentwiseTypography.footnote)
                        .foregroundColor(.secondary)
                        .lineLimit(1)

                    Text(CurrencyFormatter.shared.formatBDT(account.currentBalance, showSign: false, compact: true))
                        .font(CentwiseTypography.amountMedium)
                        .foregroundColor(.primary)
                }
            }
            .frame(width: 148, height: 112)
            .padding(CentwiseSpacing.mdSm)
            .glassCard(cornerRadius: CentwiseSpacing.radiusMd)
        }
        .buttonStyle(.plain)
    }
}
