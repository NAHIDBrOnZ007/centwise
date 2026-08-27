package com.centwise.core.design.formatters

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double): String = formatBDT(amount)

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun formatBDT(
        amount: Double,
        showSign: Boolean = false,
        compact: Boolean = false,
        useBengaliNumerals: Boolean = false
    ): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val pattern = if (amount % 1.0 == 0.0) "#,##0" else "#,##0.00"
        val df = DecimalFormat(pattern, symbols)
        val formattedNumber = df.format(kotlin.math.abs(amount))

        val finalNumber = if (useBengaliNumerals) {
            convertToBengaliDigits(formattedNumber)
        } else {
            formattedNumber
        }

        val prefix = if (showSign) {
            if (amount > 0) "+ " else if (amount < 0) "- " else ""
        } else ""

        return "${prefix}৳ $finalNumber"
    }

    private fun convertToBengaliDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}
