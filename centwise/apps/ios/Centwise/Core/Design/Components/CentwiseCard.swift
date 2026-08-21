import SwiftUI

public struct CentwiseCard<Content: View>: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled
    public var cornerRadius: CGFloat
    public var padding: CGFloat
    public var hasBorder: Bool
    public var content: () -> Content

    public init(
        cornerRadius: CGFloat = CentwiseSpacing.radiusLg,
        padding: CGFloat = CentwiseSpacing.md,
        hasBorder: Bool = true,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.hasBorder = hasBorder
        self.content = content
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            content()
        }
        .padding(padding)
        .glassCard(cornerRadius: cornerRadius, hasBorder: hasBorder)
    }
}
