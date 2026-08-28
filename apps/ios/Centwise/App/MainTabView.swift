import SwiftUI

public enum AppTab: String, CaseIterable {
    case home = "Home"
    case transactions = "Transactions"
    case analytics = "Analytics"
    case settings = "Settings"

    public var icon: String {
        switch self {
        case .home: return "house"
        case .transactions: return "list.bullet.rectangle"
        case .analytics: return "chart.bar.xaxis"
        case .settings: return "gearshape"
        }
    }
}

public struct MainTabView: View {
    @State private var selectedTab: AppTab = .home
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared

    public init() {}

    public var body: some View {
        Group {
            if profileManager.hasCompletedOnboarding {
                TabView(selection: $selectedTab) {
                    NavigationStack {
                        HomeScreen {
                            selectedTab = .transactions
                        }
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
                        SettingsScreen()
                    }
                    .tabItem {
                        Label(AppTab.settings.rawValue, systemImage: AppTab.settings.icon)
                    }
                    .tag(AppTab.settings)
                }
                .tint(themeManager.accentColor)
            } else {
                OnboardingScreen {
                    profileManager.hasCompletedOnboarding = true
                }
            }
        }
    }
}
