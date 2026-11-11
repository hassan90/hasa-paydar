package ir.hasabdari.paydar.core

import com.google.common.truth.Truth.assertThat
import ir.hasabdari.paydar.core.money.Money
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `zero has no value`() {
        assertThat(Money.ZERO.rials).isEqualTo(0L)
        assertThat(Money.ZERO.isZero).isTrue()
    }

    @Test
    fun `fromToman converts correctly to rials`() {
        val m = Money.fromToman(25_000)
        assertThat(m.rials).isEqualTo(250_000L)
    }

    @Test
    fun `addition works`() {
        val a = Money.fromRials(1_500_000)
        val b = Money.fromRials(2_300_000)
        assertThat((a + b).rials).isEqualTo(3_800_000L)
    }

    @Test
    fun `subtraction can go negative`() {
        val a = Money.fromRials(100)
        val b = Money.fromRials(300)
        assertThat((a - b).rials).isEqualTo(-200L)
    }

    @Test
    fun `multiplication by quantity`() {
        val unit = Money.fromToman(25_000)
        val total = unit * 3
        assertThat(total.rials).isEqualTo(750_000L)
    }

    @Test
    fun `percent calculation rounds correctly`() {
        val base = Money.fromRials(1_000_000)
        val tax = base.timesPercent(BigDecimal("10"))
        assertThat(tax.rials).isEqualTo(100_000L)
    }

    @Test
    fun `percent on non-round amount rounds half up`() {
        val base = Money.fromRials(1_234_567)
        val tax = base.timesPercent(BigDecimal("10"))
        // 123456.7 → rounds to 123457
        assertThat(tax.rials).isEqualTo(123_457L)
    }

    @Test
    fun `average cost calculation`() {
        val total = Money.fromRials(900_000)
        val avg = total.divideBy(7)
        // 900000 / 7 = 128571.42 → 128571
        assertThat(avg.rials).isEqualTo(128_571L)
    }

    @Test
    fun `comparison works`() {
        val a = Money.fromRials(100)
        val b = Money.fromRials(200)
        assertThat(a < b).isTrue()
        assertThat(a > b).isFalse()
        assertThat(a == Money.fromRials(100)).isTrue()
    }

    @Test
    fun `absolute value`() {
        val negative = Money.fromRials(-500)
        assertThat(negative.absolute.rials).isEqualTo(500L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `multiplying by negative quantity throws`() {
        Money.fromRials(100) * -1
    }

    @Test(expected = IllegalArgumentException::class)
    fun `division by zero throws`() {
        Money.fromRials(100).divideBy(0)
    }

    @Test
    fun `toToman conversion truncates`() {
        assertThat(Money.fromRials(155).toToman()).isEqualTo(15L)
        assertThat(Money.fromRials(159).toToman()).isEqualTo(15L)
    }

    @Test
    fun `large multiplication handles overflow safely`() {
        // Math.multiplyExact باید overflow را پرتاب کند
        try {
            Money.fromRials(Long.MAX_VALUE) * 2
            throw AssertionError("Expected overflow")
        } catch (e: ArithmeticException) {
            assertThat(e).isNotNull()
        }
    }
}