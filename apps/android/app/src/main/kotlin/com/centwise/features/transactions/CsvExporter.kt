package com.centwise.features.transactions

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.centwise.data.repository.TransactionRepository
import com.centwise.data.models.TransactionItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CsvExporter {

    private val headerFields = listOf(
        "Date", "Title", "Amount", "Type", "Category",
        "Payment Method", "Reference", "Note", "Raw SMS"
    )

    private val reviewQueueHeaderFields = listOf(
        "Date", "Sender", "Reason", "Candidate Amount", "Candidate Type",
        "Candidate Party", "Reference", "Raw SMS"
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
                escape(transaction.reference ?: ""),
                escape(transaction.note ?: ""),
                escape(transaction.rawSms ?: "")
            )
            lines.add(fields.joinToString(","))
        }

        return lines.joinToString("\n")
    }

    fun reviewQueueCsv(items: List<com.centwise.data.models.ReviewQueueItem>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val lines = mutableListOf(reviewQueueHeaderFields.joinToString(","))

        items.sortedByDescending { it.timestamp }.forEach { item ->
            val fields = listOf(
                dateFormat.format(Date(item.timestamp)),
                escape(item.sender),
                escape(item.reason),
                item.candidateAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                escape(item.candidateType?.displayName ?: ""),
                escape(item.candidateParty ?: ""),
                escape(item.reference ?: ""),
                escape(item.rawSms)
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

    fun writeReviewQueueCsvFile(context: Context, items: List<com.centwise.data.models.ReviewQueueItem>): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "centwise_review_queue_$timestamp.csv")
            file.writeText(reviewQueueCsv(items))
            file
        } catch (exception: Exception) {
            null
        }
    }

    /** Exports current transactions and opens the system share sheet. */
    suspend fun shareExport(context: Context): Boolean {
        val transactions = TransactionRepository.shared.transactions.value
        val uri = withContext(Dispatchers.IO) {
            val file = writeCsvFile(context, transactions) ?: return@withContext null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } ?: return false

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Centwise export"))
        return true
    }

    /** Exports review queue items and opens the system share sheet. */
    suspend fun shareReviewQueueExport(context: Context): Boolean {
        val items = com.centwise.data.repository.ReviewQueueRepository.shared.items.value
        if (items.isEmpty()) {
            android.widget.Toast.makeText(context, "Review queue is empty", android.widget.Toast.LENGTH_SHORT).show()
            return false
        }
        val uri = withContext(Dispatchers.IO) {
            val file = writeReviewQueueCsvFile(context, items) ?: return@withContext null
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } ?: return false

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Centwise review queue export"))
        return true
    }

    private fun escape(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
