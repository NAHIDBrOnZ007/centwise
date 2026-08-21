import SwiftUI

public struct ShortcutsGuideScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // Header Banner
                VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                    HStack(spacing: CentwiseSpacing.sm) {
                        Image(systemName: "bolt.badge.automatic.fill")
                            .font(.system(size: 24))
                            .foregroundColor(themeManager.accentColor)
                        Text("iOS Auto-Tracking")
                            .font(CentwiseTypography.title2)
                            .foregroundColor(.primary)
                    }

                    Text("Because iOS restricts apps from directly reading your SMS inbox, Centwise uses Apple Shortcuts Message Automation to parse your Bangladesh bank and MFS transactions silently in the background.")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.xs)

                // 3 Step Guide
                VStack(spacing: CentwiseSpacing.md) {
                    stepCard(
                        stepNumber: 1,
                        title: "Open Apple Shortcuts",
                        description: "Open the built-in Shortcuts app on your iPhone and tap on the 'Automation' tab at the bottom.",
                        icon: "square.stack.3d.up.fill"
                    )

                    stepCard(
                        stepNumber: 2,
                        title: "Create 'Message' Automation",
                        description: "Select 'Message', set Message Contains to 'Tk, bKash, Nagad, Rocket, Cellfin, Citytouch', and toggle 'Run Immediately' ON.",
                        icon: "message.fill"
                    )

                    stepCard(
                        stepNumber: 3,
                        title: "Add Centwise Action",
                        description: "Choose the action 'Parse SMS Transaction' from Centwise and pass the Shortcut Message Input.",
                        icon: "bolt.fill"
                    )
                }
                .padding(.horizontal, CentwiseSpacing.md)

                // Supported Provider Keywords
                CentwiseCard {
                    VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                        Text("Supported Bangladesh Providers")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)

                        Text("bKash, Nagad, Rocket, Upay, Cellfin, City Bank, BRAC Bank, Eastern Bank, Dutch-Bangla Bank.")
                            .font(CentwiseTypography.footnote)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, CentwiseSpacing.md)
            }
            .padding(.bottom, 80)
        }
        .background(CentwiseColors.background(for: colorScheme).ignoresSafeArea())
        .navigationTitle("Shortcuts Setup")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func stepCard(stepNumber: Int, title: String, description: String, icon: String) -> some View {
        CentwiseCard {
            HStack(alignment: .top, spacing: CentwiseSpacing.mdSm) {
                Circle()
                    .fill(themeManager.accentColor.opacity(0.15))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text("\(stepNumber)")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(themeManager.accentColor)
                    )

                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text(title)
                        .font(CentwiseTypography.bodyMedium)
                        .foregroundColor(.primary)

                    Text(description)
                        .font(CentwiseTypography.footnote)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, CentwiseSpacing.xs)
        }
    }
}
