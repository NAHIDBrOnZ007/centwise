import SwiftUI

public enum SettingsDestination: Hashable {
    case shortcuts
    case appearance
    case currency
    case categories
    case budgets
    case accounts
    case subscriptions
    case smartRules
    case reviewQueue
    case dataManagement
    case faq
    case about
}

public struct SettingsScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared
    @ObservedObject private var appLockManager = AppLockManager.shared

    @State private var showAvatarPicker = false
    @State private var tempName = ""
    @State private var tempAvatar = ""

    public init() {}

    public var body: some View {
        List {
            Section {
                Button {
                    tempName = profileManager.userName
                    tempAvatar = profileManager.userAvatar
                    showAvatarPicker = true
                } label: {
                    HStack(spacing: 14) {
                        Image(profileManager.userAvatar)
                            .resizable()
                            .scaledToFit()
                            .frame(width: 52, height: 52)
                            .clipShape(Circle())
                            .accessibilityHidden(true)

                        VStack(alignment: .leading, spacing: 3) {
                            Text(profileManager.userName)
                                .font(.headline)
                                .foregroundStyle(.primary)
                            Text("Edit profile")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }

                        Spacer()
                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundStyle(.tertiary)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityHint("Change your name and avatar")
            }

            Section("Personalization") {
                settingsLink(
                    "Appearance",
                    subtitle: "Theme and accent color",
                    systemImage: "paintbrush",
                    destination: .appearance
                )
                settingsLink(
                    "Currency",
                    subtitle: "Currency for totals and new entries",
                    systemImage: "coloncurrencysign.circle",
                    destination: .currency
                )
                settingsLink(
                    "Apple Shortcuts",
                    subtitle: profileManager.isShortcutsSetupActive
                        ? "SMS logging is active"
                        : "Set up SMS transaction capture",
                    systemImage: "bolt.badge.clock",
                    destination: .shortcuts
                )
            }

            Section("Money & Automation") {
                settingsLink(
                    "Accounts",
                    subtitle: "Bank, card, mobile wallet, and Cash accounts",
                    systemImage: "building.columns",
                    destination: .accounts
                )
                settingsLink(
                    "Categories",
                    subtitle: "Expense and income categories",
                    systemImage: "square.grid.2x2",
                    destination: .categories
                )
                settingsLink(
                    "Budgets",
                    subtitle: "Category spending limits",
                    systemImage: "chart.pie",
                    destination: .budgets
                )
                settingsLink(
                    "Subscriptions",
                    subtitle: "Recurring payments",
                    systemImage: "arrow.triangle.2.circlepath",
                    destination: .subscriptions
                )
                settingsLink(
                    "Smart Rules",
                    subtitle: "Automatic transaction categorization",
                    systemImage: "wand.and.stars",
                    destination: .smartRules
                )
                settingsLink(
                    "Review Queue",
                    subtitle: "Resolve ambiguous financial messages",
                    systemImage: "tray.full",
                    destination: .reviewQueue
                )
            }

            Section("Privacy & Data") {
                Toggle(isOn: $appLockManager.appLockEnabled) {
                    settingsLabel(
                        "App Lock",
                        subtitle: appLockManager.canUseBiometrics
                            ? "Require \(appLockManager.biometricType) to open"
                            : "Requires a device passcode",
                        systemImage: appLockManager.appLockEnabled ? "lock.fill" : "lock.open"
                    )
                }
                .tint(themeManager.accentColor)

                settingsLink(
                    "Data Management",
                    subtitle: "Export, demo data, and reset",
                    systemImage: "internaldrive",
                    destination: .dataManagement
                )
            }

            Section("Support") {
                settingsLink(
                    "Frequently Asked Questions",
                    subtitle: "Help using Centwise",
                    systemImage: "questionmark.circle",
                    destination: .faq
                )
                settingsLink(
                    "About Centwise",
                    subtitle: "Version, privacy, and credits",
                    systemImage: "info.circle",
                    destination: .about
                )
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Settings")
        .navigationDestination(for: SettingsDestination.self) { destination in
            destinationView(destination)
        }
        .sheet(isPresented: $showAvatarPicker) {
            NavigationStack {
                AvatarPickerView(
                    selectedAvatar: $tempAvatar,
                    userName: $tempName,
                    onSave: {
                        profileManager.setProfile(name: tempName, avatar: tempAvatar)
                    }
                )
            }
            .presentationDetents([.medium, .large])
        }
    }

    private func settingsLink(
        _ title: String,
        subtitle: String,
        systemImage: String,
        destination: SettingsDestination
    ) -> some View {
        NavigationLink(value: destination) {
            settingsLabel(title, subtitle: subtitle, systemImage: systemImage)
        }
    }

    private func settingsLabel(
        _ title: String,
        subtitle: String,
        systemImage: String
    ) -> some View {
        Label {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundStyle(.primary)
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        } icon: {
            Image(systemName: systemImage)
                .foregroundStyle(themeManager.accentColor)
                .frame(width: 26)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private func destinationView(_ destination: SettingsDestination) -> some View {
        switch destination {
        case .shortcuts: ShortcutsGuideScreen()
        case .appearance: AppearanceScreen()
        case .currency: CurrencyPickerScreen()
        case .categories: CategoriesScreen()
        case .budgets: BudgetListScreen()
        case .accounts: AccountListScreen()
        case .subscriptions: SubscriptionListScreen()
        case .smartRules: RulesScreen()
        case .reviewQueue: ReviewQueueView()
        case .dataManagement: DataManagementScreen()
        case .faq: FAQScreen()
        case .about: AboutScreen()
        }
    }
}
