import SwiftUI

public enum AppTab: String, CaseIterable {
    case home = "Home"
    case transactions = "Transactions"
    case analytics = "Analytics"
    case accounts = "Wallets"
    case settings = "Settings"

    public var icon: String {
        switch self {
        case .home: return "house.fill"
        case .transactions: return "list.bullet"
        case .analytics: return "chart.pie.fill"
        case .accounts: return "creditcard.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

public struct MainTabView: View {
    @State private var selectedTab: AppTab = .home
    @ObservedObject private var themeManager = ThemeManager.shared
    @AppStorage("hasCompletedOnboarding") private var hasCompletedOnboarding: Bool = true
    @AppStorage("enableBiometricLock") private var enableBiometricLock: Bool = false
    @State private var isLocked: Bool = false
    @Environment(\.colorScheme) private var colorScheme

    public init() {}

    private var isAmoled: Bool {
        themeManager.isAmoledActive
    }

    public var body: some View {
        if hasCompletedOnboarding {
            ZStack {
                if isAmoled {
                    CentwiseColors.amoledBackground.ignoresSafeArea()
                }

                TabView(selection: $selectedTab) {
                    NavigationStack {
                        HomeScreen(
                            onSeeAllTransactions: { selectedTab = .transactions },
                            onSeeAllAccounts: { selectedTab = .accounts },
                            onSeeAllBudgets: { selectedTab = .analytics }
                        )
                        .navigationTitle("Centwise")
                    }
                    .tabItem {
                        Label(AppTab.home.rawValue, systemImage: AppTab.home.icon)
                    }
                    .tag(AppTab.home)

                    NavigationStack {
                        TransactionListView()
                    }
                    .tabItem {
                        Label(AppTab.transactions.rawValue, systemImage: AppTab.transactions.icon)
                    }
                    .tag(AppTab.transactions)

                    NavigationStack {
                        AnalyticsScreen()
                    }
                    .tabItem {
                        Label(AppTab.analytics.rawValue, systemImage: AppTab.analytics.icon)
                    }
                    .tag(AppTab.analytics)

                    NavigationStack {
                        AccountListScreen()
                    }
                    .tabItem {
                        Label(AppTab.accounts.rawValue, systemImage: AppTab.accounts.icon)
                    }
                    .tag(AppTab.accounts)

                    NavigationStack {
                        SettingsScreen()
                    }
                    .tabItem {
                        Label(AppTab.settings.rawValue, systemImage: AppTab.settings.icon)
                    }
                    .tag(AppTab.settings)
                }
                .tint(themeManager.accentColor)

                if isLocked {
                    LockScreenView {
                        withAnimation {
                            isLocked = false
                        }
                    }
                }
            }
            .environment(\.isAmoledActive, isAmoled)
            .preferredColorScheme(themeManager.colorScheme)
            .onAppear {
                if enableBiometricLock {
                    isLocked = true
                }
            }
        } else {
            OnboardingScreen {
                hasCompletedOnboarding = true
            }
            .preferredColorScheme(themeManager.colorScheme)
        }
    }
}

#Preview("Main App Flow") {
    MainTabView()
}

#Preview("Dark Mode") {
    MainTabView()
        .preferredColorScheme(.dark)
}
