import SwiftUI

public struct AboutScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
    }

    private var buildNumber: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                CentwiseCard {
                    VStack(spacing: CentwiseSpacing.md) {
                        Image("AppLogo")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 84, height: 84)
                            .clipShape(RoundedRectangle(cornerRadius: CentwiseSpacing.radiusMd, style: .continuous))

                        Text("Centwise")
                            .font(CentwiseTypography.title2)
                            .foregroundColor(.primary)

                        Text("Version \(appVersion) (\(buildNumber))")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)

                        Text("Bangladesh-focused expense tracker that turns bank and MFS SMS into insights automatically.")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, CentwiseSpacing.sm)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, CentwiseSpacing.md)
                }
                .padding(.horizontal, CentwiseSpacing.md)

                CentwiseCard {
                    VStack(spacing: CentwiseSpacing.sm) {
                        infoRow(icon: "iphone.gen3", tint: CentwiseColors.transferBlue, title: "Platform", value: "iOS")
                        divider
                        infoRow(icon: "globe.asia.australia.fill", tint: CentwiseColors.incomeGreen, title: "Made for", value: "Bangladesh 🇧🇩")
                        divider
                        infoRow(icon: "lock.shield.fill", tint: CentwiseColors.nagadOrange, title: "Data storage", value: "On-device only")
                        divider
                        infoRow(icon: "banknote.fill", tint: themeManager.accentColor, title: "Currency", value: "Bangladeshi Taka (৳)")
                    }
                }
                .padding(.horizontal, CentwiseSpacing.md)

                CentwiseCard {
                    VStack(spacing: CentwiseSpacing.sm) {
                        Text("Privacy First")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)

                        Text("Centwise works fully offline. Your SMS messages, transactions, and balances never leave your device unless you create a backup yourself.")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                }
                .padding(.horizontal, CentwiseSpacing.md)

                Text("© 2026 Centwise")
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity)
            }
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("About")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Rows

    private var divider: some View {
        Divider().padding(.leading, 46)
    }

    private func infoRow(icon: String, tint: Color, title: String, value: String) -> some View {
        HStack(spacing: CentwiseSpacing.md) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(tint)
                .frame(width: 30, height: 30)

            Text(title)
                .font(CentwiseTypography.bodyMedium)
                .foregroundColor(.primary)

            Spacer()

            Text(value)
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.secondary)
        }
    }
}
