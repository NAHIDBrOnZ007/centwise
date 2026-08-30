import SwiftUI

public enum CentwiseColors {
    // MARK: - Brand & Accents
    public static let primaryEmerald = Color(red: 0.0, green: 0.658, blue: 0.419) // #00A86B
    public static let primaryEmeraldDark = Color(red: 0.0, green: 0.50, blue: 0.32)
    public static let primaryEmeraldLight = Color(red: 0.20, green: 0.80, blue: 0.55)

    // MARK: - Bangladesh MFS & Bank Brand Colors
    public static let bKashPink = Color(red: 0.886, green: 0.075, blue: 0.431) // #E2136E
    public static let nagadOrange = Color(red: 0.969, green: 0.580, blue: 0.114) // #F7941D
    public static let rocketPurple = Color(red: 0.549, green: 0.204, blue: 0.580) // #8C3494
    public static let upayBlue = Color(red: 0.0, green: 0.478, blue: 0.80)
    public static let cellfinGreen = Color(red: 0.0, green: 0.60, blue: 0.30)
    public static let cityBankRed = Color(red: 0.85, green: 0.10, blue: 0.15)
    public static let bracBankBlue = Color(red: 0.05, green: 0.25, blue: 0.60)
    public static let easternBankGold = Color(red: 0.80, green: 0.60, blue: 0.10)

    // MARK: - Modern Aesthetic Accents
    public static let cyberCyan = Color(red: 0.02, green: 0.71, blue: 0.83)        // #06B6D4
    public static let electricIndigo = Color(red: 0.39, green: 0.40, blue: 0.95)   // #6366F1
    public static let aestheticLavender = Color(red: 0.65, green: 0.42, blue: 0.96)// #A855F7
    public static let crimsonRuby = Color(red: 0.88, green: 0.11, blue: 0.28)      // #E11D48
    public static let sunsetAmber = Color(red: 0.96, green: 0.62, blue: 0.04)      // #F59E0B
    public static let mintBreeze = Color(red: 0.06, green: 0.73, blue: 0.51)       // #10B981
    public static let roseGold = Color(red: 0.98, green: 0.44, blue: 0.52)         // #FB7185
    public static let graphiteSlate = Color(red: 0.39, green: 0.45, blue: 0.55)    // #64748B
    public static let nordicSky = Color(red: 0.08, green: 0.53, blue: 0.89)        // #1488E0
    public static let sakuraBloom = Color(red: 0.96, green: 0.45, blue: 0.71)      // #F472B6
    public static let midnightAmethyst = Color(red: 0.49, green: 0.23, blue: 0.93) // #7C3AED
    public static let neonCoral = Color(red: 0.99, green: 0.45, blue: 0.40)        // #FD7266
    public static let auroraTeal = Color(red: 0.08, green: 0.72, blue: 0.65)       // #14B8A6
    public static let matchaGreen = Color(red: 0.52, green: 0.80, blue: 0.09)      // #84CC16
    public static let warmChampagne = Color(red: 0.85, green: 0.47, blue: 0.02)    // #D97706
    public static let obsidianCharcoal = Color(red: 0.20, green: 0.25, blue: 0.33) // #334155

    // MARK: - Financial Semantics
    public static let incomeGreen = Color(red: 0.13, green: 0.77, blue: 0.36) // #22C55E
    public static let expenseRed = Color(red: 0.94, green: 0.27, blue: 0.27)  // #EF4444
    public static let transferBlue = Color(red: 0.23, green: 0.51, blue: 0.96) // #3B82F6
    public static let refundAmber = Color(red: 0.96, green: 0.62, blue: 0.07)  // #F59E0B

    // MARK: - Backgrounds & Surfaces
    public static let darkBackground = Color(red: 0.05, green: 0.07, blue: 0.10) // #0D1117
    public static let darkSurface = Color(red: 0.09, green: 0.12, blue: 0.16)    // #161F28
    public static let darkSurfaceSecondary = Color(red: 0.13, green: 0.17, blue: 0.22)
    public static let darkBorder = Color(white: 1.0, opacity: 0.08)

    public static let lightBackground = Color(red: 0.96, green: 0.97, blue: 0.98) // #F5F7FA
    public static let lightSurface = Color(white: 1.0)
    public static let lightSurfaceSecondary = Color(red: 0.93, green: 0.94, blue: 0.96)
    public static let lightBorder = Color(white: 0.0, opacity: 0.06)

    public static let amoledBackground = Color.black
    public static let amoledSurface = Color(red: 0.07, green: 0.07, blue: 0.07)

    // MARK: - Adaptive Helpers
    public static func surface(for scheme: ColorScheme, isAmoled: Bool = false) -> Color {
        if scheme == .dark {
            return isAmoled ? amoledSurface : darkSurface
        }
        return lightSurface
    }

    public static func surfaceSecondary(for scheme: ColorScheme, isAmoled: Bool = false) -> Color {
        if scheme == .dark {
            return isAmoled ? Color(red: 0.12, green: 0.12, blue: 0.12) : darkSurfaceSecondary
        }
        return lightSurfaceSecondary
    }

    public static func background(for scheme: ColorScheme, isAmoled: Bool = false) -> Color {
        if scheme == .dark {
            return isAmoled ? amoledBackground : darkBackground
        }
        return lightBackground
    }

    public static func border(for scheme: ColorScheme) -> Color {
        scheme == .dark ? darkBorder : lightBorder
    }
}
