package ir.hasabdari.paydar.domain.model

// ═══════════════════════════════════════════════════════════
// نمودار حساب‌ها
// ═══════════════════════════════════════════════════════════

enum class AccountCategory { ASSET, LIABILITY, EQUITY, REVENUE, COGS, EXPENSE }
enum class AccountNature { DEBIT, CREDIT }

data class Account(
    val code: String,
    val name: String,
    val parentCode: String?,
    val level: Int,
    val nature: AccountNature,
    val category: AccountCategory,
    val isPostable: Boolean,
    val isActive: Boolean
) {
    val isGroupLevel: Boolean get() = level <= 2
}

// ═══════════════════════════════════════════════════════════
// سال مالی
// ═══════════════════════════════════════════════════════════

data class FiscalYear(
    val id: String,
    val title: String,
    val startDate: Long,
    val endDate: Long,
    val isClosed: Boolean,
    val storeId: String
)

// ═══════════════════════════════════════════════════════════
// اسناد حسابداری
// ═══════════════════════════════════════════════════════════

enum class JournalSourceType {
    INVOICE_SALE,
    INVOICE_PURCHASE,
    CASH_RECEIPT,
    CASH_PAYMENT,
    CASH_TRANSFER,
    CHEQUE_RECEIVED,      // ← جدید (F9)
    CHEQUE_ISSUED,        // ← جدید (F9)
    STOCK_ADJUSTMENT,
    OPENING,
    CLOSING,
    MANUAL,
    REVERSAL
}

enum class JournalStatus { POSTED, VOIDED }

data class JournalEntry(
    val id: String,
    val number: Long,
    val fiscalYearId: String,
    val entryDate: Long,
    val description: String,
    val sourceType: JournalSourceType,
    val sourceId: String?,
    val status: JournalStatus,
    val reversalOf: String?,
    val createdBy: String,
    val createdAt: Long,
    val storeId: String
)

data class JournalLine(
    val id: String,
    val entryId: String,
    val lineNo: Int,
    val accountCode: String,
    val partyId: String?,
    val debit: Long,
    val credit: Long,
    val description: String?
)

/** پیش‌نویس سند — قبل از ثبت */
data class JournalDraft(
    val entryDate: Long,
    val description: String,
    val sourceType: JournalSourceType,
    val sourceId: String?,
    val lines: List<JournalLineDraft>,
    val createdBy: String = "system",
    val storeId: String
) {
    init {
        require(lines.isNotEmpty()) { "سند باید حداقل یک ردیف داشته باشد" }
        val dSum = lines.sumOf { it.debit }
        val cSum = lines.sumOf { it.credit }
        require(dSum == cSum) { "سند نامتوازن: بدهکار=$dSum بستانکار=$cSum" }
        require(lines.all { (it.debit > 0 && it.credit == 0L) || (it.credit > 0 && it.debit == 0L) }) {
            "هر ردیف باید فقط بدهکار یا فقط بستانکار باشد"
        }
        require(lines.all { it.accountCode.isNotBlank() }) { "کد حساب نمی‌تواند خالی باشد" }
    }
}

data class JournalLineDraft(
    val accountCode: String,
    val partyId: String? = null,
    val debit: Long = 0L,
    val credit: Long = 0L,
    val description: String? = null
)

/** سند با ردیف‌ها — برای نمایش */
data class JournalEntryWithLines(
    val entry: JournalEntry,
    val lines: List<JournalLine>
) {
    val totalDebit: Long get() = lines.sumOf { it.debit }
    val totalCredit: Long get() = lines.sumOf { it.credit }
}

// ═══════════════════════════════════════════════════════════
// تراز آزمایشی
// ═══════════════════════════════════════════════════════════

data class TrialBalanceRow(
    val accountCode: String,
    val accountName: String,
    val debit: Long,
    val credit: Long
)

data class TrialBalance(
    val fromDate: Long,
    val toDate: Long,
    val rows: List<TrialBalanceRow>,
    val totalDebit: Long,
    val totalCredit: Long,
    val isBalanced: Boolean
)