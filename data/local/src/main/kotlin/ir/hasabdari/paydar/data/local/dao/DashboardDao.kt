package ir.hasabdari.paydar.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

@Dao
interface DashboardDao {

    // ═══════════════════════════════════════════════════════════
    // فروش امروز / سود / تعداد فاکتور
    // ═══════════════════════════════════════════════════════════

    @Query("""
        SELECT COALESCE(SUM(grand_total), 0) FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND issue_date BETWEEN :fromMillis AND :toMillis
    """)
    suspend fun getSalesTotal(storeId: String, fromMillis: Long, toMillis: Long): Long

    @Query("""
        SELECT COUNT(*) FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND issue_date BETWEEN :fromMillis AND :toMillis
    """)
    suspend fun getInvoiceCount(storeId: String, fromMillis: Long, toMillis: Long): Int

    /** جمع خالص ردیف‌ها (بعد از تخفیف ردیف) */
    @Query("""
        SELECT COALESCE(SUM(il.line_total), 0) FROM invoice_lines il
        INNER JOIN invoices i ON i.id = il.invoice_id
        WHERE i.store_id = :storeId AND i.type = 'SALE' AND i.status = 'CONFIRMED'
          AND i.issue_date BETWEEN :fromMillis AND :toMillis
    """)
    suspend fun getLinesNetTotal(storeId: String, fromMillis: Long, toMillis: Long): Long

    /** تخفیف سطح فاکتور */
    @Query("""
        SELECT COALESCE(SUM(discount_total), 0) FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND issue_date BETWEEN :fromMillis AND :toMillis
    """)
    suspend fun getInvoiceDiscountTotal(storeId: String, fromMillis: Long, toMillis: Long): Long

    /** COGS ثبت‌شده روی هر ردیف فاکتور */
    @Query("""
        SELECT COALESCE(SUM(COALESCE(il.cost_at_sale, 0)), 0) FROM invoice_lines il
        INNER JOIN invoices i ON i.id = il.invoice_id
        WHERE i.store_id = :storeId AND i.type = 'SALE' AND i.status = 'CONFIRMED'
          AND i.issue_date BETWEEN :fromMillis AND :toMillis
    """)
    suspend fun getCogsTotal(storeId: String, fromMillis: Long, toMillis: Long): Long

    // ═══════════════════════════════════════════════════════════
    // بدهی مشتریان و تأمین‌کنندگان (از فاکتورهای پرداخت‌نشده)
    // ═══════════════════════════════════════════════════════════

    @Query("""
        SELECT COALESCE(SUM(grand_total - paid_amount), 0) FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND grand_total > paid_amount
    """)
    suspend fun getCustomerDebt(storeId: String): Long

    @Query("""
        SELECT COUNT(DISTINCT party_id) FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND grand_total > paid_amount AND party_id IS NOT NULL
    """)
    suspend fun getDebtorCustomerCount(storeId: String): Int

    @Query("""
        SELECT COALESCE(SUM(grand_total - paid_amount), 0) FROM invoices
        WHERE store_id = :storeId AND type = 'PURCHASE' AND status = 'CONFIRMED'
          AND grand_total > paid_amount
    """)
    suspend fun getSupplierDebt(storeId: String): Long

    @Query("""
        SELECT COUNT(DISTINCT party_id) FROM invoices
        WHERE store_id = :storeId AND type = 'PURCHASE' AND status = 'CONFIRMED'
          AND grand_total > paid_amount AND party_id IS NOT NULL
    """)
    suspend fun getCreditorSupplierCount(storeId: String): Int

    // ═══════════════════════════════════════════════════════════
    // نمودار ۷ روزه
    // ═══════════════════════════════════════════════════════════

    @Query("""
        SELECT issue_date, grand_total FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND issue_date BETWEEN :fromMillis AND :toMillis
        ORDER BY issue_date ASC
    """)
    suspend fun getDailySalesRaw(
        storeId: String, fromMillis: Long, toMillis: Long
    ): List<DailySaleRaw>

    // ═══════════════════════════════════════════════════════════
    // تراکنش‌های اخیر (فاکتورها + تراکنش‌های نقدی)
    // ═══════════════════════════════════════════════════════════

    @Query("""
        SELECT id, issue_date, type, number, grand_total, party_id FROM invoices
        WHERE store_id = :storeId AND status = 'CONFIRMED'
        ORDER BY issue_date DESC, created_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentInvoices(storeId: String, limit: Int): List<RecentInvoiceRaw>

    @Query("""
        SELECT id, occurred_at, type, amount, party_id, description FROM cash_transactions
        WHERE store_id = :storeId
        ORDER BY occurred_at DESC
        LIMIT :limit
    """)
    suspend fun getRecentCashTxns(storeId: String, limit: Int): List<RecentCashTxnRaw>

    @Query("SELECT id, name FROM parties WHERE store_id = :storeId AND deleted_at IS NULL")
    suspend fun getAllPartyNames(storeId: String): List<PartyNameRaw>
}

// ═══════════════════════════════════════════════════════════
// Raw DTOs
// ═══════════════════════════════════════════════════════════

data class DailySaleRaw(
    @ColumnInfo(name = "issue_date") val issueDate: Long,
    @ColumnInfo(name = "grand_total") val grandTotal: Long
)

data class RecentInvoiceRaw(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "issue_date") val issueDate: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "number") val number: String,
    @ColumnInfo(name = "grand_total") val grandTotal: Long,
    @ColumnInfo(name = "party_id") val partyId: String?
)

data class RecentCashTxnRaw(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "party_id") val partyId: String?,
    @ColumnInfo(name = "description") val description: String?
)

data class PartyNameRaw(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String
)