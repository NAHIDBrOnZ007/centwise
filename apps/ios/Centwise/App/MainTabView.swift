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
    @Namespace private var tabAnimation
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared

    public init() {}

    public var body: some View {
        ZStack(alignment: .bottom) {
            if !profileManager.hasCompletedOnboarding {
                OnboardingScreen {
                    withAnimation {
                        profileManager.hasCompletedOnboarding = true
                    }
                }
                .transition(.opacity)
                .zIndex(2)
            } else {
                // Memory-efficient active tab viewport (Frees inactive tab textures and charts)
                Group {
                    switch selectedTab {
                    case .home:
                        NavigationStack {
                            HomeScreen(onSeeAllTransactions: {
                                selectTab(.transactions)
                            })
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

                // Floating Pill Tab Bar
                floatingTabBar
                    .padding(.horizontal, 20)
                    .padding(.bottom, 16)
            }
        }
        .ignoresSafeArea(.keyboard)
    }

    private func selectTab(_ tab: AppTab) {
        guard selectedTab != tab else { return }
        themeManager.triggerHapticFeedback(.selection)
        withAnimation(.easeInOut(duration: 0.18)) {
            selectedTab = tab
        }
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
            selectTab(tab)
        }) {
            VStack(spacing: 3) {
                if #available(iOS 17.0, *) {
                    Image(systemName: tab.icon)
                        .font(.system(size: 18, weight: .semibold))
                        .symbolEffect(.bounce, value: isSelected)
                        .scaleEffect(isSelected ? 1.08 : 1.0)
                } else {
                    Image(systemName: tab.icon)
                        .font(.system(size: 18, weight: .semibold))
                        .scaleEffect(isSelected ? 1.08 : 1.0)
                }

                Text(tab.rawValue)
                    .font(.system(size: 10, weight: isSelected ? .bold : .semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background {
                if isSelected {
                    Capsule()
                        .fill(themeManager.accentColor.opacity(0.14))
                        .matchedGeometryEffect(id: "activeTabCapsule", in: tabAnimation)
                }
            }
            .foregroundColor(
                isSelected
                    ? themeManager.accentColor
                    : (colorScheme == .dark ? Color(white: 0.6) : Color(white: 0.35))
            )
        }
        .buttonStyle(TabItemButtonStyle())
    }
}

private struct TabItemButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.93 : 1.0)
            .animation(.spring(response: 0.22, dampingFraction: 0.7), value: configuration.isPressed)
    }
}
