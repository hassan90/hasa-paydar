package ir.hasabdari.paydar.domain.model

data class NewSaleCommand(
    val issueDate: Long,
    val partyId: String?,                // null = مشتری متفرقه
    val warehouseId: String,
    val settlementType: SettlementType,
    val paidAmountRials: Long,
    val discountTotalRials: Long,
    val taxRate: Int,                    // نرخ VAT برای همه اقلام (مثلاً 1000=۱۰٪)
    val notes: String?,
    val lines: List<NewSaleLine>,
    val storeId: String
)

data class NewSaleLine(
    val productId: String,
    val productName: String,
    val unitId: String,
    val quantityScaled: Long,
    val unitPriceRials: Long,
    val discountRials: Long,
    val costAtSaleRials: Long = 0L     // COGS برای هر واحد — قبل از ذخیره محاسبه می‌شود
)

data class SaleResult(
    val invoiceId: String,
    val invoiceNumber: String,
    val grandTotalRials: Long,
    val cogsTotalRials: Long,
    val profitRials: Long
)