import SwiftUI

public struct CategoryChip: View {
    public let category: TransactionCategory
    public var isSelected: Bool
    public var action: (() -> Void)?

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(
        category: TransactionCategory,
        isSelected: Bool = false,
        action: (() -> Void)? = nil
    ) {
        self.category = category
        self.isSelected = isSelected
        self.action = action
    }

    public var body: some View {
        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            action?()
        }) {
            HStack(spacing: CentwiseSpacing.xs) {
                Image(systemName: category.icon)
                    .font(.system(size: 12, weight: .semibold))
                Text(category.name)
                    .font(CentwiseTypography.caption1)
            }
            .padding(.horizontal, CentwiseSpacing.mdSm)
            .padding(.vertical, CentwiseSpacing.sm)
            .background(
                isSelected
                    ? category.color
                    : CentwiseColors.surfaceSecondary(for: colorScheme)
            )
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(CentwiseSpacing.radiusFull)
            .overlay(
                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusFull)
                    .stroke(
                        isSelected ? category.color : CentwiseColors.border(for: colorScheme),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}
