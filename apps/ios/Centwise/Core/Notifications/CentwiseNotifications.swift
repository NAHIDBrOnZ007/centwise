import Foundation
import UserNotifications

public enum CentwiseNotifications {

    /// Requests notification permission (call once during onboarding or first save).
    public static func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in }
    }

    /// Shows a local notification for a newly tracked transaction.
    public static func notifyNewTransaction(_ transaction: CentwiseTransaction) {
        let content = UNMutableNotificationContent()
        content.title = NSLocalizedString("notification.newTransaction.title", comment: "")

        let sign = transaction.type == .income ? "+" : "-"
        content.body = "\(transaction.title) — \(transaction.category.name) • \(sign)\(CurrencyFormatter.shared.formatBDT(transaction.amount))"
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "centwise-transaction-\(transaction.id)",
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request)
    }

    /// Shows a notification for a background SMS tracked transaction.
    public static func notifyIngestedTransaction(title: String, amountMinor: Int64, isIncome: Bool, categoryName: String, id: String) {
        let content = UNMutableNotificationContent()
        content.title = "Transaction Tracked"

        let sign = isIncome ? "+" : "-"
        let amount = Double(amountMinor) / 100.0
        content.body = "\(title) — \(categoryName) • \(sign)\(CurrencyFormatter.shared.formatBDT(amount))"
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "centwise-transaction-\(id)",
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request)
    }

    /// Shows a budget warning notification when spending crosses a threshold.
    public static func notifyBudgetWarning(categoryName: String, usedPercent: Double) {
        let content = UNMutableNotificationContent()
        content.title = "Budget warning"
        content.body = "\(categoryName) budget is \(Int(usedPercent * 100))% used"
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "centwise-budget-\(categoryName)",
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request)
    }
}
