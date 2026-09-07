package com.centwise

import com.centwise.core.design.formatters.CurrencyFormatter
import com.centwise.data.models.TransactionItem
import com.centwise.data.models.TransactionType
import com.centwise.features.transactions.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvAndFormatterTest {

    @Test
    fun testCurrencyFormatterLakhCroreGrouping() {
        assertEquals("৳ 500", CurrencyFormatter.formatBDT(500.0))
        assertEquals("৳ 1,000", CurrencyFormatter.formatBDT(1000.0))
        assertEquals("৳ 20,000", CurrencyFormatter.formatBDT(20000.0))
        assertEquals("৳ 1,64,000", CurrencyFormatter.formatBDT(164000.0))
        assertEquals("৳ 16,00,000", CurrencyFormatter.formatBDT(1600000.0))
        assertEquals("৳ 1,60,00,000", CurrencyFormatter.formatBDT(16000000.0))
        assertEquals("৳ 2,75,53,508", CurrencyFormatter.formatBDT(27553508.0))
    }

    @Test
    fun testCsvExporterIncludesRawSmsAndReference() {
        val sampleTransactions = listOf(
            TransactionItem(
                id = "tx-1",
                title = "Salary Deposit",
                amount = 164000.0,
                type = TransactionType.INCOME,
                category = "Income",
                paymentMethod = "Dutch-Bangla Bank",
                timestamp = 1661065666000L,
                note = "EFT by BIPOULHOSSAIN",
                rawSms = "Dear Sir, your A/C ***5543 credited(EFT by: BIPOULHOSSAIN adn: FRtk160,00000,gov) by Tk1,64,000.00 on 21-08-2022 01:07:46 PM C/B Tk1,64,528.94. NexusPay https",
                reference = "FRtk160,00000"
            )
        )

        val csv = CsvExporter.transactionsCsv(sampleTransactions)
        val lines = csv.split("\n")

        // Header check
        assertEquals("Date,Title,Amount,Type,Category,Payment Method,Reference,Note,Raw SMS", lines[0])

        // Data row check
        assertTrue(lines[1].contains("164000.00"))
        assertTrue(lines[1].contains("Salary Deposit"))
        assertTrue(lines[1].contains("Dutch-Bangla Bank"))
        assertTrue(lines[1].contains("BIPOULHOSSAIN"))
        assertTrue(lines[1].contains("NexusPay"))
    }
}
