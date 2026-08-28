import SwiftUI

public struct TransactionRow: View {
    public let transaction: CentwiseTransaction
    public var showChevron: Bool = true
    public var showMenu: Bool = false
    public var onTap: (() -> Void)? = nil
    public var onEdit: (() -> Void)? = nil
    public var onDelete: (() -> Void)? = nil

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(
        transaction: CentwiseTransaction,
        showChevron: Bool = true,
        showMenu: Bool = false,
        onTap: (() -> Void)? = nil,
        onEdit: (() -> Void)? = nil,
        onDelete: (() -> Void)? = nil
    ) {
        self.transaction = transaction
        self.showChevron = showChevron
        self.showMenu = showMenu
        self.onTap = onTap
        self.onEdit = onEdit
        self.onDelete = onDelete
    }

    public var body: some View {
        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            onTap?()
        }) {
            HStack(spacing: 12) {
                // Category Icon
                Image(systemName: transaction.category.icon)
                    .font(.system(size: 20, weight: .regular))
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 28, height: 28)

                // Title & Subtitle
                VStack(alignment: .leading, spacing: 3) {
                    Text(transaction.title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    HStack(spacing: 4) {
                        Text(transaction.category.name)
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                            .lineLimit(1)

                        Text("•")
                            .font(.system(size: 10))
                            .foregroundColor(.secondary)

                        Text(transaction.date.formatted(date: .abbreviated, time: .omitted))
                            .font(.system(size: 12))
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    .lineLimit(1)
                }

                Spacer()

                // Amount
                Text(formatSignedAmount())
                    .font(.system(size: 14, weight: .bold, design: .rounded))
                    .foregroundColor(transaction.type == .income ? CentwiseColors.incomeGreen : CentwiseColors.expenseRed)

                if showChevron {
                    Image(systemName: "chevron.right")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(Color(white: 0.7))
                        .padding(.leading, 2)
                } else if showMenu {
                    Menu {
                        if let onTap {
                            Button("View Details", action: onTap)
                        }
                        if let onEdit {
                            Button("Edit", action: onEdit)
                        }
                        if let onDelete {
                            Button("Delete", role: .destructive, action: onDelete)
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                            .font(.system(size: 16))
                            .foregroundColor(.secondary)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.vertical, 4)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func formatSignedAmount() -> String {
        let prefix = transaction.type == .expense ? "-" : "+"
        return "\(prefix)\(CurrencyFormatter.shared.formatBDT(transaction.amount, showSign: false))"
    }
}

