import SwiftUI

@main
struct CentwiseApp: App {
    @StateObject private var themeManager = ThemeManager.shared

    var body: some Scene {
        WindowGroup {
            MainTabView()
                .environmentObject(themeManager)
        }
    }
}
