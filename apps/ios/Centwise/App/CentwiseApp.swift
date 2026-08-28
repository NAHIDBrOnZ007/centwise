import SwiftUI

@main
struct CentwiseApp: App {
    @ObservedObject private var themeManager = ThemeManager.shared
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
            .preferredColorScheme(themeManager.colorScheme)
            .environment(\.isAmoledActive, themeManager.isAmoledActive)
            .onAppear {
                CentwiseRustBackend.initialize()
                TransactionRepository.shared.loadFromRust()
                _ = ReviewQueueRepository.shared
                CentwiseShortcuts.updateAppShortcutParameters()
                if appLockManager.appLockEnabled {
                    appLockManager.lockNow()
                }
            }
            .onOpenURL { url in
                handleIncomingURL(url)
            }
        }
    }

    private func handleIncomingURL(_ url: URL) {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true) else { return }
        
        // Match host or path: "parse-sms" or "track"
        let hostOrPath = (components.host ?? "") + (components.path)
        if hostOrPath.contains("parse-sms") || hostOrPath.contains("track") {
            if let queryItems = components.queryItems {
                let text = queryItems.first(where: { $0.name == "text" || $0.name == "body" || $0.name == "sms" })?.value
                let sender = queryItems.first(where: { $0.name == "sender" })?.value
                
                if let rawText = text, !rawText.isEmpty {
                    _ = SmsTransactionProcessor.shared.processIncomingSms(body: rawText, senderHint: sender)
                }
            }
        }
    }
}
