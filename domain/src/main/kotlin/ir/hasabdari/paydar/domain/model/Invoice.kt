package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

enum class InvoiceType { PURCHASE, SALE, PURCHASE_RETURN, SALE_RETURN, PROFORMA }
enum class SettlementType { CASH, CREDIT, MIXED }
enum class InvoiceStatus { DRAFT, CONFIRMED, VOIDED }

data class Invoice(
    val id: String,
    val type: InvoiceType,
    val number: String,
    val issueDate: Long,
    val dueDate: Long?,
    val partyId: String?,
    val warehouseId: String,
    val settlementType: SettlementType,
    val paidAmount: Money,
    val subtotal: Money,
    val discountTotal: Money,
    val taxTotal: Money,
    val shippingCost: Money,
    val otherCosts: Money,
    val grandTotal: Money,
    val status: InvoiceStatus,
    val parentId: String?,
    val notes: String?,
    val createdBy: String,
    val createdAt: Long,
    val storeId: String,
    val fiscalYearId: String
) {
    val remaining: Money get() = grandTotal - paidAmount
}

data class InvoiceLine(
    val id: String,
    val invoiceId: String,
    val lineNo: Int,
    val productId: String,
    val description: String?,
    val unitId: String,
    val quantityScaled: Long,
    val unitPrice: Money,
    val discount: Money,
    val taxRate: Int,
    val lineTotal: Money,
    val costAtSale: Money?
)

data class InvoiceWithLines(
    val invoice: Invoice,
    val lines: List<InvoiceLine>
)

/** ورودی افزودن فاکتور خرید */
data class NewPurchaseCommand(
    val issueDate: Long,
    val partyId: String?,             // تأمین‌کننده (اختیاری اگر نقدی)
    val warehouseId: String,
    val settlementType: SettlementType,
    val paidAmountRials: Long,
    val discountTotalRials: Long,
    val shippingCostRials: Long,
    val otherCostsRials: Long,
    val notes: String?,
    val lines: List<NewPurchaseLine>,
    val storeId: String
)

data class NewPurchaseLine(
    val productId: String,
    val productName: String,          // فقط برای پیام‌های خطا
    val unitId: String,
    val quantityScaled: Long,
    val unitPriceRials: Long,
    val discountRials: Long,
    val taxRate: Int                  // مثلاً ۱۰۰۰ = ۱۰٪
)