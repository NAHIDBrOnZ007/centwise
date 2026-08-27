import SwiftUI

public struct OnboardingScreen: View {
    public var onComplete: () -> Void

    @ObservedObject private var themeManager = ThemeManager.shared
    @ObservedObject private var profileManager = ProfileManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var currentPage = 0

    // Step 1 Profile State
    @State private var selectedAvatar: String = "avatar_1"
    @State private var userNameInput: String = ""

    // Step 3 Test SMS State
    @State private var sampleSmsText: String = "Payment Tk 500.00 to Foodpanda successful. TrxID 9J3K2L. Balance Tk 4,250.50."
    @State private var testResult: String?

    public init(onComplete: @escaping () -> Void) {
        self.onComplete = onComplete
    }

    public var body: some View {
        ZStack {
            CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Bar with Centwise Badge & Skip Button
                HStack {
                    HStack(spacing: 6) {
                        Image(systemName: "banknote.fill")
                            .foregroundColor(themeManager.accentColor)
                        Text("Centwise")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)
                    }

                    Spacer()

                    Button("Skip") {
                        saveProfileAndFinish()
                    }
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(.secondary)
                }
                .padding(.horizontal, CentwiseSpacing.lg)
                .padding(.top, CentwiseSpacing.md)
                .padding(.bottom, CentwiseSpacing.xs)

                // Paged Carousel
                TabView(selection: $currentPage) {
                    // Page 0: Choose Avatar & Profile
                    avatarProfileStep
                        .tag(0)

                    // Page 1: Automatic Bangladesh SMS Tracking
                    trackingStep
                        .tag(1)

                    // Page 2: Shortcuts Message Automation Setup
                    shortcutsGuideStep
                        .tag(2)

                    // Page 3: Privacy & Ready
                    privacyStep
                        .tag(3)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))

