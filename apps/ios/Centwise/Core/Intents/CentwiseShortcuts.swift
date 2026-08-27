import AppIntents

public struct CentwiseShortcuts: AppShortcutsProvider {
    public static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: ParseTransactionIntent(),
            phrases: [
                "Log transaction in \(.applicationName)",
                "Track expense in \(.applicationName)",
                "Add transaction to \(.applicationName)"
            ],
            shortTitle: "Log Transaction",
            systemImageName: "plus.circle.fill"
        )
    }
}
