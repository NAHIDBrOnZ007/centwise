import SwiftUI

public enum ThemeMode: String, CaseIterable, Identifiable {
    case system = "System"
    case light = "Light"
    case dark = "Dark"
    case amoled = "AMOLED Black"

    public var id: String { rawValue }
}

public enum AccentCategory: String, CaseIterable, Identifiable {
    case bangladesh = "Bangladesh Providers"
    case aesthetic = "Aesthetic Vibes"
    case modern = "Modern & Minimal"

    public var id: String { rawValue }
}

public enum AccentChoice: String, CaseIterable, Identifiable {
    // Bangladesh / Financial Brands
    case emerald = "Emerald Green"
    case bKash = "bKash Pink"
    case nagad = "Nagad Orange"
    case rocket = "Rocket Violet"
    case upay = "Upay Blue"
    case cellfin = "Cellfin Green"
    case cityBank = "City Bank Red"
    case easternBank = "EBL Gold"
    case sapphire = "Sapphire Blue"

    // Aesthetic & Neon Vibes
    case cyan = "Cyber Cyan"
    case indigo = "Electric Indigo"
    case lavender = "Velvet Lavender"
    case ruby = "Crimson Ruby"
    case sakura = "Sakura Bloom"
    case amethyst = "Midnight Amethyst"
    case coral = "Neon Coral"
    case aurora = "Aurora Teal"
    case nordicSky = "Nordic Sky"

    // Modern & Minimal Palettes
    case amber = "Sunburst Amber"
    case mint = "Mint Breeze"
    case matcha = "Matcha Green"
    case champagne = "Warm Champagne"
    case roseGold = "Rose Gold"
    case slate = "Graphite Slate"
    case obsidian = "Obsidian Noir"

    public var id: String { rawValue }

    public static var featuredCases: [AccentChoice] {
        [.emerald, .bKash, .nagad, .rocket, .sapphire, .cyan, .lavender, .sakura, .aurora, .ruby]
    }

    public var category: AccentCategory {
        switch self {
        case .emerald, .bKash, .nagad, .rocket, .upay, .cellfin, .cityBank, .easternBank, .sapphire:
            return .bangladesh
        case .cyan, .indigo, .lavender, .ruby, .sakura, .amethyst, .coral, .aurora, .nordicSky:
            return .aesthetic
        case .amber, .mint, .matcha, .champagne, .roseGold, .slate, .obsidian:
            return .modern
        }
    }

    public var color: Color {
        switch self {
        case .emerald: return CentwiseColors.primaryEmerald
        case .bKash: return CentwiseColors.bKashPink
        case .nagad: return CentwiseColors.nagadOrange
        case .rocket: return CentwiseColors.rocketPurple
        case .upay: return CentwiseColors.upayBlue
        case .cellfin: return CentwiseColors.cellfinGreen
        case .cityBank: return CentwiseColors.cityBankRed
        case .easternBank: return CentwiseColors.easternBankGold
        case .sapphire: return CentwiseColors.transferBlue
        case .cyan: return CentwiseColors.cyberCyan
        case .indigo: return CentwiseColors.electricIndigo
        case .lavender: return CentwiseColors.aestheticLavender
        case .ruby: return CentwiseColors.crimsonRuby
        case .sakura: return CentwiseColors.sakuraBloom
        case .amethyst: return CentwiseColors.midnightAmethyst
        case .coral: return CentwiseColors.neonCoral
        case .aurora: return CentwiseColors.auroraTeal
        case .nordicSky: return CentwiseColors.nordicSky
        case .amber: return CentwiseColors.sunsetAmber
        case .mint: return CentwiseColors.mintBreeze
        case .matcha: return CentwiseColors.matchaGreen
        case .champagne: return CentwiseColors.warmChampagne
        case .roseGold: return CentwiseColors.roseGold
        case .slate: return CentwiseColors.graphiteSlate
        case .obsidian: return CentwiseColors.obsidianCharcoal
        }
    }

    public var subtitle: String {
        switch self {
        case .emerald: return "Signature Centwise"
        case .bKash: return "Vibrant Magenta"
        case .nagad: return "Warm Sunset"
        case .rocket: return "Royal Purple"
        case .upay: return "Dynamic Blue"
        case .cellfin: return "Islamic Emerald"
        case .cityBank: return "Signature Crimson"
        case .easternBank: return "Eastern Gold"
        case .sapphire: return "Deep Cobalt"
        case .cyan: return "Electric Neon"
        case .indigo: return "Aesthetic Iris"
        case .lavender: return "Pastel Dream"
        case .ruby: return "Luxury Velvet"
        case .sakura: return "Cherry Blossom"
        case .amethyst: return "Deep Royal Violet"
        case .coral: return "Sunset Peach Glow"
        case .aurora: return "Northern Lights"
        case .nordicSky: return "Crisp Azure"
        case .amber: return "Golden Glow"
        case .mint: return "Fresh Neo Mint"
        case .matcha: return "Zen Matcha"
        case .champagne: return "Refined Amber"
        case .roseGold: return "Soft Metallic"
        case .slate: return "Pure Minimalist"
        case .obsidian: return "Deep Slate Noir"
        }
    }
}

public final class ThemeManager: ObservableObject {
    public static let shared = ThemeManager()

    @AppStorage("selectedThemeMode") public var themeMode: ThemeMode = .system {
        didSet {
            objectWillChange.send()
        }
    }
    @AppStorage("selectedAccentChoice") public var accentChoice: AccentChoice = .emerald {
        didSet {
            objectWillChange.send()
        }
    }
    @AppStorage("enableHaptics") public var enableHaptics: Bool = true {
        didSet {
            objectWillChange.send()
        }
    }

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

    public enum HapticType {
        case light
        case medium
        case heavy
        case soft
        case rigid
        case selection
        case success
        case warning
        case error
    }

    public func triggerHapticFeedback(_ type: HapticType = .medium) {
        guard enableHaptics else { return }
        #if !targetEnvironment(simulator)
        switch type {
        case .light:
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        case .medium:
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        case .heavy:
            UIImpactFeedbackGenerator(style: .heavy).impactOccurred()
        case .soft:
            UIImpactFeedbackGenerator(style: .soft).impactOccurred()
        case .rigid:
            UIImpactFeedbackGenerator(style: .rigid).impactOccurred()
        case .selection:
            UISelectionFeedbackGenerator().selectionChanged()
        case .success:
            UINotificationFeedbackGenerator().notificationOccurred(.success)
        case .warning:
            UINotificationFeedbackGenerator().notificationOccurred(.warning)
        case .error:
            UINotificationFeedbackGenerator().notificationOccurred(.error)
        }
        #endif
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
