import SwiftUI
import UIKit

@main
struct CentwiseApp: App {
    @ObservedObject private var themeManager = ThemeManager.shared
    @StateObject private var appLockManager = AppLockManager.shared

    var body: some Scene {
        WindowGroup {
            ZStack {
                MainTabView()
                    .environmentObject(themeManager)

                KeyboardDismissalInstaller()
                    .frame(width: 0, height: 0)
                    .allowsHitTesting(false)

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
                // Repository initialization owns the first background refresh.
                _ = TransactionRepository.shared
                _ = ReviewQueueRepository.shared
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

// MARK: - Global Keyboard Dismissal on Tap Outside

final class DismissKeyboardTapGesture: UITapGestureRecognizer, UIGestureRecognizerDelegate {
    init() {
        super.init(target: nil, action: nil)
        self.cancelsTouchesInView = false
        self.delaysTouchesBegan = false
        self.delaysTouchesEnded = false
        self.delegate = self
        self.addTarget(self, action: #selector(handleTap))
    }

    @objc private func handleTap() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }

    func gestureRecognizer(_ gestureRecognizer: UIGestureRecognizer, shouldReceive touch: UITouch) -> Bool {
        var current: UIView? = touch.view
        while let view = current {
            if view is UITextField || view is UITextView {
                return false
            }
            let className = String(describing: type(of: view))
            if className.contains("TextField") || className.contains("TextView") {
                return false
            }
            current = view.superview
        }
        return true
    }

    func gestureRecognizer(
        _ gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
    ) -> Bool {
        return true
    }
}

final class KeyboardDismissalManager: NSObject {
    static let shared = KeyboardDismissalManager()
    private var installedWindows = NSHashTable<UIWindow>.weakObjects()

    override private init() {
        super.init()
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(windowDidBecomeVisible(_:)),
            name: UIWindow.didBecomeVisibleNotification,
            object: nil
        )
    }

    @objc private func windowDidBecomeVisible(_ notification: Notification) {
        guard let window = notification.object as? UIWindow else { return }
        install(on: window)
    }

    func install(on window: UIWindow) {
        let className = String(describing: type(of: window))
        if className.contains("Keyboard") || className.contains("TextEffects") {
            return
        }
        if installedWindows.contains(window) {
            return
        }
        if window.gestureRecognizers?.contains(where: { $0 is DismissKeyboardTapGesture }) == true {
            return
        }
        installedWindows.add(window)
        let gesture = DismissKeyboardTapGesture()
        window.addGestureRecognizer(gesture)
    }
}

struct KeyboardDismissalInstaller: UIViewRepresentable {
    func makeUIView(context: Context) -> KeyboardDismissalUIView {
        KeyboardDismissalUIView()
    }

    func updateUIView(_ uiView: KeyboardDismissalUIView, context: Context) {}
}

final class KeyboardDismissalUIView: UIView {
    override func didMoveToWindow() {
        super.didMoveToWindow()
        if let window = self.window {
            KeyboardDismissalManager.shared.install(on: window)
        }
    }
}

extension View {
    func hideKeyboard() {
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}
