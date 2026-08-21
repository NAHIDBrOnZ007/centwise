import SwiftUI

public struct AddEditAccountScreen: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var name: String = ""
    @State private var selectedProvider: FinancialProvider = .bkash
    @State private var selectedType: AccountType = .mfs
    @State private var lastFour: String = ""
    @State private var initialBalance: String = ""

    public init() {}

    public var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("Account Information")) {
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

                Section(header: Text("Details")) {
                    TextField("Last 4 digits (optional)", text: $lastFour)
                        .keyboardType(.numberPad)

                    HStack {
                        Text("Starting Balance ৳")
                        TextField("0.00", text: $initialBalance)
                            .keyboardType(.decimalPad)
                    }
                }
            }
            .navigationTitle("New Account")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let bal = Double(initialBalance) ?? 0.0
                        let newAcc = FinancialAccount(
                            name: name.isEmpty ? selectedProvider.rawValue : name,
                            provider: selectedProvider,
                            type: selectedType,
                            lastFourDigits: lastFour.isEmpty ? nil : lastFour,
                            currentBalance: bal
                        )
                        FakeTransactionRepository.shared.addAccount(newAcc)
                        themeManager.triggerHapticFeedback(.success)
                        dismiss()
                    }
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(themeManager.accentColor)
                }
            }
        }
    }
}
