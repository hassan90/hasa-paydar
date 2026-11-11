package ir.hasabdari.paydar.domain.model

data class NewReturnCommand(
    val originalInvoiceId: String,
    val returnDate: Long,
    val reason: String?,
    val lines: List<NewReturnLine>,
    val storeId: String
)

data class NewReturnLine(
    val originalLineId: String,
    val productId: String,
    val productName: String,
    val unitId: String,
    val quantityScaled: Long,     // مقدار برگشتی
    val unitPriceRials: Long
)

data class ReturnResult(
    val returnInvoiceId: String,
    val returnInvoiceNumber: String,
    val totalRefundRials: Long
)