package ir.hasabdari.paydar.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * نمایش پولی در واحد پایه «ریال» — بدون اعشار.
 * همه محاسبات مالی در سیستم از این کلاس استفاده می‌کنند تا خطای float رخ ندهد.
 */
@JvmInline
value class Money private constructor(val rials: Long) : Comparable<Money> {

    // ─── عملیات حسابی ───

    operator fun plus(other: Money): Money = Money(rials + other.rials)

    operator fun minus(other: Money): Money = Money(rials - other.rials)

    operator fun unaryMinus(): Money = Money(-rials)

    /** ضرب در مقدار صحیح (تعداد کالا) */
    operator fun times(quantity: Long): Money {
        require(quantity >= 0) { "تعداد نمی‌تواند منفی باشد" }
        return Money(Math.multiplyExact(rials, quantity))
    }

    /** ضرب در ضریب اعشاری — فقط برای درصد و امثال آن */
    fun timesPercent(percent: BigDecimal): Money {
        require(percent.signum() >= 0) { "درصد نمی‌تواند منفی باشد" }
        val result = BigDecimal(rials)
            .multiply(percent)
            .divide(BigDecimal(100), 0, RoundingMode.HALF_UP)
        return Money(result.toLong())
    }

    /** تقسیم — برای محاسبه میانگین بهای تمام‌شده */
    fun divideBy(divisor: Long): Money {
        require(divisor > 0) { "مقسوم‌علیه باید بزرگ‌تر از صفر باشد" }
        return Money(BigDecimal(rials)
            .divide(BigDecimal(divisor), 0, RoundingMode.HALF_UP)
            .toLong())
    }

    // ─── تبدیل به تومان ───

    fun toToman(): Long = rials / 10

    fun toTomanRounded(): Long = Math.round(rials / 10.0)

    // ─── وضعیت ───

    val isZero: Boolean get() = rials == 0L
    val isPositive: Boolean get() = rials > 0
    val isNegative: Boolean get() = rials < 0
    val absolute: Money get() = Money(Math.abs(rials))

    // ─── مقایسه ───

    override fun compareTo(other: Money): Int = rials.compareTo(other.rials)

    override fun toString(): String = rials.toString()

    companion object {
        val ZERO: Money = Money(0L)

        fun fromRials(rials: Long): Money = Money(rials)

        fun fromToman(toman: Long): Money = Money(Math.multiplyExact(toman, 10L))

        fun fromToman(toman: BigDecimal): Money = Money(
            toman.multiply(BigDecimal(10)).setScale(0, RoundingMode.HALF_UP).toLong()
        )
    }
}