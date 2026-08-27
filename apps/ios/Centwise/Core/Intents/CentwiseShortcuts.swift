import AppIntents

public struct CentwiseShortcuts: AppShortcutsProvider {
    public static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: ParseTransactionIntent(),
            phrases: [
                "Track transaction in \(.applicationName)",
                "Parse SMS in \(.applicationName)",
                "Log financial message in \(.applicationName)"
            ],
            shortTitle: "Track SMS",
            systemImageName: "creditcard.and.123"
        )
    }
}
