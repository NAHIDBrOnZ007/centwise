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

    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared
    @State private var appeared = false

    public init(points: [TrendPoint]) {
        self.points = points
    }

    private var maxValue: Double {
        max(points.map(\.value).max() ?? 0, 1)
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
                    HStack(alignment: .bottom, spacing: CentwiseSpacing.mdSm) {
                        ForEach(points) { point in
                            barColumn(point)
                        }
                    }
                    .frame(height: 120)
                }
            }
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.6)) {
                appeared = true
            }
        }
    }

    private func barColumn(_ point: TrendPoint) -> some View {
        let barHeight = appeared ? 100 * CGFloat(point.value / maxValue) : 0

        return VStack(spacing: CentwiseSpacing.xs) {
            Text(CurrencyFormatter.shared.formatBDT(point.value, compact: true))
                .font(CentwiseTypography.caption2)
                .foregroundColor(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.5)

            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [themeManager.accentColor.opacity(0.65), themeManager.accentColor],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                )
                .frame(height: max(barHeight, 4))

            Text(point.label)
                .font(CentwiseTypography.caption2)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}