                // Bottom Pagination Dots & Controls
                VStack(spacing: CentwiseSpacing.md) {
                    // Page Indicators
                    HStack(spacing: 6) {
                        ForEach(0..<4) { index in
                            Capsule()
                                .fill(currentPage == index ? themeManager.accentColor : Color.secondary.opacity(0.3))
                                .frame(width: currentPage == index ? 22 : 6, height: 6)
                                .animation(.spring(response: 0.3), value: currentPage)
                        }
                    }
                    .padding(.top, 8)

                    // Action Buttons
                    HStack(spacing: CentwiseSpacing.md) {
                        if currentPage > 0 {
                            Button(action: {
                                withAnimation {
                                    currentPage -= 1
                                }
                            }) {
                                Image(systemName: "chevron.left")
                                    .font(.system(size: 16, weight: .semibold))
                                    .foregroundColor(.primary)
                                    .frame(width: 50, height: 50)
                                    .background(
                                        RoundedRectangle(cornerRadius: 14)
                                            .fill(colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.05))
                                    )
                            }
                            .buttonStyle(.plain)
                        }

                        if currentPage < 3 {
                            CentwiseButton("Continue", variant: .primary, isFullWidth: true) {
                                themeManager.triggerHapticFeedback(.light)
                                withAnimation {
                                    currentPage += 1
                                }
                            }
                        } else {
                            CentwiseButton("Get Started with Centwise", variant: .primary, isFullWidth: true) {
                                themeManager.triggerHapticFeedback(.success)
                                saveProfileAndFinish()
                            }
                        }
                    }
                }
                .padding(.horizontal, CentwiseSpacing.lg)
                .padding(.bottom, CentwiseSpacing.xl)
            }
        }
        .onAppear {
            selectedAvatar = profileManager.userAvatar
            userNameInput = profileManager.userName == "User" ? "" : profileManager.userName
        }
    }

    private func saveProfileAndFinish() {
        let finalName = userNameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? "User" : userNameInput
        profileManager.setProfile(name: finalName, avatar: selectedAvatar)
        profileManager.hasCompletedOnboarding = true
        onComplete()
    }

    // MARK: - Step 1: Avatar & Profile
    private var avatarProfileStep: some View {
        ScrollView {
            VStack(spacing: CentwiseSpacing.md) {
                VStack(spacing: CentwiseSpacing.xxs) {
                    Text("Welcome to Centwise")
                        .font(CentwiseTypography.title2)
                        .foregroundColor(.primary)

                    Text("Choose your avatar and enter your name to personalize your dashboard.")
                        .font(CentwiseTypography.footnote)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, CentwiseSpacing.md)
                }

                // Selected Avatar with Glowing Ring
                ZStack {
                    Circle()
                        .fill(themeManager.accentColor.opacity(0.15))
                        .frame(width: 80, height: 80)

                    Image(selectedAvatar)
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 62, height: 62)
                        .clipShape(Circle())
                }
                .overlay(
                    Circle()
                        .stroke(themeManager.accentColor, lineWidth: 3)
                        .frame(width: 80, height: 80)
                )

                // Name Input
                TextField("Your Name (e.g. Faysal)", text: $userNameInput)
                    .font(CentwiseTypography.headline)
                    .multilineTextAlignment(.center)
                    .textFieldStyle(.plain)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 10)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.05))
                    )
                    .frame(maxWidth: 240)

                Text("Pick an Avatar")
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(.secondary)
                    .padding(.top, 4)

                // 10 Avatars Grid
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 54, maximum: 66), spacing: 10)], spacing: 10) {
                    ForEach(ProfileManager.availableAvatars, id: \.self) { avatar in
                        let isSelected = selectedAvatar == avatar
                        Button(action: {
                            themeManager.triggerHapticFeedback(.light)
                            withAnimation(.spring(response: 0.25)) {
                                selectedAvatar = avatar
                            }
                        }) {
                            ZStack {
                                Circle()
                                    .fill(colorScheme == .dark ? Color.white.opacity(0.06) : Color.black.opacity(0.04))
                                    .frame(width: 54, height: 54)

                                Image(avatar)
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 42, height: 42)
                                    .clipShape(Circle())

                                if isSelected {
                                    Circle()
                                        .stroke(themeManager.accentColor, lineWidth: 2.5)
                                        .frame(width: 54, height: 54)

                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.system(size: 14))
                                        .foregroundColor(themeManager.accentColor)
                                        .background(Circle().fill(Color.white).padding(1))
                                        .offset(x: 18, y: -18)
                                }
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, CentwiseSpacing.lg)
            }
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.md)
        }
    }

    // MARK: - Step 2: SMS Tracking
    private var trackingStep: some View {
        VStack(spacing: CentwiseSpacing.lg) {
            Circle()
                .fill(themeManager.accentColor.opacity(0.15))
                .frame(width: 90, height: 90)
                .overlay(
                    Image(systemName: "message.badge.filled.fill")
                        .font(.system(size: 40))
                        .foregroundColor(themeManager.accentColor)
                )

            VStack(spacing: CentwiseSpacing.xs) {
                Text("Track All Bangladesh MFS & Banks")
                    .font(CentwiseTypography.title2)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)

                Text("Centwise automatically recognizes transaction SMS from bKash, Nagad, Rocket, Cellfin, City Bank, BRAC Bank, EBL, and DBBL.")
                    .font(CentwiseTypography.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, CentwiseSpacing.md)
            }

            // Supported providers badge grid
            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    providerPill(name: "bKash", color: CentwiseColors.bKashPink)
                    providerPill(name: "Nagad", color: CentwiseColors.nagadOrange)
                    providerPill(name: "Rocket", color: CentwiseColors.rocketPurple)
                }
                HStack(spacing: 8) {
                    providerPill(name: "Cellfin", color: CentwiseColors.cellfinGreen)
                    providerPill(name: "City Bank", color: CentwiseColors.cityBankRed)
                    providerPill(name: "BRAC Bank", color: CentwiseColors.bracBankBlue)
                }
            }
        }
        .padding(.horizontal, CentwiseSpacing.lg)
    }

    private func providerPill(name: String, color: Color) -> some View {
        Text(name)
            .font(CentwiseTypography.caption1)
            .fontWeight(.semibold)
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(
                Capsule()
                    .fill(color.opacity(0.12))
            )
    }

    // MARK: - Step 3: Apple Shortcuts Automation
    private var shortcutsGuideStep: some View {
        ScrollView {
            VStack(spacing: CentwiseSpacing.md) {
                VStack(spacing: CentwiseSpacing.xxs) {
                    HStack(spacing: 6) {
                        Image(systemName: "bolt.badge.automatic.fill")
                            .foregroundColor(CentwiseColors.bKashPink)
                        Text("How Auto-Tracking Works on iOS")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)
                    }

                    Text("Because iOS keeps SMS private, Centwise links with Apple Shortcuts Message Automation to parse transactions instantly in the background.")
                        .font(CentwiseTypography.footnote)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, CentwiseSpacing.sm)
                }

                // 3 Quick Steps Box
                VStack(alignment: .leading, spacing: 10) {
                    shortcutMiniStep(number: "1", title: "Open Shortcuts", desc: "Tap 'Automation' tab ➔ New Automation (+)")
                    shortcutMiniStep(number: "2", title: "Choose 'Message'", desc: "When Message Contains 'Tk, bKash, Nagad', set 'Run Immediately'")
                    shortcutMiniStep(number: "3", title: "Add Centwise Action", desc: "Select 'Log Transaction' with Shortcut Input")
                }
                .padding(14)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(colorScheme == .dark ? Color.white.opacity(0.06) : Color.black.opacity(0.04))
                )

                // 1-Tap Open Shortcuts Button
                Button(action: {
                    if let url = URL(string: "shortcuts://create-automation"), UIApplication.shared.canOpenURL(url) {
                        UIApplication.shared.open(url)
                    } else if let url = URL(string: "shortcuts://") {
                        UIApplication.shared.open(url)
                    }
                }) {
                    HStack {
                        Image(systemName: "bolt.fill")
                        Text("Open Apple Shortcuts App")
                            .font(CentwiseTypography.headline)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(themeManager.accentColor)
                    .foregroundColor(.white)
                    .cornerRadius(12)
                }
                .buttonStyle(.plain)

                // Live Test Sandbox
                VStack(spacing: 8) {
                    Text("Live Test the Parser:")
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)

                    Button(action: {
                        let result = SmsTransactionProcessor.shared.processIncomingSms(body: sampleSmsText)
                        if case .inserted? = result?.status {
                            testResult = "✅ Rust core tracked the sample transaction"
                        } else {
                            testResult = "ℹ️ Rust core filtered or queued the sample message"
                        }
                    }) {
                        HStack {
                            Image(systemName: "play.circle.fill")
                            Text("Simulate bKash SMS (৳500 Foodpanda)")
                                .font(CentwiseTypography.caption1)
                                .fontWeight(.medium)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 8)
                        .background(themeManager.accentColor.opacity(0.12))
                        .foregroundColor(themeManager.accentColor)
                        .cornerRadius(8)
                    }
                    .buttonStyle(.plain)

                    if let res = testResult {
                        Text(res)
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(CentwiseColors.incomeGreen)
                    }
                }
            }
            .padding(.horizontal, CentwiseSpacing.lg)
            .padding(.top, CentwiseSpacing.xs)
            .padding(.bottom, CentwiseSpacing.md)
        }
    }

    private func shortcutMiniStep(number: String, title: String, desc: String) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Text(number)
                .font(CentwiseTypography.caption1)
                .fontWeight(.bold)
                .foregroundColor(themeManager.accentColor)
                .frame(width: 22, height: 22)
                .background(Circle().fill(themeManager.accentColor.opacity(0.15)))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(CentwiseTypography.caption1)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)

                Text(desc)
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Step 4: Privacy
    private var privacyStep: some View {
        VStack(spacing: CentwiseSpacing.lg) {
            Circle()
                .fill(CentwiseColors.incomeGreen.opacity(0.15))
                .frame(width: 90, height: 90)
                .overlay(
                    Image(systemName: "lock.shield.fill")
                        .font(.system(size: 40))
                        .foregroundColor(CentwiseColors.incomeGreen)
                )

            VStack(spacing: CentwiseSpacing.xs) {
                Text("100% On-Device Privacy")
                    .font(CentwiseTypography.title2)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)

                Text("No account required. No cloud servers. Your finances, SMS messages, and bank balances are stored securely right here on your iPhone.")
                    .font(CentwiseTypography.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, CentwiseSpacing.md)
            }

            VStack(spacing: 8) {
                privacyFeatureRow(icon: "icloud.slash", text: "Zero data uploaded to cloud")
                privacyFeatureRow(icon: "person.crop.circle.badge.xmark", text: "No signup or personal info asked")
                privacyFeatureRow(icon: "shield.lefthalf.filled", text: "Protected by Face ID / Touch ID")
            }
            .padding(.top, 8)
        }
        .padding(.horizontal, CentwiseSpacing.lg)
    }

    private func privacyFeatureRow(icon: String, text: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(CentwiseColors.incomeGreen)
                .frame(width: 24)

            Text(text)
                .font(CentwiseTypography.footnote)
                .foregroundColor(.primary)

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(colorScheme == .dark ? Color.white.opacity(0.04) : Color.black.opacity(0.03))
        )
    }
}
