import SwiftUI

public struct SubscriptionListScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var showAddSubscription = false
    @State private var editingSubscription: RecurringSubscription?
    @State private var toastItem: ToastItem?

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
                    Button {
                        themeManager.triggerHapticFeedback(.light)
                        editingSubscription = sub
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

                            Image(systemName: "chevron.right")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(Color(uiColor: .tertiaryLabel))
                        }
                        .padding(.vertical, CentwiseSpacing.xxs)
                    }
                    .tint(.primary)
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            repository.deleteSubscription(id: sub.id)
                            toastItem = ToastItem("Subscription deleted", style: .success)
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
        .toast(item: $toastItem)
        .sheet(isPresented: $showAddSubscription) {
            NavigationStack {
                AddEditSubscriptionScreen { sub in
                    repository.addSubscription(sub)
                    toastItem = ToastItem("Subscription added successfully", style: .success)
                }
            }
        }
        .sheet(item: $editingSubscription) { sub in
            NavigationStack {
                AddEditSubscriptionScreen(editingSubscription: sub) { updated in
                    repository.updateSubscription(updated)
                    toastItem = ToastItem("Subscription updated successfully", style: .success)
                }
            }
        }
    }
}
