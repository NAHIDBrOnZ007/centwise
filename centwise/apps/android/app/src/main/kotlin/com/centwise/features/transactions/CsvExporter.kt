package com.centwise.features.transactions

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.centwise.data.fakes.FakeTransactionRepository
import com.centwise.data.models.TransactionItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val headerFields = listOf(
        "Date", "Title", "Amount", "Type", "Category",
        "Payment Method", "Note"
    )

    fun transactionsCsv(transactions: List<TransactionItem>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val lines = mutableListOf(headerFields.joinToString(","))

        transactions.sortedByDescending { it.timestamp }.forEach { transaction ->
            val fields = listOf(
                dateFormat.format(Date(transaction.timestamp)),
                escape(transaction.title),
                String.format(Locale.US, "%.2f", transaction.amount),
                transaction.type.displayName,
                escape(transaction.category),
                escape(transaction.paymentMethod),
                escape(transaction.note ?: "")
            )
            lines.add(fields.joinToString(","))
        }

        return lines.joinToString("\n")
    }

    fun writeCsvFile(context: Context, transactions: List<TransactionItem>): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "centwise_export_$timestamp.csv")
            file.writeText(transactionsCsv(transactions))
            file
        } catch (exception: Exception) {
            null
        }
    }

    /** Exports current transactions and opens the system share sheet. */
    fun shareExport(context: Context): Boolean {
        val transactions = FakeTransactionRepository.shared.transactions.value
        val file = writeCsvFile(context, transactions) ?: return false

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Centwise export"))
        return true
    }

    private fun escape(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
