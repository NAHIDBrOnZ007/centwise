import SwiftUI

public struct AppearanceScreen: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @State private var showAllColorsPicker = false

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

            Section {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: CentwiseSpacing.mdLg) {
                        ForEach(AccentChoice.featuredCases) { choice in
                            accentSwatch(choice)
                        }
                    }
                    .padding(.vertical, CentwiseSpacing.xxs)
                }
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))

                Button {
                    showAllColorsPicker = true
                    themeManager.triggerHapticFeedback(.light)
                } label: {
                    HStack(spacing: 12) {
                        Image(systemName: "paintpalette.fill")
                            .foregroundStyle(themeManager.accentColor)
                            .frame(width: 26)
                            .accessibilityHidden(true)

                        Text("See All Colors")
                            .font(.body)
                            .foregroundColor(.primary)

                        Spacer()

                        Text("\(AccentChoice.allCases.count) Colors")
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        Image(systemName: "chevron.forward")
                            .font(.footnote.weight(.semibold))
                            .foregroundColor(.secondary)
                    }
                }
            } header: {
                Text("Accent Color")
            }

            Section("Haptics") {
                Toggle("Haptic Feedback", isOn: $themeManager.enableHaptics)
            }
        }
        .tint(themeManager.accentColor)
        .formStyle(.grouped)
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showAllColorsPicker) {
            NavigationStack {
                ThemeColorPickerView()
            }
            .presentationDetents([.medium, .large])
        }
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

// MARK: - Color Picker Sheet (Matching AvatarPickerView Pattern)
public struct ThemeColorPickerView: View {
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var selectedCategory: AccentCategory? = nil

    private let columns = [
        GridItem(.adaptive(minimum: 64, maximum: 80), spacing: 14)
    ]

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(spacing: CentwiseSpacing.lg) {
                // Live Selected Color Preview (matching AvatarPickerView top preview)
                VStack(spacing: CentwiseSpacing.sm) {
                    ZStack {
                        Circle()
                            .fill(themeManager.accentColor.opacity(0.15))
                            .frame(width: 90, height: 90)

                        Circle()
                            .fill(themeManager.accentColor)
                            .frame(width: 60, height: 60)
                    }
                    .overlay(
                        Circle()
                            .stroke(themeManager.accentColor, lineWidth: 3)
                            .frame(width: 90, height: 90)
                    )

                    Text(themeManager.accentChoice.rawValue)
                        .font(CentwiseTypography.headline)

                    Text(themeManager.accentChoice.subtitle)
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, CentwiseSpacing.sm)

                // Category Filter Pills
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        filterPill(title: "All", category: nil)
                        ForEach(AccentCategory.allCases) { cat in
                            filterPill(title: cat.rawValue, category: cat)
                        }
                    }
                    .padding(.horizontal, CentwiseSpacing.md)
                }

                Text("Choose Your Color")
                    .font(CentwiseTypography.subheadline)
                    .foregroundColor(.secondary)

                // Grid of Colors (matching AvatarPickerView grid)
                let displayedChoices = AccentChoice.allCases.filter {
                    selectedCategory == nil || $0.category == selectedCategory
                }

                LazyVGrid(columns: columns, spacing: 14) {
                    ForEach(displayedChoices) { choice in
                        let isSelected = themeManager.accentChoice == choice
                        Button {
                            themeManager.triggerHapticFeedback(.light)
                            withAnimation(.spring(response: 0.25, dampingFraction: 0.7)) {
                                themeManager.accentChoice = choice
                            }
                        } label: {
                            VStack(spacing: 6) {
                                ZStack {
                                    Circle()
                                        .fill(colorScheme == .dark ? Color.white.opacity(0.06) : Color.black.opacity(0.04))
                                        .frame(width: 64, height: 64)

                                    Circle()
                                        .fill(choice.color)
                                        .frame(width: 48, height: 48)

                                    if isSelected {
                                        Circle()
                                            .stroke(themeManager.accentColor, lineWidth: 3)
                                            .frame(width: 64, height: 64)

                                        Image(systemName: "checkmark.circle.fill")
                                            .font(.system(size: 18))
                                            .foregroundColor(themeManager.accentColor)
                                            .background(Circle().fill(Color.white).padding(2))
                                            .offset(x: 22, y: -22)
                                    }
                                }

                                Text(choice.rawValue)
                                    .font(CentwiseTypography.caption2)
                                    .foregroundColor(isSelected ? .primary : .secondary)
                                    .lineLimit(1)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.bottom, CentwiseSpacing.lg)
            }
        }
        .padding(.top, CentwiseSpacing.lg)
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("Accent Color")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Done") {
                    dismiss()
                }
            }
        }
    }

    private func filterPill(title: String, category: AccentCategory?) -> some View {
        let isSelected = selectedCategory == category
        return Button {
            themeManager.triggerHapticFeedback(.selection)
            withAnimation(.spring(response: 0.25, dampingFraction: 0.75)) {
                selectedCategory = category
            }
        } label: {
            Text(title)
                .font(CentwiseTypography.caption1.weight(isSelected ? .semibold : .medium))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    Capsule()
                        .fill(isSelected ? themeManager.accentColor : (colorScheme == .dark ? Color.white.opacity(0.08) : Color.black.opacity(0.05)))
                )
                .foregroundColor(isSelected ? .white : .primary)
        }
    }
}
