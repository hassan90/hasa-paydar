package ir.hasabdari.paydar.core.error

/**
 * همه خطاهای دامنه — پیام‌ها به فارسی برای نمایش مستقیم به کاربر.
 * هر خطا یک کد دارد تا برای لاگ و عیب‌یابی هم قابل استفاده باشد.
 */
sealed class DomainError(
    val code: String,
    val message: String,
    val cause: Throwable? = null
) {
    // ─── اعتبارسنجی ورودی ───

    class InvalidInput(field: String, reason: String) : DomainError(
        code = "INVALID_INPUT",
        message = "مقدار «$field» نامعتبر است: $reason"
    )

    class NegativeAmount(field: String) : DomainError(
        code = "NEGATIVE_AMOUNT",
        message = "مبلغ «$field» نمی‌تواند منفی باشد"
    )

    class EmptyField(field: String) : DomainError(
        code = "EMPTY_FIELD",
        message = "فیلد «$field» نمی‌تواند خالی باشد"
    )

    // ─── عملیات غیرمجاز ───

    class InsufficientStock(
        val productName: String,
        val requested: Long,
        val available: Long
    ) : DomainError(
        code = "INSUFFICIENT_STOCK",
        message = "موجودی «$productName» کافی نیست. درخواستی: $requested، موجود: $available"
    )

    class CreditLimitExceeded(
        val partyName: String,
        val currentDebt: Long,
        val creditLimit: Long
    ) : DomainError(
        code = "CREDIT_LIMIT_EXCEEDED",
        message = "سقف اعتبار «$partyName» پر شده است. بدهی فعلی: $currentDebt، سقف: $creditLimit"
    )

    class UnbalancedJournal(val debit: Long, val credit: Long) : DomainError(
        code = "UNBALANCED_JOURNAL",
        message = "سند نامتوازن است. بدهکار: $debit، بستانکار: $credit"
    )

    class ClosedFiscalYear(val yearTitle: String) : DomainError(
        code = "CLOSED_FISCAL_YEAR",
        message = "سال مالی «$yearTitle» بسته شده و قابل تغییر نیست"
    )

    // ─── خطاهای پایگاه داده ───

    class NotFound(entity: String, id: String) : DomainError(
        code = "NOT_FOUND",
        message = "«$entity» با شناسه «$id» یافت نشد"
    )

    class DuplicateEntry(field: String, value: String) : DomainError(
        code = "DUPLICATE_ENTRY",
        message = "مقدار «$value» برای «$field» تکراری است"
    )

    // ─── خطاهای همگام‌سازی ───

    class NetworkError(cause: Throwable? = null) : DomainError(
        code = "NETWORK_ERROR",
        message = "اتصال اینترنت قطع است. تغییرات شما محلی ذخیره شد و بعداً ارسال می‌شود.",
        cause = cause
    )

    class SyncConflict(entityType: String) : DomainError(
        code = "SYNC_CONFLICT",
        message = "تعارض در همگام‌سازی «$entityType». نیاز به بررسی دستی دارد."
    )

    // ─── خطاهای عمومی ───

    class Unknown(cause: Throwable? = null) : DomainError(
        code = "UNKNOWN",
        message = "خطای غیرمنتظره‌ای رخ داد. لطفاً دوباره تلاش کنید.",
        cause = cause
    )
}