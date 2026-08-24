import SwiftUI

public struct SettingsScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared
    @ObservedObject private var appLockManager = AppLockManager.shared
    @ObservedObject private var repository = FakeTransactionRepository.shared
    @State private var showExportSheet = false
    @State private var showAvatarPicker = false
    @State private var tempName = ""
    @State private var tempAvatar = ""
    @Environment(\.colorScheme) private var colorScheme

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.mdLg) {
                // Profile Header Card
                profileHeaderCard

                sectionHeader("Automation & Personalization")

                sectionCard {
                    navigationRow(
                        systemImage: "bolt.badge.automatic.fill",
                        iconColor: CentwiseColors.bKashPink,
                        title: "Shortcuts & SMS Auto-Tracking",
                        subtitle: "3-step setup guide and live parser tester",
                        showDivider: true
                    ) {
                        ShortcutsGuideScreen()
                    }

                    navigationRow(
                        systemImage: "paintbrush.fill",
                        iconColor: .orange,
                        title: "Appearance",
                        subtitle: themeManager.themeMode.rawValue + " theme, accent color",
                        showDivider: true
                    ) {
                        AppearanceScreen()
                    }

                    navigationRow(
                        systemImage: "coloncurrencysign.circle.fill",
                        iconColor: .green,
                        title: "Currency",
                        subtitle: "Default currency for totals and new entries",
                        showDivider: false
                    ) {
                        CurrencyPickerScreen()
                    }
                }

                sectionHeader("Data Management")

                sectionCard {
                    navigationRow(
                        systemImage: "square.grid.2x2.fill",
                        iconColor: Color(red: 0.69, green: 0.32, blue: 0.87),
                        title: "Categories",
                        subtitle: "Manage expense and income categories",
                        showDivider: true
                    ) {
                        CategoriesScreen()
                    }

                    navigationRow(
                        systemImage: "chart.pie.fill",
                        iconColor: .green,
                        title: "Budgets",
                        subtitle: "Set spending limits by category",
                        showDivider: true
                    ) {
                        BudgetListScreen()
                    }

                    navigationRow(
                        systemImage: "building.columns.fill",
                        iconColor: Color(red: 0.0, green: 0.48, blue: 1.0),
                        title: "Accounts",
                        subtitle: "Manage bank accounts and cards",
                        showDivider: true
                    ) {
                        AccountListScreen()
                    }

                    navigationRow(
                        systemImage: "arrow.triangle.2.circlepath",
                        iconColor: Color(red: 0.35, green: 0.34, blue: 0.84),
                        title: "Subscriptions",
                        subtitle: "Track recurring payments",
                        showDivider: true
                    ) {
                        SubscriptionListScreen()
                    }

                    navigationRow(
                        systemImage: "square.and.arrow.up",
                        iconColor: CentwiseColors.transferBlue,
                        title: "Export CSV",
                        subtitle: "Share all transactions as a spreadsheet",
                        showDivider: true,
                        action: { showExportSheet = true },
                        destination: { EmptyView() }
                    )

                    navigationRow(
                        systemImage: "sparkles",
                        iconColor: Color(red: 1.0, green: 0.18, blue: 0.33),
                        title: "Smart Rules",
                        subtitle: "Auto-categorize transactions",
                        showDivider: true
                    ) {
                        RulesScreen()
                    }

                    navigationRow(
                        systemImage: "tray.full.fill",
                        iconColor: CentwiseColors.transferBlue,
                        title: "Review Queue",
                        subtitle: "Review unclassified SMS messages",
                        showDivider: false
                    ) {
                        ReviewQueueView()
                    }
                }

                sectionHeader("Support & About")

                sectionCard {
                    appLockRow

                    navigationRow(
                        systemImage: "questionmark.circle.fill",
                        iconColor: CentwiseColors.transferBlue,
                        title: "FAQ",
                        subtitle: "Common questions and answers",
                        showDivider: true
                    ) {
                        FAQScreen()
                    }

                    navigationRow(
                        systemImage: "info.circle.fill",
                        iconColor: .gray,
                        title: "About Centwise",
                        subtitle: "Version, privacy, and credits",
                        showDivider: false
                    ) {
                        AboutScreen()
                    }
                }
            }
            .padding(.horizontal, CentwiseSpacing.md)
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, 100)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("Settings")
        .sheet(isPresented: $showExportSheet) {
            CsvExportSheet(transactions: repository.transactions)
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
                .navigationTitle("Edit Profile")
                .navigationBarTitleDisplayMode(.inline)
            }
        }
    }

    // MARK: - Building Blocks

    private var profileHeaderCard: some View {
        Button(action: {
            tempName = profileManager.userName
            tempAvatar = profileManager.userAvatar
            showAvatarPicker = true
        }) {
            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(themeManager.accentColor.opacity(0.18))
                        .frame(width: 60, height: 60)

                    Image(profileManager.userAvatar)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                }
                .overlay(
                    Circle()
                        .stroke(themeManager.accentColor.opacity(0.4), lineWidth: 2)
                )

                VStack(alignment: .leading, spacing: 3) {
                    Text(profileManager.userName)
                        .font(CentwiseTypography.title3)
                        .foregroundColor(.primary)

                    Text("Tap to change avatar & name")
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(themeManager.accentColor)
                }

                Spacer()

                Image(systemName: "pencil.circle.fill")
                    .font(.system(size: 24))
                    .foregroundColor(themeManager.accentColor.opacity(0.7))
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(CentwiseColors.surface(for: colorScheme, isAmoled: themeManager.isAmoledActive))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(CentwiseColors.border(for: colorScheme), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title)
            .font(CentwiseTypography.caption1)
            .foregroundColor(.secondary)
            .padding(.leading, CentwiseSpacing.xs)
    }

    private func sectionCard<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(spacing: 0) {
            content()
        }
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(CentwiseColors.surface(for: colorScheme, isAmoled: themeManager.isAmoledActive))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .strokeBorder(CentwiseColors.border(for: colorScheme), lineWidth: 1)
        )
    }

    private func navigationRow<Destination: View>(
        systemImage: String,
        iconColor: Color,
        title: String,
        subtitle: String,
        showDivider: Bool,
        action: (() -> Void)? = nil,
        @ViewBuilder destination: () -> Destination
    ) -> some View {
        VStack(spacing: 0) {
            if let action = action {
                Button {
                    action()
                } label: {
                    rowLabel(systemImage: systemImage, iconColor: iconColor, title: title, subtitle: subtitle)
                }
                .buttonStyle(.plain)
            } else {
                NavigationLink {
                    destination()
                } label: {
                    rowLabel(systemImage: systemImage, iconColor: iconColor, title: title, subtitle: subtitle)
                }
                .buttonStyle(.plain)
            }

            if showDivider {
                Divider().padding(.leading, 58)
            }
        }
    }

    private var appLockRow: some View {
        VStack(spacing: 0) {
            HStack(spacing: 14) {
                Image(systemName: appLockManager.appLockEnabled ? "lock.fill" : "lock.open.fill")
                    .font(.system(size: 22))
                    .foregroundColor(CentwiseColors.incomeGreen)
                    .frame(width: 28, height: 28)

                VStack(alignment: .leading, spacing: 2) {
                    Text("App Lock")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.primary)

                    Text(appLockManager.canUseBiometrics
                         ? "Require \(appLockManager.biometricType) to open"
                         : "Requires a device passcode")
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                }

                Spacer()

                Toggle("", isOn: $appLockManager.appLockEnabled)
                    .labelsHidden()
                    .tint(themeManager.accentColor)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 13)

            Divider().padding(.leading, 58)
        }
    }

    private func rowLabel(systemImage: String, iconColor: Color, title: String, subtitle: String) -> some View {
        HStack(spacing: 14) {
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
        .contentShape(Rectangle())
    }
}
