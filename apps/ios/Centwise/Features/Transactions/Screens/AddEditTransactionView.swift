import SwiftUI

public struct AddEditTransactionView: View {
    public var transactionToEdit: CentwiseTransaction?
    public var onSave: (() -> Void)?
    public var writesToRepository: Bool
    public var onCommit: ((CentwiseTransaction) -> Bool)?

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var title: String = ""
    @State private var amountString: String = ""
    @State private var selectedType: TransactionType = .expense
    @ObservedObject private var repository = TransactionRepository.shared
    @State private var selectedCategory: TransactionCategory = TransactionRepository.shared.category(id: "food")
    @State private var selectedAccountId: String = ""
    @State private var selectedProvider: FinancialProvider = .cash
    @State private var date: Date = Date()
    @State private var notes: String = ""
    @State private var accounts: [FinancialAccount] = TransactionRepository.shared.accounts
    @State private var saveError: String?

    public init(
        transactionToEdit: CentwiseTransaction? = nil,
        onSave: (() -> Void)? = nil,
        writesToRepository: Bool = true,
        onCommit: ((CentwiseTransaction) -> Bool)? = nil
    ) {
        self.transactionToEdit = transactionToEdit
        self.onSave = onSave
        self.writesToRepository = writesToRepository
        self.onCommit = onCommit
    }

    public var body: some View {
        NavigationStack {
            Form {
                // Amount Hero Field
                Section {
                    VStack(alignment: .center, spacing: CentwiseSpacing.xs) {
                        Text("AMOUNT")
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)

                        HStack(alignment: .firstTextBaseline, spacing: CentwiseSpacing.xs) {
                            Text("৳")
                                .font(CentwiseTypography.title1)
                                .foregroundColor(.secondary)
                            TextField("0.00", text: $amountString)
                                .font(CentwiseTypography.amountHero)
                                .keyboardType(.decimalPad)
                                .multilineTextAlignment(.leading)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, CentwiseSpacing.sm)
                }

                // Type Picker
                Section {
                    Picker("Transaction Type", selection: $selectedType) {
                        ForEach(TransactionType.allCases) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                // Basic Details
                Section(header: Text("Details")) {
                    TextField("Merchant / Title (e.g. Foodpanda)", text: $title)

                    Picker("Category", selection: $selectedCategory) {
                        ForEach(repository.categories) { cat in
                            HStack {
                                Image(systemName: cat.icon)
                                Text(cat.name)
                            }
                            .tag(cat)
                        }
                    }

                    Picker("Account / Wallet", selection: $selectedAccountId) {
                        Label("Cash / Unassigned", systemImage: "banknote")
                            .tag("")
                        ForEach(accounts) { account in
                            Label(account.name, systemImage: account.type.defaultIcon)
                                .tag(account.id)
                        }
                    }

                    if selectedAccountId.isEmpty {
                        Picker("Create Account As", selection: $selectedProvider) {
                            ForEach(FinancialProvider.allCases) { provider in
                                Label(provider.rawValue, systemImage: provider.icon)
                                    .tag(provider)
                            }
                        }
                    }

                    DatePicker("Date & Time", selection: $date)
                }

                // Notes
                Section(header: Text("Notes (Optional)")) {
                    TextField("Add notes or tags...", text: $notes)
                }

                if let saveError {
                    Section {
                        Label(saveError, systemImage: "exclamationmark.triangle.fill")
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(transactionToEdit == nil ? "New Transaction" : "Edit Transaction")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        saveTransaction()
                    }
                    .font(CentwiseTypography.bodyMedium)
                    .foregroundColor(themeManager.accentColor)
                    .disabled(amountString.isEmpty || title.isEmpty)
                }
            }
            .onAppear {
                accounts = repository.accounts

                if let tx = transactionToEdit {
                    title = tx.title
                    amountString = String(format: "%.2f", tx.amount)
                    selectedType = tx.type
                    selectedCategory = tx.category
                    date = tx.date
                    notes = tx.notes ?? ""
                    selectedAccountId = tx.accountId
                    selectedProvider = tx.provider
                } else if let firstCategory = repository.categories.first {
                    selectedCategory = firstCategory
                }
            }
        }
    }

    private func saveTransaction() {
        guard let amount = Double(amountString),
              !title.isEmpty
        else { return }

        let chosenAccount = accounts.first { $0.id == selectedAccountId } ?? FinancialAccount(
            id: "",
            name: selectedProvider == .cash ? "Cash / Unassigned" : selectedProvider.rawValue,
            provider: selectedProvider,
            type: accountType(for: selectedProvider),
            currentBalance: 0
        )

        let saved: Bool

        if let existing = transactionToEdit {
            var updated = existing
            updated.title = title
            updated.amount = amount
            updated.type = selectedType
            updated.category = selectedCategory
            updated.accountId = chosenAccount.id
            updated.accountName = chosenAccount.name
            updated.provider = chosenAccount.provider
            updated.date = date
            updated.notes = notes.isEmpty ? nil : notes

            if writesToRepository {
                saved = TransactionRepository.shared.updateTransaction(updated)
            } else {
                saved = onCommit?(updated) ?? false
            }
        } else {
            let newTx = CentwiseTransaction(
                title: title,
                amount: amount,
                type: selectedType,
                category: selectedCategory,
                date: date,
                accountId: chosenAccount.id,
                accountName: chosenAccount.name,
                provider: chosenAccount.provider,
                notes: notes.isEmpty ? nil : notes,
                isAutoTracked: false
            )
            if writesToRepository {
                saved = TransactionRepository.shared.addTransaction(newTx)
            } else {
                saved = onCommit?(newTx) ?? false
            }
        }

        guard saved else {
            saveError = "Centwise could not save this transaction. Check the selected account and try again."
            themeManager.triggerHapticFeedback(.error)
            return
        }
        saveError = nil
        themeManager.triggerHapticFeedback(.success)
        dismiss()
        onSave?()
    }

    private func accountType(for provider: FinancialProvider) -> AccountType {
        switch provider {
        case .bkash, .nagad, .rocket, .upay, .cellfin: return .mfs
        case .cash: return .cash
        case .other: return .bank
        default: return .bank
        }
    }
}
