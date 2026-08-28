import SwiftUI

public struct DataManagementScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var showLoadDemoAlert = false
    @State private var showResetAlert = false
    @State private var showExportSheet = false
    @State private var toastMessage: String?

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
        .overlay(alignment: .bottom) {
            if let toast = toastMessage {
                toastBanner(toast)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .padding(.bottom, 24)
            }
        }
        .sheet(isPresented: $showExportSheet) {
            CsvExportSheet(transactions: repository.transactions)
        }
        .alert("Load Demo Sample Data?", isPresented: $showLoadDemoAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Load Demo Data") {
                let summary = repository.loadSampleDemoData()
                themeManager.triggerHapticFeedback(.success)
                if let summary {
                    showToast("Sample demo data loaded (\(summary.transactions) transactions)")
                } else {
                    showToast("Could not load demo data")
                }
            }
        } message: {
            Text("This will populate your database with realistic sample transactions, accounts, budgets, and subscriptions for previewing Centwise.")
        }
        .alert("Wipe Database & Reset?", isPresented: $showResetAlert) {
            Button("Cancel", role: .cancel) {}
            Button("Wipe Everything", role: .destructive) {
                repository.resetToEmptyDatabase()
                themeManager.triggerHapticFeedback(.warning)
                showToast("Database wiped. Starting completely clean.")
            }
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

    private func showToast(_ message: String) {
        withAnimation(.spring(response: 0.3, dampingFraction: 0.75)) {
            toastMessage = message
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
            withAnimation {
                toastMessage = nil
            }
        }
    }

    private func toastBanner(_ message: String) -> some View {
        HStack(spacing: 10) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundColor(CentwiseColors.incomeGreen)

            Text(message)
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.white)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            Capsule()
                .fill(Color(white: 0.15))
                .shadow(color: .black.opacity(0.3), radius: 10, y: 5)
        )
    }
}
