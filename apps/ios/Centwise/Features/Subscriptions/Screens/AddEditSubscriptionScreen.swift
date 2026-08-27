import SwiftUI

public struct AddEditSubscriptionScreen: View {
    private let editingSubscription: RecurringSubscription?
    private let onSave: (RecurringSubscription) -> Void

    @State private var name: String = ""
    @State private var amountText: String = ""
    @State private var billingCycle: String = "Monthly"
    @State private var nextDueDate: Date = Calendar.current.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    @State private var provider: FinancialProvider = .bkash
    @State private var isActive: Bool = true

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    private let cycles = ["Weekly", "Monthly", "Quarterly", "Yearly"]

    public init(
        editingSubscription: RecurringSubscription? = nil,
        onSave: @escaping (RecurringSubscription) -> Void
    ) {
        self.editingSubscription = editingSubscription
        self.onSave = onSave
    }

    public var body: some View {
        Form {
            Section {
                TextField("Service name (e.g. Netflix)", text: $name)
                TextField("Amount (৳)", text: $amountText)
                    .keyboardType(.decimalPad)
            } header: {
                Text("Subscription Info")
            }

            Section("Billing") {
                Picker("Cycle", selection: $billingCycle) {
                    ForEach(cycles, id: \.self) { cycle in
                        Text(cycle).tag(cycle)
                    }
                }

                DatePicker(
                    "Next due date",
                    selection: $nextDueDate,
                    displayedComponents: .date
                )
            }

            Section("Paid From") {
                Picker("Account", selection: $provider) {
                    ForEach(FinancialProvider.allCases) { provider in
                        Label(provider.rawValue, systemImage: provider.icon)
                            .tag(provider)
                    }
                }
            }

            Section {
                Toggle("Active", isOn: $isActive)
                    .tint(themeManager.accentColor)
            } footer: {
                Text("Paused subscriptions stop reminders but keep their history.")
            }
        }
        .navigationTitle(editingSubscription == nil ? "New Subscription" : "Edit Subscription")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button("Save") { save() }
                    .font(CentwiseTypography.bodyMedium)
                    .disabled(!isValid)
            }
        }
        .onAppear(perform: populateFields)
    }

    // MARK: - Actions

    private var isValid: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty &&
        Double(amountText.replacingOccurrences(of: ",", with: ".")) != nil
    }

    private func populateFields() {
        guard let subscription = editingSubscription, name.isEmpty else { return }
        name = subscription.name
        amountText = String(format: "%.0f", subscription.amount)
        billingCycle = subscription.billingCycle
        nextDueDate = subscription.nextDueDate
        provider = subscription.provider
        isActive = subscription.isActive
    }

    private func save() {
        guard let amount = Double(amountText.replacingOccurrences(of: ",", with: ".")) else { return }

        let subscription = RecurringSubscription(
            id: editingSubscription?.id ?? UUID().uuidString,
            name: name.trimmingCharacters(in: .whitespaces),
            amount: amount,
            billingCycle: billingCycle,
            nextDueDate: nextDueDate,
            provider: provider,
            isActive: isActive,
            icon: "play.tv.fill"
        )
        onSave(subscription)
        dismiss()
    }
}
