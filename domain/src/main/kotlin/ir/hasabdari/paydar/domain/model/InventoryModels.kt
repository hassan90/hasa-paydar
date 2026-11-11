package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

/**
 * انبار — هر فروشگاه می‌تواند چند انبار داشته باشد.
 */
data class Warehouse(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val storeId: String
) {
    init {
        require(name.isNotBlank()) { "نام انبار نمی‌تواند خالی باشد" }
    }
}

/**
 * موجودی یک کالا در یک انبار — با میانگین بهای تمام‌شده.
 * quantity در واحد پایه × ۱۰۰۰ ذخیره می‌شود.
 */
data class StockItem(
    val productId: String,
    val warehouseId: String,
    val quantityScaled: Long,
    val avgCost: Money,
    val lastUpdated: Long
) {
    val quantity: Double get() = quantityScaled / 1000.0
    val totalValue: Money get() = Money.fromRials(
        (quantityScaled * avgCost.rials) / 1000
    )
}

/**
 * نوع حرکت انبار.
 */
enum class StockMovementType {
    OPENING,        // موجودی اولیه
    PURCHASE_IN,    // ورود از خرید
    SALE_OUT,       // خروج از فروش
    RETURN_IN,      // برگشت از فروش
    RETURN_OUT,     // برگشت به تأمین‌کننده
    TRANSFER_IN,    // ورود از انتقال
    TRANSFER_OUT,   // خروج از انتقال
    ADJUST_IN,      // اضافی انبارگردانی
    ADJUST_OUT      // کسری/ضایعات
}

/**
 * یک حرکت انبار — فقط INSERT، هرگز تغییر یا حذف نمی‌شود (ADR-03).
 */
data class StockMovement(
    val id: String,
    val productId: String,
    val warehouseId: String,
    val type: StockMovementType,
    val quantityDeltaScaled: Long,   // مثبت=ورود، منفی=خروج
    val unitCost: Money,             // بهای واحد در لحظه حرکت
    val referenceType: String? = null,
    val referenceId: String? = null,
    val note: String? = null,
    val occurredAt: Long,
    val storeId: String
) {
    val isIn: Boolean get() = quantityDeltaScaled > 0
}

/**
 * View model برای نمایش موجودی هر انبار در صفحه جزئیات کالا.
 */
data class WarehouseStock(
    val warehouseId: String,
    val warehouseName: String,
    val quantityScaled: Long,
    val avgCost: Money
) {
    val quantity: Double get() = quantityScaled / 1000.0
}

/**
 * کالا + موجودی کل (برای لیست).
 */
data class ProductWithStock(
    val product: Product,
    val totalStockScaled: Long,
    val totalValueRials: Long = 0
) {
    val totalStock: Double get() = totalStockScaled / 1000.0
    val isLowStock: Boolean get() = totalStockScaled <= product.minStockScaled
}