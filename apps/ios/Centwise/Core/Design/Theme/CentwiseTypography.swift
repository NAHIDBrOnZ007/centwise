import SwiftUI

public enum CentwiseTypography {
    public static let largeTitle = Font.system(size: 34, weight: .bold, design: .rounded)
    public static let title1 = Font.system(size: 28, weight: .bold, design: .rounded)
    public static let title2 = Font.system(size: 22, weight: .bold, design: .rounded)
    public static let title3 = Font.system(size: 18, weight: .semibold, design: .rounded)
    public static let headline = Font.system(size: 16, weight: .semibold, design: .default)
    public static let body = Font.system(size: 15, weight: .regular, design: .default)
    public static let bodyMedium = Font.system(size: 15, weight: .medium, design: .default)
    public static let subheadline = Font.system(size: 14, weight: .regular, design: .default)
    public static let footnote = Font.system(size: 13, weight: .regular, design: .default)
    public static let caption = Font.system(size: 12, weight: .medium, design: .default)
    public static let caption1 = Font.system(size: 12, weight: .medium, design: .default)
    public static let caption2 = Font.system(size: 11, weight: .regular, design: .default)

    // MARK: - Amount & Numerical Typography (Monospaced digits)
    public static let amountHero = Font.system(size: 32, weight: .bold, design: .rounded).monospacedDigit()
    public static let amountLarge = Font.system(size: 24, weight: .bold, design: .rounded).monospacedDigit()
    public static let amountMedium = Font.system(size: 17, weight: .semibold, design: .rounded).monospacedDigit()
    public static let amountSmall = Font.system(size: 14, weight: .medium, design: .rounded).monospacedDigit()
}
