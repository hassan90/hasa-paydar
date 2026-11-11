package ir.hasabdari.paydar.ui.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val priceFormat = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))

private val persianDigits = mapOf(
    '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
    '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
)

fun Long.toPriceString(): String =
    priceFormat.format(this).map { persianDigits[it] ?: it }.joinToString("").replace(",", "٬")

fun Long.toTomanString(): String = (this / 10).toPriceString()

fun Long.scaledToQuantityString(): String {
    val whole = this / 1000
    val frac = (this % 1000).toInt()
    return if (frac == 0) whole.toString().map { persianDigits[it] ?: it }.joinToString("")
    else {
        val s = String.format(Locale.US, "%d.%03d", whole, frac).trimEnd('0').trimEnd('.')
        s.map { persianDigits[it] ?: it }.joinToString("")
    }
}