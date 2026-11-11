package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

enum class ChequeKind { RECEIVED, ISSUED }

enum class ChequeStatus {
    IN_SAFE,      // دریافتی: نزد صندوق
    DEPOSITED,    // دریافتی: واگذار به بانک
    CLEARED,      // وصول‌شده (هر دو)
    BOUNCED,      // برگشتی (دریافتی)
    RETURNED,     // مسترد به مشتری (دریافتی، بعد از برگشت)
    SPENT,        // پرداختی: خرج‌شده
    CANCELLED     // ابطال
}

data class Cheque(
    val id: String,
    val kind: ChequeKind,
    val number: String,
    val amount: Money,
    val issueDate: Long,
    val dueDate: Long,
    val bankName: String?,
    val branch: String?,
    val accountHolder: String?,
    val accountNumber: String?,
    val partyId: String?,
    val status: ChequeStatus,
    val relatedInvoiceId: String?,
    val note: String?,
    val createdAt: Long,
    val storeId: String
) {
    init {
        require(number.isNotBlank()) { "شماره چک الزامی است" }
        require(!amount.isZero) { "مبلغ چک نمی‌تواند صفر باشد" }
        require(!amount.isNegative) { "مبلغ چک نمی‌تواند منفی باشد" }
        require(dueDate >= issueDate) { "تاریخ سررسید باید بعد از تاریخ صدور باشد" }
    }

    val isReceived: Boolean get() = kind == ChequeKind.RECEIVED
    val isIssued: Boolean get() = kind == ChequeKind.ISSUED
    val isOpen: Boolean get() = status in listOf(
        ChequeStatus.IN_SAFE, ChequeStatus.DEPOSITED,
        ChequeStatus.SPENT
    )
    val isFinal: Boolean get() = status in listOf(
        ChequeStatus.CLEARED, ChequeStatus.BOUNCED,
        ChequeStatus.RETURNED, ChequeStatus.CANCELLED
    )

    /** روزهای مانده تا سررسید */
    fun daysToDue(now: Long): Long = (dueDate - now) / 86_400_000L
}