import AppIntents

public struct CentwiseShortcuts: AppShortcutsProvider {
    public static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: ParseTransactionIntent(),
            phrases: [
                "Track transaction in \(.applicationName)",
                "Log transaction in \(.applicationName)",
                "Track expense in \(.applicationName)"
            ],
            shortTitle: "Track Transaction from SMS",
            systemImageName: "plus.circle.fill"
        )
    }
}
