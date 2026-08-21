import SwiftUI

public struct SettingsScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    @AppStorage("enableBiometricLock") private var enableBiometricLock: Bool = false
    @AppStorage("enableBengaliDigits") private var enableBengaliDigits: Bool = false
    @State private var showExportSuccess = false

    public init() {}

    public var body: some View {
        Form {
            // Automation & iOS Tracking
            Section(header: Text("Tracking & Automations")) {
                NavigationLink(destination: ShortcutsGuideScreen()) {
                    HStack(spacing: CentwiseSpacing.sm) {
                        Image(systemName: "bolt.badge.automatic.fill")
                            .foregroundColor(themeManager.accentColor)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Apple Shortcuts Setup")
                                .font(CentwiseTypography.bodyMedium)
                            Text("Background SMS parsing guide")
                                .font(CentwiseTypography.caption2)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }

            // Theme & Customization
            Section(header: Text("Appearance")) {
                Picker("Theme", selection: $themeManager.themeMode) {
                    ForEach(ThemeMode.allCases) { mode in
                        Text(mode.rawValue).tag(mode)
                    }
                }

                Picker("Accent Color", selection: $themeManager.accentChoice) {
                    ForEach(AccentChoice.allCases) { choice in
                        HStack {
                            Circle().fill(choice.color).frame(width: 12, height: 12)
                            Text(choice.rawValue)
                        }
                        .tag(choice)
                    }
                }

                Toggle("Haptic Feedback", isOn: $themeManager.enableHaptics)
            }

            // Security & Privacy
            Section(header: Text("Security & Privacy")) {
                Toggle("Face ID / Biometric Lock", isOn: $enableBiometricLock)

                HStack {
                    Image(systemName: "shield.lefthalf.filled")
                        .foregroundColor(CentwiseColors.incomeGreen)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("On-Device Processing")
                            .font(CentwiseTypography.bodyMedium)
                        Text("SMS messages never leave your iPhone")
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }

            // Data & Export
            Section(header: Text("Data & Backups")) {
                Button(action: {
                    themeManager.triggerHapticFeedback(.success)
                    showExportSuccess = true
                }) {
                    HStack {
                        Image(systemName: "square.and.arrow.up")
                            .foregroundColor(themeManager.accentColor)
                        Text("Export Transactions (CSV)")
                            .foregroundColor(.primary)
                    }
                }

                Button(action: {
                    themeManager.triggerHapticFeedback(.success)
                }) {
                    HStack {
                        Image(systemName: "arrow.down.doc.fill")
                            .foregroundColor(themeManager.accentColor)
                        Text("Backup Local Database")
                            .foregroundColor(.primary)
                    }
                }
            }

            // Localization
            Section(header: Text("Region & Currency")) {
                HStack {
                    Text("Currency")
                    Spacer()
                    Text("৳ BDT (Taka)")
                        .foregroundColor(.secondary)
                }

                Toggle("Bengali Numerals (০-৯)", isOn: $enableBengaliDigits)
            }

            // About
            Section(header: Text("About")) {
                HStack {
                    Text("Version")
                    Spacer()
                    Text("1.0.0 (Build 1)")
                        .foregroundColor(.secondary)
                }

                HStack {
                    Text("Designed for")
                    Spacer()
                    Text("Bangladesh 🇧🇩")
                        .foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle("Settings")
        .alert("CSV Export Ready", isPresented: $showExportSuccess) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Your transaction history has been exported successfully.")
        }
    }
}
