import Foundation
import LocalAuthentication
import SwiftUI
import UIKit

public final class AppLockManager: ObservableObject {
    public static let shared = AppLockManager()

    @AppStorage("appLockEnabled") public var appLockEnabled: Bool = false
    @AppStorage("lockTimeoutMinutes") public var lockTimeoutMinutes: Int = 0

    @Published public var isLocked: Bool = false

    private var lastBackgroundDate: Date?

    private init() {
        let notificationCenter = NotificationCenter.default
        notificationCenter.addObserver(
            self,
            selector: #selector(handleDidEnterBackground),
            name: UIApplication.didEnterBackgroundNotification,
            object: nil
        )
        notificationCenter.addObserver(
            self,
            selector: #selector(handleWillEnterForeground),
            name: UIApplication.willEnterForegroundNotification,
            object: nil
        )
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    // MARK: - Biometrics

    public var canUseBiometrics: Bool {
        var error: NSError?
        return LAContext().canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
    }

    public var biometricType: String {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return "Passcode"
        }
        switch context.biometryType {
        case .faceID: return "Face ID"
        case .touchID: return "Touch ID"
        default: return "Passcode"
        }
    }

    public var biometricIcon: String {
        let context = LAContext()
        var error: NSError?
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return "lock.fill"
        }
        switch context.biometryType {
        case .faceID: return "faceid"
        case .touchID: return "touchid"
        default: return "lock.fill"
        }
    }

    // MARK: - Lock Lifecycle

    @objc private func handleDidEnterBackground() {
        guard appLockEnabled else { return }
        lastBackgroundDate = Date()
    }

    @objc private func handleWillEnterForeground() {
        guard appLockEnabled, let lastDate = lastBackgroundDate else { return }

        let elapsed = Date().timeIntervalSince(lastDate)
        if elapsed >= TimeInterval(lockTimeoutMinutes * 60) {
            isLocked = true
        }
    }

    public func requestUnlock() {
        let context = LAContext()
        var error: NSError?

        let policy: LAPolicy = canUseBiometrics
            ? .deviceOwnerAuthenticationWithBiometrics
            : .deviceOwnerAuthentication

        guard context.canEvaluatePolicy(policy, error: &error) else {
            isLocked = false
            return
        }

        context.evaluatePolicy(
            policy,
            localizedReason: "Unlock Centwise to view your finances"
        ) { [weak self] success, _ in
            DispatchQueue.main.async {
                self?.isLocked = !success
            }
        }
    }

    public func lockNow() {
        guard appLockEnabled else { return }
        isLocked = true
    }
}
