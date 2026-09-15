package com.centwise.core.design.formatters

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double): String = formatBDT(amount)

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    /**
     * Formats an amount into standard Bangladeshi Taka currency format (BDT):
     * Uses standard 3-digit comma grouping (e.g. 1,000, 20,000, 164,000, 8,829,960).
     * Displays whole Taka only — no poisha / decimals.
     */
    fun formatBDT(
        amount: Double,
        showSign: Boolean = false,
        compact: Boolean = false,
        useBengaliNumerals: Boolean = false,
        @Suppress("UNUSED_PARAMETER") forceDecimals: Boolean = false
    ): String {
        val absAmount = kotlin.math.abs(amount)
        val wholeNumber = absAmount.toLong()

        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,##0", symbols)
        val formattedNumber = formatter.format(wholeNumber)

        val finalNumber = if (useBengaliNumerals) {
            convertToBengaliDigits(formattedNumber)
        } else {
            formattedNumber
        }

        val prefix = if (amount < 0) {
            "-৳ "
        } else if (showSign && amount > 0) {
            "+৳ "
        } else {
            "৳ "
        }

        return "$prefix$finalNumber"
    }

    /**
     * Groups numbers in standard 3-digit comma format (e.g. 1,000, 164,000, 8,829,960).
     */
    fun formatSouthAsianNumber(wholeNumber: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        return DecimalFormat("#,##0", symbols).format(wholeNumber)
    }

    fun convertToBengaliDigits(input: String): String {
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
