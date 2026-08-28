import SwiftUI

public enum ButtonVariant {
    case primary
    case secondary
    case destructive
    case outline
}

public struct CentwiseButton: View {
    public let title: String
    public let icon: String?
    public let variant: ButtonVariant
    public let isFullWidth: Bool
    public let action: () -> Void

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(
        _ title: String,
        icon: String? = nil,
        variant: ButtonVariant = .primary,
        isFullWidth: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.variant = variant
        self.isFullWidth = isFullWidth
        self.action = action
    }

    public var body: some View {
        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            action()
        }) {
            HStack(spacing: CentwiseSpacing.sm) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 15, weight: .semibold))
                }
                Text(title)
                    .font(CentwiseTypography.bodyMedium)
            }
            .frame(maxWidth: isFullWidth ? .infinity : nil)
            .padding(.vertical, CentwiseSpacing.mdSm)
            .padding(.horizontal, CentwiseSpacing.lg)
            .background(backgroundView)
            .foregroundColor(foregroundColor)
            .cornerRadius(CentwiseSpacing.radiusMd)
            .overlay(
                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusMd)
                    .stroke(borderColor, lineWidth: variant == .outline ? 1.5 : 0)
            )
        }
        .buttonStyle(CentwisePressStyle(reduceMotion: reduceMotion))
    }

    @ViewBuilder
    private var backgroundView: some View {
        switch variant {
        case .primary:
            themeManager.accentColor
        case .secondary:
            CentwiseColors.surfaceSecondary(for: colorScheme)
        case .destructive:
            CentwiseColors.expenseRed
        case .outline:
            Color.clear
        }
    }

    private var foregroundColor: Color {
        switch variant {
        case .primary, .destructive:
            return .white
        case .secondary, .outline:
            return colorScheme == .dark ? .white : .black
        }
    }

    private var borderColor: Color {
        switch variant {
        case .outline:
            return themeManager.accentColor
        default:
            return .clear
        }
    }
}

private struct CentwisePressStyle: ButtonStyle {
    let reduceMotion: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.72 : 1)
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.98 : 1)
            .animation(
                reduceMotion ? nil : .easeOut(duration: 0.12),
                value: configuration.isPressed
            )
    }
}
