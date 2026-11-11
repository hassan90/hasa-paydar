package ir.hasabdari.paydar.core.date

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * تاریخ شمسی سبک — برای نمایش.
 * ذخیره‌سازی در DB همیشه به‌صورت epoch millis انجام می‌شود؛ این کلاس فقط برای presentation.
 */
data class JalaliDate(val year: Int, val month: Int, val day: Int) {

    init {
        require(month in 1..12) { "ماه باید بین ۱ تا ۱۲ باشد" }
        require(day in 1..31) { "روز باید بین ۱ تا ۳۱ باشد" }
    }

    fun format(): String = "%04d/%02d/%02d".format(year, month, day)

    fun formatPersian(): String = toPersianDigits(format())

    fun formatLong(): String = "${toPersianDigits(day.toString())} ${MONTH_NAMES[month - 1]} ${toPersianDigits(year.toString())}"

    companion object {
        private val MONTH_NAMES = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )

        private val zone: ZoneId = ZoneId.of("Asia/Tehran")

        fun fromEpochMillis(millis: Long): JalaliDate {
            val localDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
            return fromGregorian(localDate)
        }

        fun fromGregorian(date: LocalDate): JalaliDate {
            val gy = date.year
            val gm = date.monthValue
            val gd = date.dayOfMonth

            val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            var gy2 = if (gm > 2) gy + 1 else gy
            var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
            var jy = -1595 + (33 * (days / 12053))
            days %= 12053
            jy += 4 * (days / 1461)
            days %= 1461
            if (days > 365) {
                jy += (days - 1) / 365
                days = (days - 1) % 365
            }
            val jm: Int
            val jd: Int
            if (days < 186) {
                jm = 1 + days / 31
                jd = 1 + days % 31
            } else {
                jm = 7 + (days - 186) / 30
                jd = 1 + (days - 186) % 30
            }
            return JalaliDate(jy, jm, jd)
        }

        fun today(): JalaliDate = fromEpochMillis(System.currentTimeMillis())

        fun toPersianDigits(input: String): String {
            val digits = mapOf(
                '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
                '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
            )
            return input.map { digits[it] ?: it }.joinToString("")
        }
    }
}