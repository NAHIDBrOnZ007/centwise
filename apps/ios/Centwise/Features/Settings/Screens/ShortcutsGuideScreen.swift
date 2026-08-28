import SwiftUI

public struct ShortcutsGuideScreen: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // Header Banner
                VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                    HStack(spacing: CentwiseSpacing.sm) {
                        Image(systemName: "bolt.badge.automatic.fill")
                            .font(.system(size: 24))
                            .foregroundColor(themeManager.accentColor)
                        Text("Apple Shortcuts Sync")
                            .font(CentwiseTypography.title2)
                            .foregroundColor(.primary)
                    }

                    Text("Because iOS protects your SMS privacy, Centwise uses Apple Shortcuts Automation to log your bKash, Nagad, and bank transactions securely with zero manual entry.")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.xs)

                // 3 Step Guide
                VStack(spacing: CentwiseSpacing.md) {
                    stepCard(
                        stepNumber: 1,
                        title: "Open Apple Shortcuts",
                        description: "Open the built-in Shortcuts app on your iPhone and tap on the 'Automation' tab at the bottom.",
                        icon: "square.stack.3d.up.fill"
                    )

                    stepCard(
                        stepNumber: 2,
                        title: "Create 'Message' Automation",
                        description: "Select 'Message', set Message Contains to 'Tk' (or paste the full keyword list below), and toggle 'Run Immediately' ON.",
                        icon: "message.fill"
                    )

                    stepCard(
                        stepNumber: 3,
                        title: "Add Centwise Action",
                        description: "Tap 'Centwise' from the apps list, choose 'Log Transaction', and set Message Text to 'Shortcut Input'.",
                        icon: "bolt.fill"
                    )
                }
                .padding(.horizontal, CentwiseSpacing.md)

                // 1-Tap Copy Keywords Card
                keywordsCopyCard
                    .padding(.horizontal, CentwiseSpacing.md)

                // 1-Tap Open Shortcuts Button
                VStack(spacing: CentwiseSpacing.sm) {
                    Button(action: {
                        themeManager.triggerHapticFeedback(.selection)
                        if let url = URL(string: "shortcuts://create-automation"), UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        } else if let url = URL(string: "shortcuts://") {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        Label("Open Apple Shortcuts App", systemImage: "bolt.fill")
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .tint(themeManager.accentColor)

                    Text("Tapping above opens Shortcuts directly on your iPhone")
                        .font(CentwiseTypography.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.horizontal, CentwiseSpacing.md)

                // Interactive Shortcut Simulator (Test in Xcode / Device)
                shortcutTesterCard
                    .padding(.horizontal, CentwiseSpacing.md)

                // Supported Provider Keywords
                CentwiseCard {
                    VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                        Text("Supported Bangladesh Providers")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)

                        Text("bKash, Nagad, Rocket, Upay, Cellfin, City Bank, BRAC Bank, Eastern Bank, Dutch-Bangla Bank, Islami Bank, UCB, Prime Bank.")
                            .font(CentwiseTypography.footnote)
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal, CentwiseSpacing.md)
            }
            .padding(.bottom, 120)
        }
        .background(CentwiseColors.background(for: colorScheme).ignoresSafeArea())
        .navigationTitle("Shortcuts Setup")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func stepCard(stepNumber: Int, title: String, description: String, icon: String) -> some View {
        CentwiseCard {
            HStack(alignment: .top, spacing: CentwiseSpacing.mdSm) {
                Circle()
                    .fill(themeManager.accentColor.opacity(0.15))
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text("\(stepNumber)")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(themeManager.accentColor)
                    )

                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text(title)
                        .font(CentwiseTypography.bodyMedium)
                        .foregroundColor(.primary)

                    Text(description)
                        .font(CentwiseTypography.footnote)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, CentwiseSpacing.xs)
        }
    }

    @State private var hasCopiedKeywords = false
    private let allFinancialKeywords = "Tk, bKash, Nagad, Rocket, Cellfin, Payment, Balance, debited, credited, TrxID, Fee, BDT, Cash Out, Transfer"

    @ViewBuilder
    private var keywordsCopyCard: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                HStack {
                    Image(systemName: "doc.on.doc.fill")
                        .foregroundColor(themeManager.accentColor)
                    Text("All Financial Keywords")
                        .font(CentwiseTypography.headline)
                        .foregroundColor(.primary)
                    Spacer()
                    if hasCopiedKeywords {
                        Text("Copied!")
                            .font(CentwiseTypography.caption)
                            .foregroundColor(CentwiseColors.incomeGreen)
                    }
                }

                Text("Copy and paste these keywords into the 'Message Contains' field in Shortcuts:")
                    .font(CentwiseTypography.footnote)
                    .foregroundColor(.secondary)

                Text(allFinancialKeywords)
                    .font(.system(size: 12, design: .monospaced))
                    .foregroundColor(.primary)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(colorScheme == .dark ? Color(white: 0.12) : Color(white: 0.94))
                    .cornerRadius(8)

                Button(action: {
                    UIPasteboard.general.string = allFinancialKeywords
                    themeManager.triggerHapticFeedback(.selection)
                    withAnimation {
                        hasCopiedKeywords = true
                    }
                    DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                        withAnimation {
                            hasCopiedKeywords = false
                        }
                    }
                }) {
                    Label(
                        hasCopiedKeywords ? "Copied to Clipboard!" : "Copy All Keywords",
                        systemImage: hasCopiedKeywords ? "checkmark.circle.fill" : "doc.on.doc"
                    )
                }
                .buttonStyle(.bordered)
                .tint(hasCopiedKeywords ? CentwiseColors.incomeGreen : themeManager.accentColor)
                .frame(maxWidth: .infinity)
                .padding(.top, 4)
            }
        }
    }

    @State private var sampleSmsText: String = "Payment Tk 500.00 to Foodpanda successful. TrxID 9J3K2L. Balance Tk 4,250.50."
    @State private var testResult: String?

    @ViewBuilder
    private var shortcutTesterCard: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                HStack {
                    Image(systemName: "flask.fill")
                        .foregroundColor(themeManager.accentColor)
                    Text("Test Shortcut Action Live")
                        .font(CentwiseTypography.headline)
                        .foregroundColor(.primary)
                }

                Text("Simulate an incoming SMS to test the iOS AppIntent parser directly:")
                    .font(CentwiseTypography.footnote)
                    .foregroundColor(.secondary)

                TextField("Paste SMS here...", text: $sampleSmsText)
                    .textFieldStyle(.roundedBorder)
                    .font(CentwiseTypography.caption)

                HStack(spacing: CentwiseSpacing.xs) {
                    Button("bKash Sample") {
                        let uniqueId = Int(Date().timeIntervalSince1970) % 100000 + Int.random(in: 100...999)
                        sampleSmsText = "Payment Tk 500.00 to Foodpanda successful. TrxID BK\(uniqueId). Balance Tk 4,250.50."
                    }
                    .font(CentwiseTypography.caption)
                    .buttonStyle(.bordered)

                    Button("Nagad Sample") {
                        let uniqueId = Int(Date().timeIntervalSince1970) % 100000 + Int.random(in: 100...999)
                        sampleSmsText = "Payment of Tk 250.00 to Pathao is successful. TrxID NG\(uniqueId). Balance Tk 3,000.00."
                    }
                    .font(CentwiseTypography.caption)
                    .buttonStyle(.bordered)
                }

                Button(action: {
                    let result = SmsTransactionProcessor.shared.processIncomingSms(body: sampleSmsText)
                    switch result?.status {
                    case .inserted?:
                        testResult = "✅ Transaction tracked and added to your dashboard!"
                    case .queuedForReview?:
                        testResult = "📝 Sent to Review Queue for your confirmation."
                    case .duplicate?:
                        testResult = "ℹ️ This transaction (TrxID) was already recorded earlier."
                    case .ignored?:
                        testResult = "ℹ️ Non-financial message (filtered out safely)."
                    case .none:
                        testResult = "⚠️ Could not process message."
                    }
                }) {
                    Label("Run Live Test", systemImage: "play.circle.fill")
                }
                .buttonStyle(.bordered)
                .tint(themeManager.accentColor)
                .frame(maxWidth: .infinity)

                if let result = testResult {
                    Text(result)
                        .font(CentwiseTypography.footnote)
                        .foregroundColor(themeManager.accentColor)
                        .padding(.top, 4)
                }
            }
            .padding(.vertical, CentwiseSpacing.xs)
        }
    }
}
