import SwiftUI

public struct AccountCarousel: View {
    public let accounts: [FinancialAccount]
    public var onSelectAccount: ((FinancialAccount) -> Void)? = nil

    public init(
        accounts: [FinancialAccount],
        onSelectAccount: ((FinancialAccount) -> Void)? = nil
    ) {
        self.accounts = accounts
        self.onSelectAccount = onSelectAccount
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            Text("Accounts")
                .font(CentwiseTypography.headline)
                .foregroundColor(.primary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: CentwiseSpacing.md) {
                    ForEach(accounts) { account in
                        AccountCarouselCard(account: account, onSelect: {
                            onSelectAccount?(account)
                        })
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }
}

private struct AccountCarouselCard: View {
    let account: FinancialAccount
    var onSelect: (() -> Void)? = nil
    @State private var isAmountHidden = true
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    private var accountTypeIcon: String {
        switch account.type {
        case .card: return "creditcard.fill"
        case .mfs: return "iphone.gen3"
        case .bank: return "building.columns.fill"
        case .cash: return "banknote.fill"
        }
    }

    private var accountTypeLabel: String {
        switch account.type {
        case .card: return "Credit"
        case .mfs: return "Savings"
        case .bank: return "Bank"
        case .cash: return "Cash"
        }
    }

    var body: some View {
        Button(action: {
            onSelect?()
        }) {
            VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                HStack(spacing: CentwiseSpacing.xs) {
                    Image(systemName: accountTypeIcon)
                        .font(.caption2)
                        .foregroundColor(.secondary)

                    Text(accountTypeLabel)
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)
                }

                Text(account.name)
                    .font(CentwiseTypography.footnote)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
                    .lineLimit(1)

                if let lastFour = account.lastFourDigits, !lastFour.isEmpty {
                    Text("••\(lastFour)")
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)
                }

                Spacer(minLength: 8)

                HStack(spacing: 6) {
                    Text(isAmountHidden
                        ? "••••••"
                        : CurrencyFormatter.shared.formatBDT(account.currentBalance, showSign: false, compact: true))
                        .font(CentwiseTypography.amountSmall)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    Button {
                        themeManager.triggerHapticFeedback(.light)
                        withAnimation(.easeInOut(duration: 0.2)) {
                            isAmountHidden.toggle()
                        }
                    } label: {
                        Image(systemName: isAmountHidden ? "eye.slash.fill" : "eye.fill")
                            .font(.system(size: 11))
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(CentwiseSpacing.md)
            .frame(width: 156, height: 130)
            .background(colorScheme == .dark ? Color(white: 0.12) : Color(white: 0.96))
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.2 : 0.04), radius: 6, x: 0, y: 2)
        }
        .buttonStyle(.plain)
    }
}
