import SwiftUI

public struct FAQScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        List {
            faqSection("SMS Tracking", items: smsFAQs)
            faqSection("Privacy & Data", items: privacyFAQs)
            faqSection("Accounts & Providers", items: accountFAQs)
            faqSection("Budgets & Analytics", items: budgetFAQs)
        }
        .listStyle(.insetGrouped)
        .navigationTitle("FAQ")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Data

    private var smsFAQs: [(String, String)] {
        [
            ("How does Centwise track my transactions?",
             "Centwise reads bank and MFS SMS messages on your device and turns them into transactions automatically. On Android this happens in the background. On iOS, a Shortcut forwards new SMS to Centwise automatically once set up."),
            ("Which providers are supported?",
             "Major Bangladeshi banks and MFS services including bKash, Nagad, Rocket, Upay, and leading bank cards. Support for more providers is added regularly through the parser."),
            ("Why was an SMS not tracked?",
             "OTP, promotional, and non-transaction messages are ignored on purpose. If a real transaction was missed, add it manually or check the review queue."),
            ("Which SMS languages are supported?",
             "Centwise parses English-language transaction messages.")
        ]
    }

    private var privacyFAQs: [(String, String)] {
        [
            ("Is my financial data secure?",
             "Yes. All parsing and storage happens on your device. Centwise has no servers and does not upload your SMS or transactions anywhere by default."),
            ("Can I back up my data?",
             "Yes. You can create a local backup and export transactions to CSV from Settings."),
            ("How do I delete all my data?",
             "Use the data deletion option in Settings. This permanently removes all transactions, accounts, and preferences from the device.")
        ]
    }

    private var accountFAQs: [(String, String)] {
        [
            ("How are accounts detected?",
             "Accounts are created automatically from SMS using the provider and the last four digits of your card or wallet. You can rename them or add manual accounts like Cash."),
            ("What is a manual account?",
             "A manual account lets you track cash or anything without SMS. You update the balance yourself."),
            ("Why does my balance look wrong?",
             "Balances come from the balance mentioned in SMS. If a message is missing, add the transaction manually and the balance will correct itself.")
        ]
    }

    private var budgetFAQs: [(String, String)] {
        [
            ("How do category budgets work?",
             "Set a monthly limit for a category. Centwise tracks spending against it and warns you as you approach the limit."),
            ("What are Smart Rules?",
             "Rules automatically categorize transactions when the merchant name matches a keyword, for example every Foodpanda order goes to Food & Dining.")
        ]
    }

    // MARK: - Views

    private func faqSection(_ title: String, items: [(String, String)]) -> some View {
        Section(title) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                DisclosureGroup(item.0) {
                    Text(item.1)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .padding(.vertical, CentwiseSpacing.xxs)
                }
                .tint(themeManager.accentColor)
            }
        }
    }
}
