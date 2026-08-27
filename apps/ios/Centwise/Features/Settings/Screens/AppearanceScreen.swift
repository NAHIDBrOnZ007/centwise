import SwiftUI

public struct AppearanceScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                themeModeCard
                accentColorCard
                hapticsCard
            }
            .padding(.horizontal, CentwiseSpacing.md)
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Theme Mode

    private var themeModeCard: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                Text("Theme Mode")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                Picker("Theme", selection: $themeManager.themeMode) {
                    Text("System").tag(ThemeMode.system)
                    Text("Light").tag(ThemeMode.light)
                    Text("Dark").tag(ThemeMode.dark)
                }
                .pickerStyle(.segmented)

                if themeManager.themeMode == .dark || themeManager.themeMode == .amoled {
                    Toggle(isOn: amoledBinding) {
                        VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                            Text("AMOLED Black")
                                .font(CentwiseTypography.bodyMedium)
                            Text("Pure black background for OLED displays")
                                .font(CentwiseTypography.caption1)
                                .foregroundColor(.secondary)
                        }
                    }
                    .tint(themeManager.accentColor)
                }

                Text("System follows your device settings automatically.")
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var amoledBinding: Binding<Bool> {
        Binding(
            get: { themeManager.themeMode == .amoled },
            set: { themeManager.themeMode = $0 ? .amoled : .dark }
        )
    }

    // MARK: - Accent Color

    private var accentColorCard: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                Text("Accent Color")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: CentwiseSpacing.mdLg) {
                        ForEach(AccentChoice.allCases) { choice in
                            accentSwatch(choice)
                        }
                    }
                    .padding(.vertical, CentwiseSpacing.xxs)
                }
            }
        }
    }

    private func accentSwatch(_ choice: AccentChoice) -> some View {
        let isSelected = themeManager.accentChoice == choice

        return Button {
            themeManager.accentChoice = choice
            themeManager.triggerHapticFeedback(.light)
        } label: {
            VStack(spacing: CentwiseSpacing.xs) {
                ZStack {
                    if isSelected {
                        Circle()
                            .strokeBorder(choice.color, lineWidth: 2.5)
                            .frame(width: 50, height: 50)
                    }

                    Circle()
                        .fill(choice.color)
                        .frame(width: 42, height: 42)

                    if isSelected {
                        Image(systemName: "checkmark")
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(.white)
                    }
                }
                .frame(width: 50, height: 50)

                Text(choice.rawValue)
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(isSelected ? .primary : .secondary)
                    .lineLimit(1)
            }
            .frame(width: 72)
        }
        .buttonStyle(.plain)
    }

    // MARK: - Haptics

    private var hapticsCard: some View {
        CentwiseCard {
            Toggle(isOn: $themeManager.enableHaptics) {
                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text("Haptic Feedback")
                        .font(CentwiseTypography.bodyMedium)
                    Text("Subtle vibration on taps and actions")
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                }
            }
            .tint(themeManager.accentColor)
        }
    }
}
