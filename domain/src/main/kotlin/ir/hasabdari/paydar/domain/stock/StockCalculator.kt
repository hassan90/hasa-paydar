package ir.hasabdari.paydar.domain.stock

/**
 * محاسبه میانگین موزون متحرک (Weighted Moving Average).
 * تمام ورودی‌ها در واحد پایه × ۱۰۰۰ (Scale).
 * خروجی: بهای میانگین جدید به ریال.
 *
 * فرمول:
 *   newAvg = (oldQty × oldAvg + addQty × addCost) / (oldQty + addQty)
 * اگر oldQty = 0 → newAvg = addCost
 */
object StockCalculator {

    fun newAverageCost(
        currentQtyScaled: Long,
        currentAvgCostRials: Long,
        addQtyScaled: Long,
        addUnitCostRials: Long
    ): Long {
        require(currentQtyScaled >= 0) { "موجودی فعلی نمی‌تواند منفی باشد" }
        require(addQtyScaled >= 0) { "مقدار اضافه‌شده نمی‌تواند منفی باشد" }
        require(currentAvgCostRials >= 0) { "بهای میانگین نمی‌تواند منفی باشد" }
        require(addUnitCostRials >= 0) { "بهای واحد نمی‌تواند منفی باشد" }

        if (addQtyScaled == 0L) return currentAvgCostRials
        if (currentQtyScaled == 0L) return addUnitCostRials

        val newQty = currentQtyScaled + addQtyScaled
        // (oldQty × oldAvg + addQty × addCost) / newQty
        // برای جلوگیری از overflow: ضرب در 1000 بعد تقسیم
        val oldValue = currentQtyScaled * currentAvgCostRials
        val addValue = addQtyScaled * addUnitCostRials
        return (oldValue + addValue) / newQty
    }

    /**
     * محاسبه بهای تمام‌شده برای مقدار خارج‌شده از انبار.
     * در میانگین موزون، بهای خروج = بهای میانگین فعلی.
     */
    fun costOfOutgoing(currentAvgCostRials: Long, outQtyScaled: Long): Long {
        require(outQtyScaled >= 0) { "مقدار خروجی نمی‌تواند منفی باشد" }
        return (currentAvgCostRials * outQtyScaled) / 1000
    }
}