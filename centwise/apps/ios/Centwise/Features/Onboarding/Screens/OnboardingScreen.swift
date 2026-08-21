import SwiftUI

public struct OnboardingScreen: View {
    public var onComplete: () -> Void

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var currentPage = 0

    public init(onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
    }

    public var body: some View {
        ZStack {
            CentwiseColors.background(for: colorScheme)
                .ignoresSafeArea()

            VStack(spacing: CentwiseSpacing.xl) {
                // Top Skip Button
                HStack {
                    Spacer()
                    Button("Skip") {
                        onComplete()
                    }
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(.secondary)
                }
                .padding(.horizontal, CentwiseSpacing.lg)
                .padding(.top, CentwiseSpacing.md)

                Spacer()

                // Hero Graphic / Features
                TabView(selection: $currentPage) {
                    pageContent(
                        icon: "wallet.pass.fill",
                        iconColor: themeManager.accentColor,
                        title: "Track All Bangladesh MFS & Banks",
                        description: "Auto-parse bKash, Nagad, Rocket, Cellfin, and Bank SMS directly into smart expense logs."
                    )
                    .tag(0)

                    pageContent(
                        icon: "bolt.badge.automatic.fill",
                        iconColor: CentwiseColors.bKashPink,
                        title: "100% Background Automation",
                        description: "Link with Apple Shortcuts in 1-tap to track your transactions silently as SMS arrives."
                    )
                    .tag(1)

                    pageContent(
                        icon: "lock.shield.fill",
                        iconColor: CentwiseColors.incomeGreen,
                        title: "On-Device Privacy First",
                        description: "Your financial data and SMS never leave your iPhone. Zero cloud accounts required."
                    )
                    .tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .always))
                .frame(height: 380)

                Spacer()

                // Action Button
                VStack(spacing: CentwiseSpacing.sm) {
                    if currentPage < 2 {
                        CentwiseButton("Continue", variant: .primary, isFullWidth: true) {
                            withAnimation {
                                currentPage += 1
                            }
                        }
                    } else {
                        CentwiseButton("Get Started with Centwise", variant: .primary, isFullWidth: true) {
                            onComplete()
                        }
                    }
                }
                .padding(.horizontal, CentwiseSpacing.lg)
                .padding(.bottom, CentwiseSpacing.xxl)
            }
        }
    }

    @ViewBuilder
    private func pageContent(icon: String, iconColor: Color, title: String, description: String) -> some View {
        VStack(spacing: CentwiseSpacing.lg) {
            Circle()
                .fill(iconColor.opacity(0.15))
                .frame(width: 100, height: 100)
                .overlay(
                    Image(systemName: icon)
                        .font(.system(size: 46, weight: .semibold))
                        .foregroundColor(iconColor)
                )

            VStack(spacing: CentwiseSpacing.sm) {
                Text(title)
                    .font(CentwiseTypography.title2)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)

                Text(description)
                    .font(CentwiseTypography.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, CentwiseSpacing.lg)
            }
        }
        .padding(.horizontal, CentwiseSpacing.md)
    }
}
