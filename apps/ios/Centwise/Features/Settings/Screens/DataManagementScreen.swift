import SwiftUI

public struct DataManagementScreen: View {
    @ObservedObject private var repository = TransactionRepository.shared
    @ObservedObject private var themeManager = ThemeManager.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    @State private var showLoadDemoAlert = false
    @State private var showResetAlert = false
    @State private var showExportSheet = false
    @State private var toastMessage: String?
    @State private var isDatabaseHealthy = true

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
        ScrollView {
            VStack(alignment: .leading, spacing: CentwiseSpacing.lg) {
                // Storage Status & Metrics Card
                databaseOverviewCard

                // Data Records Breakdown Card
                recordsBreakdownCard

                // Actions Section
                sectionHeader("Data & Backup Options")

                sectionCard {
                    // Load Demo Data Action
                    Button {
                        showLoadDemoAlert = true
                    } label: {
                        actionRow(
                            icon: "sparkles",
                            iconColor: themeManager.accentColor,
                            title: "Load Demo Sample Data",
                            subtitle: "Populate realistic bKash, Nagad & bank transactions for demo"
                        )
                    }
                    .buttonStyle(.plain)

                    Divider().padding(.leading, 56)

                    // Export CSV Action
                    Button {
                        showExportSheet = true
                    } label: {
                        actionRow(
                            icon: "square.and.arrow.up.fill",
                            iconColor: CentwiseColors.transferBlue,
                            title: "Export Data to CSV",
                            subtitle: "Download all records as a spreadsheet"
                        )
                    }
                    .buttonStyle(.plain)

                    Divider().padding(.leading, 56)

                    // Reset Database Action
                    Button {
                        showResetAlert = true
                    } label: {
                        actionRow(
                            icon: "trash.fill",
                            iconColor: .red,
                            title: "Reset Database (Start Clean)",
                            subtitle: "Permanently delete all transactions and reset"
                        )
                    }
                    .buttonStyle(.plain)
                }

                // Storage Notice
                storageInfoNotice
            }
            .padding(.horizontal, CentwiseSpacing.md)
            .padding(.top, CentwiseSpacing.sm)
            .padding(.bottom, CentwiseSpacing.xxl)
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
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

    // MARK: - Subviews

    private var databaseOverviewCard: some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                HStack {
                    ZStack {
                        Circle()
                            .fill(CentwiseColors.incomeGreen.opacity(0.15))
                            .frame(width: 44, height: 44)

                        Image(systemName: "internaldrive.fill")
                            .font(.system(size: 20))
                            .foregroundColor(CentwiseColors.incomeGreen)
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        HStack(spacing: 6) {
                            Text("Local Storage")
                                .font(CentwiseTypography.headline)
                                .foregroundColor(.primary)

                            Circle()
                                .fill(CentwiseColors.incomeGreen)
                                .frame(width: 8, height: 8)
                        }

                        Text("Encrypted On-Device Database")
                            .font(CentwiseTypography.caption2)
                            .foregroundColor(.secondary)
                    }

                    Spacer()

                    Text(databaseFileSizeString)
                        .font(CentwiseTypography.caption1)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(
                            Capsule()
                                .fill(Color.secondary.opacity(0.12))
                        )
                }
            }
        }
    }

    private var recordsBreakdownCard: some View {
        CentwiseCard {
            VStack(spacing: CentwiseSpacing.md) {
                HStack {
                    Text("CURRENT DATABASE RECORDS")
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .foregroundColor(.secondary)
                    Spacer()
                }

                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: CentwiseSpacing.sm) {
                    metricPill(
                        title: "Transactions",
                        count: "\(repository.transactions.count)",
                        icon: "list.bullet.rectangle.portrait.fill",
                        color: CentwiseColors.primaryEmerald
                    )

                    metricPill(
                        title: "Accounts",
                        count: "\(repository.accounts.count)",
                        icon: "building.columns.fill",
                        color: CentwiseColors.transferBlue
                    )

                    metricPill(
                        title: "Budgets",
                        count: "\(repository.budgets.count)",
                        icon: "chart.pie.fill",
                        color: .orange
                    )

                    metricPill(
                        title: "Subscriptions",
                        count: "\(repository.subscriptions.count)",
                        icon: "arrow.triangle.2.circlepath",
                        color: Color(red: 0.35, green: 0.34, blue: 0.84)
                    )
                }
            }
        }
    }

    private func metricPill(title: String, count: String, icon: String, color: Color) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundColor(color)
                .frame(width: 32, height: 32)
                .background(color.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8))

            VStack(alignment: .leading, spacing: 1) {
                Text(count)
                    .font(CentwiseTypography.headline)
                    .foregroundColor(.primary)

                Text(title)
                    .font(CentwiseTypography.caption2)
                    .foregroundColor(.secondary)
            }

            Spacer()
        }
        .padding(10)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.secondary.opacity(0.06))
        )
    }

    private func actionRow(icon: String, iconColor: Color, title: String, subtitle: String) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.system(size: 22))
                .foregroundColor(iconColor)
                .frame(width: 28, height: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(iconColor == .red ? .red : .primary)

                Text(subtitle)
                    .font(.system(size: 12))
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }

            Spacer()

            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(Color(white: 0.75))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 13)
        .contentShape(Rectangle())
    }

    private var storageInfoNotice: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "lock.shield.fill")
                .font(.system(size: 18))
                .foregroundColor(CentwiseColors.primaryEmerald)

            Text("All transactions and accounts are stored 100% locally on your device in your private SQLite database. No data is ever sent to external cloud servers.")
                .font(CentwiseTypography.caption1)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, CentwiseSpacing.xs)
    }

    private func sectionHeader(_ title: String) -> some View {
        Text(title.uppercased())
            .font(.system(size: 12, weight: .semibold, design: .rounded))
            .foregroundColor(.secondary)
            .padding(.horizontal, CentwiseSpacing.xs)
    }

    private func sectionCard<Content: View>(@ViewBuilder content: @escaping () -> Content) -> some View {
        CentwiseCard {
            VStack(spacing: 0) {
                content()
            }
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
