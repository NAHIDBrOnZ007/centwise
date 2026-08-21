import SwiftUI

public struct AmountText: View {
    public let amount: Double
    public var type: TransactionType?
    public var font: Font
    public var showSign: Bool

    public init(
        amount: Double,
        type: TransactionType? = nil,
        font: Font = CentwiseTypography.amountMedium,
        showSign: Bool = true
    ) {
        self.amount = amount
        self.type = type
        self.font = font
        self.showSign = showSign
    }

    public var body: some View {
        Text(CurrencyFormatter.shared.formatBDT(amount, showSign: showSign))
            .font(font)
            .foregroundColor(amountColor)
    }

    private var amountColor: Color {
        guard let type = type else {
            return .primary
        }
        switch type {
        case .expense:
            return CentwiseColors.expenseRed
        case .income:
            return CentwiseColors.incomeGreen
        case .transfer:
            return CentwiseColors.transferBlue
        case .refund:
            return CentwiseColors.refundAmber
        }
    }
}
