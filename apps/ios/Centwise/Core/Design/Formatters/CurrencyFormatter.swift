import Foundation

public final class CurrencyFormatter {
    public static let shared = CurrencyFormatter()

    public static let symbolBDT = "৳"
    public static let codeBDT = "BDT"

    private let standardFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        formatter.groupingSeparator = ","
        formatter.decimalSeparator = "."
        return formatter
    }()

    private let compactFormatter: NumberFormatter = {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 0
        formatter.groupingSeparator = ","
        return formatter
    }()

    public func formatBDT(_ amount: Double, showSign: Bool = false, compact: Bool = false) -> String {
        let formatter = compact ? compactFormatter : standardFormatter
        let absoluteAmount = abs(amount)
        let formattedNumber = formatter.string(from: NSNumber(value: absoluteAmount)) ?? String(format: "%.2f", absoluteAmount)

        if showSign {
            if amount > 0 {
                return "+\(Self.symbolBDT) \(formattedNumber)"
            } else if amount < 0 {
                return "-\(Self.symbolBDT) \(formattedNumber)"
            }
        }

        return "\(Self.symbolBDT) \(formattedNumber)"
    }

    /// Converts English digits (0-9) to Bengali digits (০-৯)
    public func toBengaliDigits(_ text: String) -> String {
        let banglaDigits: [Character: Character] = [
            "0": "০", "1": "১", "2": "২", "3": "৩", "4": "৪",
            "5": "৫", "6": "৬", "7": "৭", "8": "৮", "9": "৯"
        ]
        return String(text.map { banglaDigits[$0] ?? $0 })
    }
}
