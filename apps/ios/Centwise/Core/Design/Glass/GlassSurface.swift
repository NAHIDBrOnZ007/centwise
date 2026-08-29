import SwiftUI

public struct GlassCardModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled
    public var cornerRadius: CGFloat = CentwiseSpacing.radiusLg
    public var hasBorder: Bool = true
    public var elevation: CGFloat = 0

    private var backgroundStyle: AnyShapeStyle {
        isAmoled ? AnyShapeStyle(Color.black) : AnyShapeStyle(.regularMaterial)
    }

    public func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .fill(backgroundStyle)
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .stroke(
                        hasBorder ? CentwiseColors.border(for: colorScheme) : Color.clear,
                        lineWidth: 1
                    )
            )
            .shadow(
                color: colorScheme == .dark
                    ? Color.black.opacity(0.3)
                    : Color.black.opacity(0.04),
                radius: elevation > 0 ? elevation * 3 : 8,
                x: 0,
                y: elevation > 0 ? elevation : 3
            )
    }
}

extension View {
    public func glassCard(
        cornerRadius: CGFloat = CentwiseSpacing.radiusLg,
        hasBorder: Bool = true,
        elevation: CGFloat = 0
    ) -> some View {
        self.modifier(GlassCardModifier(cornerRadius: cornerRadius, hasBorder: hasBorder, elevation: elevation))
    }
}
