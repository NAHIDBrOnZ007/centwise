import SwiftUI

public struct DataManagementScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var showLoadDemoAlert = false
    @State private var showResetAlert = false
    @State private var showExportSheet = false
    @State private var toastItem: ToastItem?
    @State private var isDatabaseOperationInProgress = false

    public init() {}

    private var databaseFileSizeString: String {
        let dbUrl = CentwiseRustBackend.databaseURL()
        if let attrs = try? FileManager.default.attributesOfItem(atPath: dbUrl.path),
           let size = attrs[.size] as? Int64 {
            let kb = Double(size) / 1024.0
            if kb < 1024 {
                return String(format: "%.1f KB", kb)
            } else {
                return String(format: "%.2f MB", kb / 1024.0)
            }
        }
        return "Clean DB"
    }

    public var body: some View {
        List {
            Section("Local Storage") {
                LabeledContent("Database", value: databaseFileSizeString)
                Label("Encrypted on this device", systemImage: "lock.shield")
                    .foregroundStyle(.secondary)
            }

            Section("Current Records") {
                recordRow("Transactions", count: repository.transactions.count, icon: "list.bullet.rectangle")
                recordRow("Accounts", count: repository.accounts.count, icon: "building.columns")
                recordRow("Budgets", count: repository.budgets.count, icon: "chart.pie")
                recordRow("Subscriptions", count: repository.subscriptions.count, icon: "arrow.triangle.2.circlepath")
            }

            Section {
                Button {
                    showLoadDemoAlert = true
                } label: {
                    Label("Load Demo Sample Data", systemImage: "sparkles")
                }

                Button {
                    showExportSheet = true
                } label: {
                    Label("Export Data to CSV", systemImage: "square.and.arrow.up")
                }
            } header: {
                Text("Data & Backup")
            } footer: {
                Text("Demo data adds sample transactions, accounts, budgets, and subscriptions. Export creates a local CSV file.")
            }

            Section {
                Button("Reset Database", role: .destructive) {
                    showResetAlert = true
                }
            } footer: {
                Text("Reset permanently deletes all transactions, budgets, and subscriptions from this device.")
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("Data & Storage")
        .navigationBarTitleDisplayMode(.inline)
        .toast(item: $toastItem)
        .sheet(isPresented: $showExportSheet) {
            CsvExportSheet(transactions: repository.transactions)
        }
        .alert("Load Demo Sample Data?", isPresented: $showLoadDemoAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Load Demo Data") {
                isDatabaseOperationInProgress = true
                repository.loadSampleDemoDataAsync { summary in
                    isDatabaseOperationInProgress = false
                    themeManager.triggerHapticFeedback(summary == nil ? .warning : .success)
                    if let summary {
                        toastItem = ToastItem("Sample data loaded (\(summary.transactions) transactions)", style: .success)
                    } else {
                        toastItem = ToastItem("Could not load demo data", style: .error)
                    }
                }
            }
            .disabled(isDatabaseOperationInProgress)
        } message: {
            Text("This will populate your database with realistic sample transactions, accounts, budgets, and subscriptions for previewing Centwise.")
        }
        .alert("Wipe Database & Reset?", isPresented: $showResetAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Wipe Everything", role: .destructive) {
                isDatabaseOperationInProgress = true
                repository.resetToEmptyDatabaseAsync { succeeded in
                    isDatabaseOperationInProgress = false
                    themeManager.triggerHapticFeedback(succeeded ? .warning : .error)
                    toastItem = ToastItem(
                        succeeded ? "Database wiped. Starting completely clean." : "Could not reset database",
                        style: succeeded ? .info : .error
                    )
                }
            }
            .disabled(isDatabaseOperationInProgress)
        } message: {
            Text("Are you sure you want to delete all transactions, budgets, and subscriptions? This action cannot be undone.")
        }
    }

    private func recordRow(_ title: String, count: Int, icon: String) -> some View {
        LabeledContent {
            Text("\(count)")
                .monospacedDigit()
                .foregroundStyle(.secondary)
        } label: {
            Label(title, systemImage: icon)
        }
    }
}
