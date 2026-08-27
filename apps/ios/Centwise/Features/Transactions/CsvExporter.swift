import SwiftUI
import UniformTypeIdentifiers

public enum CsvExporter {

    private static let headerFields = [
        "Date", "Title", "Amount", "Currency", "Type",
        "Category", "Account", "Provider", "Reference", "Balance After", "Notes"
    ]

    /// Builds a CSV string from transactions, newest first.
    public static func transactionsCsv(_ transactions: [CentwiseTransaction]) -> String {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm"

        var lines = [headerFields.joined(separator: ",")]

        for transaction in transactions.sorted(by: { $0.date > $1.date }) {
            let fields = [
                dateFormatter.string(from: transaction.date),
                escape(transaction.title),
                String(format: "%.2f", transaction.amount),
                transaction.currency,
                transaction.type.rawValue,
                escape(transaction.category.name),
                escape(transaction.accountName),
                transaction.provider.rawValue,
                escape(transaction.transactionReference ?? ""),
                transaction.balanceAfter.map { String(format: "%.2f", $0) } ?? "",
                escape(transaction.notes ?? "")
            ]
            lines.append(fields.joined(separator: ","))
        }

        return lines.joined(separator: "\n")
    }

    /// Writes the CSV to a temporary file and returns its URL.
    public static func writeCsvFile(_ transactions: [CentwiseTransaction]) -> URL? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmm"
        let fileName = "centwise_export_\(formatter.string(from: Date())).csv"

        let url = FileManager.default.temporaryDirectory.appendingPathComponent(fileName)

        do {
            try transactionsCsv(transactions).data(using: .utf8)?.write(to: url)
            return url
        } catch {
            return nil
        }
    }

    private static func escape(_ field: String) -> String {
        if field.contains(",") || field.contains("\"") || field.contains("\n") {
            return "\"\(field.replacingOccurrences(of: "\"", with: "\"\""))\""
        }
        return field
    }
}

/// Sheet that offers the generated CSV file through the system share sheet.
public struct CsvExportSheet: View {
    private let transactions: [CentwiseTransaction]
    @Environment(\.dismiss) private var dismiss

    public init(transactions: [CentwiseTransaction]) {
        self.transactions = transactions
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: CentwiseSpacing.lg) {
                if let url = CsvExporter.writeCsvFile(transactions) {
                    Image(systemName: "doc.text")
                        .font(.system(size: 44))
                        .foregroundColor(CentwiseColors.primaryEmerald)

                    Text("Export Ready")
                        .font(CentwiseTypography.title3)

                    Text("\(transactions.count) transactions • CSV format")
                        .font(CentwiseTypography.subheadline)
                        .foregroundColor(.secondary)

                    ShareLink(item: url) {
                        Text("Share Export")
                            .font(CentwiseTypography.bodyMedium)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, CentwiseSpacing.mdSm)
                            .background(
                                RoundedRectangle(cornerRadius: CentwiseSpacing.radiusMd)
                                    .fill(CentwiseColors.primaryEmerald)
                            )
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, CentwiseSpacing.xl)
                } else {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 44))
                        .foregroundColor(CentwiseColors.expenseRed)
                    Text("Export failed")
                        .font(CentwiseTypography.title3)
                }

                Spacer()
            }
            .frame(maxWidth: .infinity)
            .padding(.top, CentwiseSpacing.xxl)
            .navigationTitle("Export CSV")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }
}
