import SwiftUI

public struct ReviewQueueView: View {
    @ObservedObject private var repository = ReviewQueueRepository.shared
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var themeManager = ThemeManager.shared

    @State private var itemToConvert: ReviewQueueItem?

    public init() {}

    public var body: some View {
        ScrollView {
            if repository.items.isEmpty {
                emptyState
            } else {
                LazyVStack(spacing: CentwiseSpacing.md) {
                    ForEach(repository.items) { item in
                        queueCard(item)
                    }
                }
                .padding(.horizontal, CentwiseSpacing.md)
                .padding(.top, CentwiseSpacing.sm)
                .padding(.bottom, CentwiseSpacing.xxl)
            }
        }
        .background(CentwiseColors.background(for: colorScheme, isAmoled: themeManager.isAmoledActive).ignoresSafeArea())
        .navigationTitle("Review Queue")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !repository.items.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Text("\(repository.items.count) pending")
                        .font(CentwiseTypography.caption1)
                        .foregroundColor(.secondary)
                }
            }
        }
        .sheet(item: $itemToConvert) { item in
            NavigationStack {
                AddEditTransactionView(
                    transactionToEdit: CentwiseTransaction(
                        title: item.candidateParty ?? "\(item.sender) Transaction",
                        amount: item.candidateAmount ?? 0.0,
                        type: item.candidateType ?? .expense,
                        category: .general,
                        account: FakeTransactionRepository.shared.accounts.first
                            ?? FinancialAccount(name: item.sender, provider: .bkash, type: .mfs, currentBalance: 0.0),
                        date: item.timestamp,
                        rawSms: item.rawSms
                    ),
                    onSave: {
                        repository.dismissItem(id: item.id)
                    }
                )
            }
        }
    }

    private var emptyState: some View {
        VStack(spacing: CentwiseSpacing.md) {
            ZStack {
                Circle()
                    .fill(CentwiseColors.incomeGreen.opacity(0.12))
                    .frame(width: 72, height: 72)

                Image(systemName: "envelope.badge.shield.halfopen.fill")
                    .font(.system(size: 32))
                    .foregroundColor(CentwiseColors.incomeGreen)
            }
            .padding(.top, CentwiseSpacing.xxl)

            Text("All Caught Up!")
                .font(CentwiseTypography.title2)
                .foregroundColor(.primary)

            Text("No pending SMS messages in your review queue. Financial SMS messages via Shortcuts or Share sheet are automatically converted into transactions.")
                .font(CentwiseTypography.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, CentwiseSpacing.xl)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, CentwiseSpacing.xxl)
    }

    private func queueCard(_ item: ReviewQueueItem) -> some View {
        CentwiseCard {
            VStack(alignment: .leading, spacing: CentwiseSpacing.sm) {
                // Header
                HStack {
                    Text(item.sender)
                        .font(CentwiseTypography.caption1)
                        .fontWeight(.semibold)
                        .foregroundColor(CentwiseColors.primaryEmerald)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            RoundedRectangle(cornerRadius: 6)
                                .fill(CentwiseColors.primaryEmerald.opacity(0.12))
                        )

                    Text(item.timestamp, format: .dateTime.day().month().hour().minute())
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)

                    Spacer()

                    Text(item.reason)
                        .font(CentwiseTypography.caption2)
                        .foregroundColor(.secondary)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.primary.opacity(0.05))
                        )
                }

                // Monospace raw message box
                Text(item.rawSms)
                    .font(.system(size: 12, weight: .regular, design: .monospaced))
                    .foregroundColor(.primary)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(
                        RoundedRectangle(cornerRadius: 8)
                            .fill(colorScheme == .dark ? Color.white.opacity(0.06) : Color.black.opacity(0.04))
                    )

                // Action Buttons
                HStack(spacing: CentwiseSpacing.sm) {
                    Button(action: {
                        repository.dismissItem(id: item.id)
                    }) {
                        Label("Dismiss", systemImage: "xmark")
                            .font(CentwiseTypography.caption1)
                            .foregroundColor(.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(Color.secondary.opacity(0.3), lineWidth: 1)
                            )
                    }
                    .buttonStyle(.plain)

                    Button(action: {
                        itemToConvert = item
                    }) {
                        Label("Convert to Tx", systemImage: "square.and.pencil")
                            .font(CentwiseTypography.caption1)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 8)
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(CentwiseColors.primaryEmerald)
                            )
                    }
                    .buttonStyle(.plain)
                }
                .padding(.top, 4)
            }
        }
    }
}
