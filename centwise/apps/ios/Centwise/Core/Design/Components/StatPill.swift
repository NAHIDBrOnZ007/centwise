import SwiftUI

public struct StatPill: View {
    public let title: String
    public let icon: String?
    public let color: Color
    public var isSelected: Bool

    @Environment(\.colorScheme) private var colorScheme

    public init(
        title: String,
        icon: String? = nil,
        color: Color = CentwiseColors.primaryEmerald,
        isSelected: Bool = false
    ) {
        self.title = title
        self.icon = icon
        self.color = color
        self.isSelected = isSelected
    }

    public var body: some View {
        HStack(spacing: CentwiseSpacing.xs) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: 11, weight: .semibold))
            }
            Text(title)
                .font(CentwiseTypography.caption1)
        }
        .padding(.horizontal, CentwiseSpacing.sm)
        .padding(.vertical, CentwiseSpacing.xs)
        .background(
            isSelected
                ? color
                : color.opacity(0.12)
        )
        .foregroundColor(isSelected ? .white : color)
        .cornerRadius(CentwiseSpacing.radiusFull)
    }
}
