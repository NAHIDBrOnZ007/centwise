package com.centwise.core.design.formatters

object CurrencyFormatter {
    fun format(amount: Double): String = formatBDT(amount)

    private val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    /**
     * Formats an amount into standard Bangladeshi Taka format:
     * Always uses clean traditional Bangladeshi Lakh & Crore grouping:
     * - 1,000 = 1 Hajar (Thousand)
     * - 20,000 = 20 Hajar (Thousand)
     * - 1,64,000 = 1 Lakh 64 Thousand (3 digits at end, 2 digits before that)
     * - 16,00,000 = 16 Lakh
     * - 1,60,00,000 = 1 Crore 60 Lakh
     */
    fun formatBDT(
        amount: Double,
        showSign: Boolean = false,
        compact: Boolean = false,
        useBengaliNumerals: Boolean = false
    ): String {
        val absAmount = kotlin.math.abs(amount)
        val formattedNumber = formatSouthAsianNumber(absAmount.toLong())

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

    /**
     * Groups numbers in the traditional Bangladeshi / South Asian Lakh & Crore system:
     * e.g.:
     * 1000 -> 1,000
     * 20000 -> 20,000
     * 164000 -> 1,64,000 (1 Lakh 64 Thousand)
     * 1600000 -> 16,00,000 (16 Lakh)
     * 16000000 -> 1,60,00,000 (1 Crore 60 Lakh)
     */
    fun formatSouthAsianNumber(wholeNumber: Long): String {
        val s = wholeNumber.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val remaining = s.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return sb.reverse().toString() + "," + last3
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
