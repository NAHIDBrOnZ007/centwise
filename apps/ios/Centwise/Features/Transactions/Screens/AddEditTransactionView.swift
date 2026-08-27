import SwiftUI

public struct AddEditTransactionView: View {
    public var transactionToEdit: CentwiseTransaction?
    public var onSave: (() -> Void)?
    public var writesToRepository: Bool
    public var onCommit: ((CentwiseTransaction) -> Void)?

    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var title: String = ""
    @State private var amountString: String = ""
    @State private var selectedType: TransactionType = .expense
    @ObservedObject private var repository = TransactionRepository.shared
    @State private var selectedCategory: TransactionCategory = TransactionRepository.shared.category(id: "food")
    @State private var selectedAccountIndex: Int = 0
    @State private var date: Date = Date()
    @State private var notes: String = ""
    @State private var accounts: [FinancialAccount] = TransactionRepository.shared.accounts

    public init(
        transactionToEdit: CentwiseTransaction? = nil,
        onSave: (() -> Void)? = nil,
        writesToRepository: Bool = true,
        onCommit: ((CentwiseTransaction) -> Void)? = nil
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

                    if !accounts.isEmpty {
                        Picker("Account / Wallet", selection: $selectedAccountIndex) {
                            ForEach(0..<accounts.count, id: \.self) { idx in
                                Text(accounts[idx].name).tag(idx)
                            }
                        }
                    }

                    DatePicker("Date & Time", selection: $date)
                }

                // Notes
                Section(header: Text("Notes (Optional)")) {
                    TextField("Add notes or tags...", text: $notes)
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
                if let tx = transactionToEdit {
                    title = tx.title
                    amountString = String(format: "%.2f", tx.amount)
                    selectedType = tx.type
                    selectedCategory = tx.category
                    date = tx.date
                    notes = tx.notes ?? ""
                } else if let firstCategory = repository.categories.first {
                    selectedCategory = firstCategory
                }
            }
        }
    }

    private func saveTransaction() {
        guard let amount = Double(amountString), !title.isEmpty else { return }

        let fallbackAccount = FinancialAccount(
            id: transactionToEdit?.accountId ?? "default",
            name: transactionToEdit?.accountName ?? "Cash / Wallet",
            provider: transactionToEdit?.provider ?? .bkash,
            type: .mfs,
            currentBalance: 0.0
        )
        let chosenAccount: FinancialAccount
        if accounts.indices.contains(selectedAccountIndex) {
            chosenAccount = accounts[selectedAccountIndex]
        } else if let first = accounts.first {
            chosenAccount = first
        } else {
            chosenAccount = fallbackAccount
        }

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
                TransactionRepository.shared.updateTransaction(updated)
            }
            onCommit?(updated)
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
                TransactionRepository.shared.addTransaction(newTx)
            }
            onCommit?(newTx)
        }

        themeManager.triggerHapticFeedback(.success)
        dismiss()
        onSave?()
    }
}
