import SwiftUI

public struct CurrencyInfo: Identifiable {
    public let code: String
    public let name: String
    public let symbol: String

    public var id: String { code }

    public static let supported: [CurrencyInfo] = [
        CurrencyInfo(code: "BDT", name: "Bangladeshi Taka", symbol: "৳"),
        CurrencyInfo(code: "USD", name: "US Dollar", symbol: "$"),
        CurrencyInfo(code: "EUR", name: "Euro", symbol: "€"),
        CurrencyInfo(code: "GBP", name: "British Pound", symbol: "£"),
        CurrencyInfo(code: "INR", name: "Indian Rupee", symbol: "₹"),
        CurrencyInfo(code: "JPY", name: "Japanese Yen", symbol: "¥"),
        CurrencyInfo(code: "CNY", name: "Chinese Yuan", symbol: "¥"),
        CurrencyInfo(code: "AUD", name: "Australian Dollar", symbol: "A$"),
        CurrencyInfo(code: "CAD", name: "Canadian Dollar", symbol: "C$"),
        CurrencyInfo(code: "SGD", name: "Singapore Dollar", symbol: "S$"),
        CurrencyInfo(code: "MYR", name: "Malaysian Ringgit", symbol: "RM"),
        CurrencyInfo(code: "SAR", name: "Saudi Riyal", symbol: "﷼"),
        CurrencyInfo(code: "AED", name: "UAE Dirham", symbol: "د.إ"),
        CurrencyInfo(code: "QAR", name: "Qatari Riyal", symbol: "﷼")
    ]
}

public struct CurrencyPickerScreen: View {
    @AppStorage("selectedCurrencyCode") private var selectedCode: String = "BDT"
    @State private var searchText: String = ""

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    public init() {}

    private var filteredCurrencies: [CurrencyInfo] {
        let query = searchText.lowercased()
        guard !query.isEmpty else { return CurrencyInfo.supported }
        return CurrencyInfo.supported.filter {
            $0.code.lowercased().contains(query) || $0.name.lowercased().contains(query)
        }
    }

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                ForEach(filteredCurrencies) { currency in
                    currencyRow(currency)
                }

                if filteredCurrencies.isEmpty {
                    Text("No currencies found")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.top, CentwiseSpacing.xl)
                }
            }
            .padding(.horizontal, CentwiseSpacing.md)
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .searchable(text: $searchText, prompt: "Search currency")
        .navigationTitle("Currency")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func currencyRow(_ currency: CurrencyInfo) -> some View {
        let isSelected = currency.code == selectedCode

        return Button {
            selectedCode = currency.code
            themeManager.triggerHapticFeedback(.light)
        } label: {
            HStack(spacing: CentwiseSpacing.md) {
                Text(currency.symbol)
                    .font(CentwiseTypography.title3)
                    .foregroundColor(themeManager.accentColor)
                    .frame(width: 44, height: 44)
                    .background(
                        Circle().fill(themeManager.accentColor.opacity(0.12))
                    )

                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                    Text(currency.code)
                        .font(CentwiseTypography.bodyMedium)
                        .foregroundColor(.primary)
                    Text(currency.name)
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 20))
                        .foregroundColor(themeManager.accentColor)
                }
            }
            .padding(CentwiseSpacing.mdSm)
            .background(
                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusMd, style: .continuous)
                    .fill(CentwiseColors.surface(for: colorScheme, isAmoled: themeManager.isAmoledActive))
            )
            .overlay(
                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusMd, style: .continuous)
                    .strokeBorder(
                        isSelected ? themeManager.accentColor.opacity(0.5) : CentwiseColors.border(for: colorScheme),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}
