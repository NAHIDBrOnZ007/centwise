import SwiftUI

public enum AppTab: String, CaseIterable {
    case home = "Home"
    case transactions = "Transactions"
    case analytics = "Analytics"
    case settings = "Settings"

    public var icon: String {
        switch self {
        case .home: return "house.fill"
        case .transactions: return "list.bullet"
        case .analytics: return "chart.bar.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

public struct MainTabView: View {
    @State private var selectedTab: AppTab = .home
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        ZStack(alignment: .bottom) {
            // Main Tab Viewport
            Group {
                switch selectedTab {
                case .home:
                    NavigationStack {
                        HomeScreen(onSeeAllTransactions: { selectedTab = .transactions })
                    }
                case .transactions:
                    NavigationStack {
                        TransactionListView()
                    }
                case .analytics:
                    NavigationStack {
                        AnalyticsScreen()
                    }
                case .settings:
                    NavigationStack {
                        SettingsScreen()
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)

            // Floating Pill Tab Bar (Exact from Screenshots 1, 2, 3)
            floatingTabBar
                .padding(.horizontal, 20)
                .padding(.bottom, 16)
        }
        .ignoresSafeArea(.keyboard)
    }

    private var floatingTabBar: some View {
        HStack(spacing: 4) {
            ForEach(AppTab.allCases, id: \.self) { tab in
                tabButton(tab)
            }
        }
        .padding(6)
        .background(
            Capsule()
                .fill(colorScheme == .dark ? Color(white: 0.12).opacity(0.85) : Color.white.opacity(0.88))
                .background(
                    Capsule()
                        .fill(.ultraThinMaterial)
                )
                .shadow(color: Color.black.opacity(colorScheme == .dark ? 0.4 : 0.08), radius: 20, x: 0, y: 8)
                .overlay(
                    Capsule()
                        .stroke(colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.04), lineWidth: 1)
                )
        )
    }

    @ViewBuilder
    private func tabButton(_ tab: AppTab) -> some View {
        let isSelected = selectedTab == tab

        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
                selectedTab = tab
            }
        }) {
            VStack(spacing: 3) {
                Image(systemName: tab.icon)
                    .font(.system(size: 18, weight: .semibold))

                Text(tab.rawValue)
                    .font(.system(size: 10, weight: .semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(isSelected ? Color(red: 0.71, green: 0.36, blue: 0.46).opacity(0.12) : Color.clear)
            )
            .foregroundColor(
                isSelected
                    ? Color(red: 0.71, green: 0.36, blue: 0.46) // Mauve / Theme Accent
                    : (colorScheme == .dark ? Color(white: 0.6) : Color(white: 0.3))
            )
        }
        .buttonStyle(.plain)
    }
}
