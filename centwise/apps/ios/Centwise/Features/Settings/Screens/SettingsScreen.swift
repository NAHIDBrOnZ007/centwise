import SwiftUI

public struct SettingsScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                // Section 1: Personalization (Matching Screenshot ios ui 1.jpeg)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Personalization")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                        .padding(.leading, 6)

                    VStack(spacing: 0) {
                        settingsRow(
                            systemImage: "paintbrush.fill",
                            iconColor: .orange,
                            title: "Appearance",
                            subtitle: "Theme, accent color, dark mode",
                            showDivider: true
                        )

                        settingsRow(
                            systemImage: "coloncurrencysign.circle.fill",
                            iconColor: .green,
                            title: "Currency",
                            subtitle: "Default currency for totals and new entries",
                            showDivider: false
                        )
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(red: 0.97, green: 0.98, blue: 0.98))
                    )
                }

                // Section 2: Data Management (Matching Screenshot ios ui 1.jpeg)
                VStack(alignment: .leading, spacing: 8) {
                    Text("Data Management")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                        .padding(.leading, 6)

                    VStack(spacing: 0) {
                        settingsRow(
                            systemImage: "square.grid.2x2.fill",
                            iconColor: Color(red: 0.69, green: 0.32, blue: 0.87),
                            title: "Categories",
                            subtitle: "Manage expense and income categories",
                            showDivider: true
                        )

                        settingsRow(
                            systemImage: "chart.pie.fill",
                            iconColor: .green,
                            title: "Budgets",
                            subtitle: "Set spending limits by category",
                            showDivider: true
                        )

                        settingsRow(
                            systemImage: "building.columns.fill",
                            iconColor: Color(red: 0.0, green: 0.48, blue: 1.0),
                            title: "Accounts",
                            subtitle: "Manage bank accounts and cards",
                            showDivider: true
                        )

                        settingsRow(
                            systemImage: "arrow.triangle.2.circlepath",
                            iconColor: Color(red: 0.35, green: 0.34, blue: 0.84),
                            title: "Subscriptions",
                            subtitle: "Track recurring payments",
                            showDivider: true
                        )

                        settingsRow(
                            systemImage: "sparkles",
                            iconColor: Color(red: 1.0, green: 0.18, blue: 0.33),
                            title: "Smart Rules",
                            subtitle: "Auto-categorize transactions",
                            showDivider: false
                        )
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 18, style: .continuous)
                            .fill(colorScheme == .dark ? Color(red: 0.11, green: 0.11, blue: 0.12) : Color(red: 0.97, green: 0.98, blue: 0.98))
                    )
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 8)
            .padding(.bottom, 100)
        }
        .background(colorScheme == .dark ? Color.black : Color.white)
        .navigationTitle("Settings")
    }

    @ViewBuilder
    private func settingsRow(
        systemImage: String,
        iconColor: Color,
        title: String,
        subtitle: String,
        showDivider: Bool
    ) -> some View {
        VStack(spacing: 0) {
            HStack(spacing: 14) {
                // Exact Bare SF Symbol without background box (Screenshot ios ui 1.jpeg)
                Image(systemName: systemImage)
                    .font(.system(size: 22))
                    .foregroundColor(iconColor)
                    .frame(width: 28, height: 28)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(Color(white: 0.75))
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 13)

            if showDivider {
                Divider()
                    .padding(.leading, 58)
            }
        }
    }
}
