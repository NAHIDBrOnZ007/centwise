import SwiftUI

public struct AddEditAccountScreen: View {
    public let accountToEdit: FinancialAccount?
    public var onSave: (() -> Void)?

    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var name: String
    @State private var selectedProvider: FinancialProvider
    @State private var selectedType: AccountType
    @State private var lastFour: String
    @State private var initialBalance: String

    public init(accountToEdit: FinancialAccount? = nil, onSave: (() -> Void)? = nil) {
        self.accountToEdit = accountToEdit
        self.onSave = onSave
        _name = State(initialValue: accountToEdit?.name ?? "")
        _selectedProvider = State(initialValue: accountToEdit?.provider ?? .bkash)
        _selectedType = State(initialValue: accountToEdit?.type ?? .mfs)
        _lastFour = State(initialValue: accountToEdit?.lastFourDigits ?? "")
        _initialBalance = State(initialValue: accountToEdit != nil ? String(format: "%.2f", accountToEdit!.currentBalance) : "")
    }

    public var body: some View {
        NavigationStack {
            Form {
                Section("Account Information") {
                    TextField("Account / Wallet Nickname", text: $name)

                    Picker("Provider", selection: $selectedProvider) {
                        ForEach(FinancialProvider.allCases) { prov in
                            Text(prov.rawValue).tag(prov)
                        }
                    }

                    Picker("Account Type", selection: $selectedType) {
                        ForEach(AccountType.allCases) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                }

                Section("Details") {
                    TextField("Last 4 digits (optional)", text: $lastFour)
                        .keyboardType(.numberPad)

                    HStack {
                        Text("Balance ৳")
                        TextField("0.00", text: $initialBalance)
                            .keyboardType(.decimalPad)
                    }
                }
            }
            .navigationTitle(accountToEdit == nil ? "New Account" : "Edit Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let bal = Double(initialBalance) ?? 0.0
                        if let existing = accountToEdit {
                            let updated = FinancialAccount(
                                id: existing.id,
                                name: name.isEmpty ? selectedProvider.rawValue : name,
                                provider: selectedProvider,
                                type: selectedType,
                                lastFourDigits: lastFour.isEmpty ? nil : lastFour,
                                currentBalance: bal,
                                isArchived: existing.isArchived
                            )
                            TransactionRepository.shared.updateAccount(updated)
                        } else {
                            let newAcc = FinancialAccount(
                                name: name.isEmpty ? selectedProvider.rawValue : name,
                                provider: selectedProvider,
                                type: selectedType,
                                lastFourDigits: lastFour.isEmpty ? nil : lastFour,
                                currentBalance: bal
                            )
                            TransactionRepository.shared.addAccount(newAcc)
                        }
                        themeManager.triggerHapticFeedback(.success)
                        onSave?()
                        dismiss()
                    }
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(themeManager.accentColor)
                }
            }
        }
    }
}
