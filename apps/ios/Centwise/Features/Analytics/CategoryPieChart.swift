import Charts
import SwiftUI

public struct CategorySlice: Identifiable {
    public let id: String
    public let name: String
    public let value: Double
    public let color: Color

    public init(id: String = UUID().uuidString, name: String, value: Double, color: Color) {
        self.id = id
        self.name = name
        self.value = value
        self.color = color
    }
}

public struct CategoryPieChart: View {
    private let slices: [CategorySlice]

    public init(slices: [CategorySlice]) {
        self.slices = slices.sorted { $0.value > $1.value }
    }

    private var total: Double {
        slices.reduce(0) { $0 + $1.value }
    }

    public var body: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.md) {
                Text("Category Breakdown")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                if slices.isEmpty || total <= 0 {
                    emptyState
                } else {
                    HStack(spacing: CentwiseSpacing.lg) {
                        donutChart
                            .frame(width: 140, height: 140)

                        legend
                    }
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Category spending breakdown")
        .accessibilityValue(accessibilitySummary)
    }

    private var accessibilitySummary: String {
        guard total > 0 else { return "No category data available" }
        return slices.map {
            "\($0.name), \(Int(($0.value / total * 100).rounded())) percent"
        }.joined(separator: ", ")
    }

    // MARK: - Donut

    private var donutChart: some View {
        ZStack {
            if #available(iOS 17.0, *) {
                Chart(slices) { slice in
                    SectorMark(
                        angle: .value("Amount", slice.value),
                        innerRadius: .ratio(0.62),
                        angularInset: 1.5
                    )
                    .foregroundStyle(slice.color)
                }
                .chartLegend(.hidden)
            } else {
                legacyDonutChart
            }

            VStack(spacing: CentwiseSpacing.xxs) {
                Text(CurrencyFormatter.shared.formatBDT(total, compact: true))
                    .font(CentwiseTypography.amountMedium)
                    .foregroundColor(.primary)
                Text("Total")
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var legacyDonutChart: some View {
        Canvas { context, size in
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2
            let innerRadius = radius * 0.62
            var startAngle = -Double.pi / 2

            for slice in slices {
                let sweep = slice.value / total * Double.pi * 2
                let endAngle = startAngle + sweep

                var path = Path()
                path.move(to: point(center: center, radius: radius, angle: startAngle))
                path.addArc(center: center, radius: radius, startAngle: Angle(radians: startAngle), endAngle: Angle(radians: endAngle), clockwise: false)
                path.addArc(center: center, radius: innerRadius, startAngle: Angle(radians: endAngle), endAngle: Angle(radians: startAngle), clockwise: true)
                path.closeSubpath()

                context.fill(path, with: .color(slice.color))
                startAngle = endAngle
            }
        }
    }

    private func point(center: CGPoint, radius: CGFloat, angle: Double) -> CGPoint {
        CGPoint(
            x: center.x + radius * CGFloat(cos(angle)),
            y: center.y + radius * CGFloat(sin(angle))
        )
    }

    // MARK: - Legend

    private var legend: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.mdSm) {
            ForEach(slices.prefix(5)) { slice in
                HStack(spacing: CentwiseSpacing.sm) {
                    Circle()
                        .fill(slice.color)
                        .frame(width: 10, height: 10)

                    Text(slice.name)
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.primary)
                        .lineLimit(1)

                    Spacer()

                    Text(String(format: "%.0f%%", slice.value / total * 100))
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                        .monospacedDigit()
                }
            }

            if slices.count > 5 {
                Text("+ \(slices.count - 5) more")
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: CentwiseSpacing.sm) {
            Image(systemName: "chart.pie")
                .font(.system(size: 26))
                .foregroundColor(.secondary)
            Text("No category data available")
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, CentwiseSpacing.md)
    }
}
