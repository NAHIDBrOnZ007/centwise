import SwiftUI

public struct LockScreenView: View {
    public var onUnlock: (() -> Void)? = nil

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(onUnlock: (() -> Void)? = nil) {
        self.onUnlock = onUnlock
    }

    public var body: some View {
        ZStack {
            CentwiseColors.background(for: colorScheme)
                .ignoresSafeArea()

            VStack(spacing: CentwiseSpacing.xl) {
                Spacer()

                // App Logo & Shield Icon
                VStack(spacing: CentwiseSpacing.md) {
                    Circle()
                        .fill(themeManager.accentColor.opacity(0.15))
                        .frame(width: 88, height: 88)
                        .overlay(
                            Image(systemName: "lock.shield.fill")
                                .font(.system(size: 40, weight: .semibold))
                                .foregroundColor(themeManager.accentColor)
                        )

                    Text("Centwise Locked")
                        .font(CentwiseTypography.title2)
                        .foregroundColor(.primary)

                    Text("Authenticate with Face ID or Passcode to view your financial transactions")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, CentwiseSpacing.lg)
                }

                Spacer()

                CentwiseButton("Unlock with Face ID", icon: "faceid", variant: .primary, isFullWidth: true) {
                    onUnlock?()
                }
                .padding(.horizontal, CentwiseSpacing.xl)
                .padding(.bottom, CentwiseSpacing.xxl)
            }
        }
    }
}
