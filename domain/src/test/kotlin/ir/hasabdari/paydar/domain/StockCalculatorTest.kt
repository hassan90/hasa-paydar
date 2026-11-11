package ir.hasabdari.paydar.domain

import com.google.common.truth.Truth.assertThat
import ir.hasabdari.paydar.domain.stock.StockCalculator
import org.junit.Test

class StockCalculatorTest {

    @Test
    fun `first movement becomes the cost`() {
        val avg = StockCalculator.newAverageCost(0, 0, 10_000, 18_000)
        assertThat(avg).isEqualTo(18_000)
    }

    @Test
    fun `weighted average of two batches`() {
        // 10 کالا به بهای 18000 + 10 کالا به بهای 22000 = میانگین 20000
        val avg = StockCalculator.newAverageCost(
            currentQtyScaled = 10_000,
            currentAvgCostRials = 18_000,
            addQtyScaled = 10_000,
            addUnitCostRials = 22_000
        )
        assertThat(avg).isEqualTo(20_000)
    }

    @Test
    fun `weighted average with different quantities`() {
        // 20 عدد به 10000 + 5 عدد به 20000 = (200k + 100k) / 25 = 12000
        val avg = StockCalculator.newAverageCost(20_000, 10_000, 5_000, 20_000)
        assertThat(avg).isEqualTo(12_000)
    }

    @Test
    fun `zero addQty keeps cost`() {
        val avg = StockCalculator.newAverageCost(10_000, 15_000, 0, 99_000)
        assertThat(avg).isEqualTo(15_000)
    }

    @Test
    fun `cost of outgoing uses current avg`() {
        val cost = StockCalculator.costOfOutgoing(15_000, 2_000)
        assertThat(cost).isEqualTo(30_000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative current qty throws`() {
        StockCalculator.newAverageCost(-100, 100, 100, 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative add qty throws`() {
        StockCalculator.newAverageCost(0, 100, -100, 100)
    }
}