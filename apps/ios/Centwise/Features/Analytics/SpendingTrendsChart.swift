import Charts
import SwiftUI

public struct TrendPoint: Identifiable {
    public let id: String
    public let label: String
    public let value: Double

    public init(label: String, value: Double) {
        self.id = label + String(value)
        self.label = label
        self.value = value
    }
}

public struct SpendingTrendsChart: View {
    private let points: [TrendPoint]

    @ObservedObject private var themeManager = ThemeManager.shared

    public init(points: [TrendPoint]) {
        self.points = points
    }

    private var trendDirection: (icon: String, color: Color)? {
        guard points.count >= 2 else { return nil }
        let last = points[points.count - 1].value
        let previous = points[points.count - 2].value
        guard previous > 0 else { return nil }

        let change = (last - previous) / previous
        if change > 0.05 {
            return ("arrow.up.right", CentwiseColors.expenseRed)
        } else if change < -0.05 {
            return ("arrow.down.right", CentwiseColors.incomeGreen)
        }
        return nil
    }

    public var body: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                HStack {
                    Text("Spending Trends")
                        .font(CentwiseTypography.headline)
                        .foregroundColor(.primary)

                    Spacer()

                    if let trend = trendDirection {
                        Label(String(format: "%.0f%%", abs((points[points.count - 1].value - points[points.count - 2].value) / max(points[points.count - 2].value, 1) * 100)), systemImage: trend.icon)
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(trend.color)
                    }
                }

                if points.isEmpty {
                    Text("No spending data for this period")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, CentwiseSpacing.md)
                } else {
                    Chart(points) { point in
                        BarMark(
                            x: .value("Period", point.label),
                            y: .value("Spending", point.value)
                        )
                        .foregroundStyle(themeManager.accentColor)
                        .cornerRadius(6)
                    }
                    .frame(height: 120)
                    .chartXAxis {
                        AxisMarks { _ in
                            AxisGridLine()
                            AxisValueLabel()
                        }
                    }
                    .chartYAxis {
                        AxisMarks { _ in
                            AxisGridLine()
                            AxisValueLabel()
                        }
                    }
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Spending trends")
        .accessibilityValue(accessibilitySummary)
    }

    private var accessibilitySummary: String {
        points.map {
            "\($0.label): \(CurrencyFormatter.shared.formatBDT($0.value, showSign: false))"
        }.joined(separator: ", ")
    }

}
