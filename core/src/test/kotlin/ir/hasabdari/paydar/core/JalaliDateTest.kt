package ir.hasabdari.paydar.core

import com.google.common.truth.Truth.assertThat
import ir.hasabdari.paydar.core.date.JalaliDate
import org.junit.Test
import java.time.LocalDate

class JalaliDateTest {

    @Test
    fun `convert gregorian 2024-11-07 to jalali 1403-08-17`() {
        val gregorian = LocalDate.of(2024, 11, 7)
        val jalali = JalaliDate.fromGregorian(gregorian)
        assertThat(jalali.year).isEqualTo(1403)
        assertThat(jalali.month).isEqualTo(8)
        assertThat(jalali.day).isEqualTo(17)
    }

    @Test
    fun `convert gregorian 2024-03-20 to jalali 1403-01-01`() {
        val gregorian = LocalDate.of(2024, 3, 20)
        val jalali = JalaliDate.fromGregorian(gregorian)
        assertThat(jalali.year).isEqualTo(1403)
        assertThat(jalali.month).isEqualTo(1)
        assertThat(jalali.day).isEqualTo(1)
    }

    @Test
    fun `format with persian digits`() {
        val date = JalaliDate(1403, 8, 17)
        assertThat(date.formatPersian()).isEqualTo("۱۴۰۳/۰۸/۱۷")
    }

    @Test
    fun `format long with month name`() {
        val date = JalaliDate(1403, 8, 17)
        assertThat(date.formatLong()).isEqualTo("۱۷ آبان ۱۴۰۳")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid month throws`() {
        JalaliDate(1403, 13, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid day throws`() {
        JalaliDate(1403, 1, 32)
    }
}