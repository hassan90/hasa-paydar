package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

/**
 * موجودیت کالا در لایه Domain — هیچ وابستگی به Room/Android ندارد.
 * تمام اعداد مقداری (quantity) با ضریب ۱۰۰۰ ذخیره می‌شوند تا کالاهای وزنی پشتیبانی شوند.
 */
data class Product(
    val id: String,
    val code: String,
    val barcode: String?,
    val name: String,
    val groupId: String?,
    val unitId: String,
    val purchasePrice: Money,
    val salePrice: Money,
    val minStockScaled: Long,     // × 1000
    val maxStockScaled: Long?,    // × 1000
    val isService: Boolean = false,
    val isActive: Boolean = true,
    val trackSerial: Boolean = false,
    val trackExpiry: Boolean = false,
    val imageUri: String? = null,
    val notes: String? = null,
    val storeId: String
) {
    init {
        require(code.isNotBlank()) { "کد کالا نمی‌تواند خالی باشد" }
        require(name.isNotBlank()) { "نام کالا نمی‌تواند خالی باشد" }
        require(!salePrice.isNegative) { "قیمت فروش نمی‌تواند منفی باشد" }
        require(!purchasePrice.isNegative) { "قیمت خرید نمی‌تواند منفی باشد" }
        require(minStockScaled >= 0) { "حداقل موجودی نمی‌تواند منفی باشد" }
    }

    val profitMargin: Money get() = salePrice - purchasePrice
    val profitPercent: Int
        get() {
            if (purchasePrice.isZero) return 0
            return ((profitMargin.rials * 100) / purchasePrice.rials).toInt()
        }
}