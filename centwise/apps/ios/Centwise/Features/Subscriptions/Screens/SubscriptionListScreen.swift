import SwiftUI

public struct SubscriptionListScreen: View {
    @ObservedObject private var repository = FakeTransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.isAmoledActive) private var isAmoled

    @State private var showAddSubscription = false

    public init() {}

    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // Total Monthly Commitments Card
                let totalMonthly = repository.subscriptions.filter { $0.isActive }.reduce(0) { $0 + $1.amount }

                CentwiseCard {
                    VStack(alignment: .leading, spacing: CentwiseSpacing.xs) {
                        Text("Monthly Recurring Bills")
                            .font(CentwiseTypography.subheadline)
                            .foregroundColor(.secondary)

                        Text(CurrencyFormatter.shared.formatBDT(totalMonthly))
                            .font(CentwiseTypography.amountHero)
                            .foregroundColor(.primary)

                        Text("\(repository.subscriptions.count) Active Subscriptions")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, CentwiseSpacing.xs)
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.xs)

                // Subscriptions List
                VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                    HStack {
                        Text("Your Subscriptions")
                            .font(CentwiseTypography.headline)
                            .foregroundColor(.primary)
                        Spacer()
                        Button {
                            themeManager.triggerHapticFeedback(.light)
                            showAddSubscription = true
                        } label: {
                            Label("Add", systemImage: "plus")
                                .font(CentwiseTypography.bodyMedium)
                        }
                        .foregroundColor(themeManager.accentColor)
                    }
                    .padding(.horizontal, CentwiseSpacing.md)

                    CentwiseCard {
                        ForEach(Array(repository.subscriptions.enumerated()), id: \.element.id) { idx, sub in
                            HStack(spacing: CentwiseSpacing.mdSm) {
                                Circle()
                                    .fill(sub.provider.brandColor.opacity(0.15))
                                    .frame(width: 40, height: 40)
                                    .overlay(
                                        Image(systemName: sub.icon)
                                            .font(.system(size: 16, weight: .semibold))
                                            .foregroundColor(sub.provider.brandColor)
                                    )

                                VStack(alignment: .leading, spacing: CentwiseSpacing.xxs) {
                                    Text(sub.name)
                                        .font(CentwiseTypography.bodyMedium)
                                        .foregroundColor(.primary)

                                    Text("Due on " + DateFormatterHelper.shared.formatRelativeOrDate(sub.nextDueDate))
                                        .font(CentwiseTypography.caption2)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                VStack(alignment: .trailing, spacing: CentwiseSpacing.xxs) {
                                    Text(CurrencyFormatter.shared.formatBDT(sub.amount, compact: true))
                                        .font(CentwiseTypography.amountMedium)
                                        .foregroundColor(.primary)

                                    Text(sub.billingCycle)
                                        .font(CentwiseTypography.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }
                            .padding(.vertical, CentwiseSpacing.xs)

                            if idx < repository.subscriptions.count - 1 {
                                Divider()
                            }
                        }
                    }
                    .padding(.horizontal, CentwiseSpacing.md)
                }
            }
            .padding(.bottom, 80)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: isAmoled).ignoresSafeArea())
        .navigationTitle("Subscriptions")
        .sheet(isPresented: $showAddSubscription) {
            NavigationStack {
                AddEditSubscriptionScreen { subscription in
                    repository.addSubscription(subscription)
                }
            }
        }
    }
}
