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
                        Text("iOS Auto-Tracking")
                            .font(CentwiseTypography.title2)
                            .foregroundColor(.primary)
                    }

                    Text("Because iOS restricts apps from directly reading your SMS inbox, Centwise uses Apple Shortcuts Message Automation to parse your Bangladesh bank and MFS transactions silently in the background.")
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
                        description: "Select 'Message', set Message Contains to 'Tk, bKash, Nagad, Rocket, Cellfin, Citytouch', and toggle 'Run Immediately' ON.",
                        icon: "message.fill"
                    )

                    stepCard(
                        stepNumber: 3,
                        title: "Add Centwise Action",
                        description: "Choose the action 'Parse SMS Transaction' from Centwise and pass the Shortcut Message Input.",
                        icon: "bolt.fill"
                    )
                }
                .padding(.horizontal, CentwiseSpacing.md)

                // 1-Tap Open Shortcuts Button
                VStack(spacing: CentwiseSpacing.sm) {
                    Button(action: {
                        if let url = URL(string: "shortcuts://create-automation"), UIApplication.shared.canOpenURL(url) {
                            UIApplication.shared.open(url)
                        } else if let url = URL(string: "shortcuts://") {
                            UIApplication.shared.open(url)
                        }
                    }) {
                        HStack {
                            Image(systemName: "bolt.fill")
                            Text("Open Apple Shortcuts Automation")
                                .fontWeight(.semibold)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(themeManager.accentColor)
                        .foregroundColor(.white)
                        .cornerRadius(14)
                    }

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

                        Text("bKash, Nagad, Rocket, Upay, Cellfin, City Bank, BRAC Bank, Eastern Bank, Dutch-Bangla Bank.")
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
                        sampleSmsText = "Payment Tk 500.00 to Foodpanda successful. TrxID TEST\(Int.random(in: 100...999)). Balance Tk 4,250.50."
                    }
                    .font(CentwiseTypography.caption)
                    .buttonStyle(.bordered)

                    Button("Nagad Sample") {
                        sampleSmsText = "Payment of Tk 250.00 to Pathao is successful. TrxID NAG\(Int.random(in: 100...999)). Balance Tk 3,000.00."
                    }
                    .font(CentwiseTypography.caption)
                    .buttonStyle(.bordered)
                }

                Button(action: {
                    if let tx = SmsTransactionProcessor.shared.processIncomingSms(body: sampleSmsText) {
                        testResult = "✅ Tracked: \(tx.title) (৳\(Int(tx.amount))) in \(tx.category)"
                    } else {
                        testResult = "ℹ️ Message filtered / queued to Review Queue"
                    }
                }) {
                    HStack {
                        Image(systemName: "play.circle.fill")
                        Text("Run Live Test")
                            .fontWeight(.medium)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(themeManager.accentColor.opacity(0.15))
                    .foregroundColor(themeManager.accentColor)
                    .cornerRadius(10)
                }

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
