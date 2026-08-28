import SwiftUI

public struct AppearanceScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    public var body: some View {
        Form {
            Section("Theme") {
                Picker("Theme", selection: baseThemeBinding) {
                    Text("System").tag(ThemeMode.system)
                    Text("Light").tag(ThemeMode.light)
                    Text("Dark").tag(ThemeMode.dark)
                }

                if themeManager.themeMode == .dark || themeManager.themeMode == .amoled {
                    Toggle("AMOLED Black", isOn: amoledBinding)
                    Text("Pure black background for OLED displays.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }

                Text("System follows your device settings automatically.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            Section("Accent Color") {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: CentwiseSpacing.mdLg) {
                        ForEach(AccentChoice.allCases) { choice in
                            accentSwatch(choice)
                        }
                    }
                    .padding(.vertical, CentwiseSpacing.xxs)
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
            }

            Section("Haptics") {
                Toggle("Haptic Feedback", isOn: $themeManager.enableHaptics)
                Text("Subtle vibration on taps and actions.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }
        .tint(themeManager.accentColor)
        .formStyle(.grouped)
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var baseThemeBinding: Binding<ThemeMode> {
        Binding(
            get: {
                if themeManager.themeMode == .amoled {
                    return .dark
                }
                return themeManager.themeMode
            },
            set: { newMode in
                if newMode == .dark && themeManager.isAmoledActive {
                    themeManager.themeMode = .amoled
                } else {
                    themeManager.themeMode = newMode
                }
            }
        )
    }

    private var amoledBinding: Binding<Bool> {
        Binding(
            get: { themeManager.themeMode == .amoled },
            set: { themeManager.themeMode = $0 ? .amoled : .dark }
        )
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
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

}
