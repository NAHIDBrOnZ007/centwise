import SwiftUI

public struct SubscriptionListScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var showAddSubscription = false

    public init() {}

    public var body: some View {
        List {
            Section {
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
                .listRowInsets(EdgeInsets())
                .listRowBackground(Color.clear)

            }

            Section("Your Subscriptions") {
                ForEach(repository.subscriptions) { sub in
                    NavigationLink {
                        AddEditSubscriptionScreen(editingSubscription: sub) { updated in
                            repository.updateSubscription(updated)
                        }
                    } label: {
                        HStack(spacing: CentwiseSpacing.mdSm) {
                            Image(systemName: sub.icon)
                                .font(.system(size: 18, weight: .regular))
                                .foregroundColor(themeManager.accentColor)
                                .frame(width: 28, height: 28)

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
                        .padding(.vertical, CentwiseSpacing.xxs)
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            repository.deleteSubscription(id: sub.id)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Subscriptions")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    themeManager.triggerHapticFeedback(.light)
                    showAddSubscription = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(themeManager.accentColor)
                }
            }
        }
        .sheet(isPresented: $showAddSubscription) {
            NavigationStack {
                AddEditSubscriptionScreen { sub in
                    repository.addSubscription(sub)
                }
            }
        }
    }
}
