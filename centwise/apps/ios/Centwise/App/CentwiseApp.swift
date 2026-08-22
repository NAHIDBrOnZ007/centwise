import SwiftUI

@main
struct CentwiseApp: App {
    @StateObject private var themeManager = ThemeManager.shared
    @StateObject private var appLockManager = AppLockManager.shared

    var body: some Scene {
        WindowGroup {
            ZStack {
                MainTabView()
                    .environmentObject(themeManager)

                if appLockManager.isLocked {
                    LockScreenView(onUnlock: {
                        appLockManager.requestUnlock()
                    })
                    .transition(.opacity)
                    .zIndex(1)
                }
            }
            .onAppear {
                if appLockManager.appLockEnabled {
                    appLockManager.lockNow()
                }
            }
        }
    }
}
