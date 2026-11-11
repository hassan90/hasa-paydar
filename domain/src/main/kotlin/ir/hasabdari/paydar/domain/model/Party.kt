package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

enum class PartyKind { CUSTOMER, SUPPLIER, BOTH }

data class Party(
    val id: String,
    val kind: PartyKind,
    val code: String? = null,
    val name: String,
    val phone: String? = null,
    val mobile: String? = null,
    val nationalId: String? = null,
    val economicCode: String? = null,
    val address: String? = null,
    val province: String? = null,
    val city: String? = null,
    val creditLimit: Money = Money.ZERO,
    val discountPercent: Int = 0,
    val groupName: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null,
    val storeId: String
) {
    init {
        require(name.isNotBlank()) { "نام طرف حساب نمی‌تواند خالی باشد" }
        require(discountPercent in 0..100) { "درصد تخفیف باید بین ۰ تا ۱۰۰ باشد" }
    }

    val isCustomer: Boolean get() = kind == PartyKind.CUSTOMER || kind == PartyKind.BOTH
    val isSupplier: Boolean get() = kind == PartyKind.SUPPLIER || kind == PartyKind.BOTH
}