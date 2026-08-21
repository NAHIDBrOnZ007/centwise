import SwiftUI

public enum ThemeMode: String, CaseIterable, Identifiable {
    case system = "System"
    case light = "Light"
    case dark = "Dark"
    case amoled = "AMOLED Black"

    public var id: String { rawValue }
}

public enum AccentChoice: String, CaseIterable, Identifiable {
    case emerald = "Emerald Green"
    case bKash = "bKash Pink"
    case nagad = "Nagad Orange"
    case rocket = "Rocket Violet"
    case sapphire = "Sapphire Blue"

    public var id: String { rawValue }

    public var color: Color {
        switch self {
        case .emerald: return CentwiseColors.primaryEmerald
        case .bKash: return CentwiseColors.bKashPink
        case .nagad: return CentwiseColors.nagadOrange
        case .rocket: return CentwiseColors.rocketPurple
        case .sapphire: return CentwiseColors.transferBlue
        }
    }
}

public final class ThemeManager: ObservableObject {
    public static let shared = ThemeManager()

    @AppStorage("selectedThemeMode") public var themeMode: ThemeMode = .system
    @AppStorage("selectedAccentChoice") public var accentChoice: AccentChoice = .emerald
    @AppStorage("enableHaptics") public var enableHaptics: Bool = true

    public var colorScheme: ColorScheme? {
        switch themeMode {
        case .system: return nil
        case .light: return .light
        case .dark, .amoled: return .dark
        }
    }

    public var isAmoledActive: Bool {
        themeMode == .amoled
    }

    public var accentColor: Color {
        accentChoice.color
    }

    public func triggerHapticFeedback(_ type: UIImpactFeedbackGenerator.FeedbackStyle = .medium) {
        guard enableHaptics else { return }
        let generator = UIImpactFeedbackGenerator(style: type)
        generator.impactOccurred()
    }
}

// MARK: - Environment Key for AMOLED Mode
private struct IsAmoledActiveKey: EnvironmentKey {
    static let defaultValue: Bool = false
}

extension EnvironmentValues {
    public var isAmoledActive: Bool {
        get { self[IsAmoledActiveKey.self] }
        set { self[IsAmoledActiveKey.self] = newValue }
    }
}
