import SwiftUI

public struct BudgetCarousel: View {
    public let budgets: [CategoryBudget]
    public var onSelectBudget: ((CategoryBudget) -> Void)? = nil

    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme

    public init(
        budgets: [CategoryBudget],
        onSelectBudget: ((CategoryBudget) -> Void)? = nil
    ) {
        self.budgets = budgets
        self.onSelectBudget = onSelectBudget
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
            HStack {
                Text("Monthly Budgets")
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)
                Spacer()
                Text("View All")
                    .font(CentwiseTypography.caption1)
                    .foregroundColor(themeManager.accentColor)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: CentwiseSpacing.mdSm) {
                    ForEach(budgets) { budget in
                        budgetCard(budget)
                    }
                }
                .padding(.vertical, CentwiseSpacing.xs)
            }
        }
    }

    @ViewBuilder
    private func budgetCard(_ budget: CategoryBudget) -> some View {
        Button(action: {
            themeManager.triggerHapticFeedback(.light)
            onSelectBudget?(budget)
        }) {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                // Header: Icon + Category Name
                HStack(spacing: CentwiseSpacing.xs) {
                    Image(systemName: budget.categoryIcon)
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundColor(Color(hex: budget.categoryColorHex) ?? themeManager.accentColor)

                    Text(budget.categoryName)
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.primary)
                        .lineLimit(1)
                }

                Spacer(minLength: 4)

                // Progress Bar
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(CentwiseColors.surfaceSecondary(for: colorScheme))
                            .frame(height: 6)

                        Capsule()
                            .fill(budget.isOverBudget ? CentwiseColors.expenseRed : (Color(hex: budget.categoryColorHex) ?? themeManager.accentColor))
                            .frame(width: geo.size.width * CGFloat(budget.percentage), height: 6)
                    }
                }
                .frame(height: 6)

                // Remaining Text
                HStack {
                    Text(CurrencyFormatter.shared.formatBDT(budget.currentSpent, showSign: false, compact: true))
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)
                    Spacer()
                    Text("of " + CurrencyFormatter.shared.formatBDT(budget.budgetLimit, showSign: false, compact: true))
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .frame(width: 156, height: 104)
            .padding(CentwiseSpacing.mdSm)
            .glassCard(cornerRadius: CentwiseSpacing.radiusMd)
        }
        .buttonStyle(.plain)
    }
}
