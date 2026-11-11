package ir.hasabdari.paydar.core.transaction

/**
 * اجرای اتمیک بلوکی از عملیات — در Data Layer با Room.withTransaction پیاده می‌شود.
 * اگر block خطا پرتاب کند، همه تغییرات rollback می‌شوند.
 */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}