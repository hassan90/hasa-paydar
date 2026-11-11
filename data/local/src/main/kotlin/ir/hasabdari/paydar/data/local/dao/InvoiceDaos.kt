package ir.hasabdari.paydar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hasabdari.paydar.data.local.entity.InvoiceEntity
import ir.hasabdari.paydar.data.local.entity.InvoiceLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("""
        SELECT * FROM invoices
        WHERE store_id = :storeId AND status != 'VOIDED'
        ORDER BY issue_date DESC, created_at DESC
    """)
    fun observeAll(storeId: String): Flow<List<InvoiceEntity>>

    @Query("""
        SELECT * FROM invoices
        WHERE store_id = :storeId AND type = :type AND status != 'VOIDED'
        ORDER BY issue_date DESC, created_at DESC
    """)
    fun observeByType(storeId: String, type: String): Flow<List<InvoiceEntity>>

    @Query("""
        SELECT * FROM invoices
        WHERE store_id = :storeId AND type IN (:types) AND status != 'VOIDED'
        ORDER BY issue_date DESC, created_at DESC
    """)
    fun observeByTypes(storeId: String, types: List<String>): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoice_lines WHERE invoice_id = :invoiceId ORDER BY line_no")
    suspend fun getLines(invoiceId: String): List<InvoiceLineEntity>

    @Query("""
        SELECT COUNT(*) FROM invoices
        WHERE store_id = :storeId AND fiscal_year_id = :fiscalYearId AND type = :type
    """)
    suspend fun countByType(storeId: String, fiscalYearId: String, type: String): Int

    @Query("""
        SELECT COALESCE(SUM(grand_total - paid_amount), 0) FROM invoices
        WHERE party_id = :partyId AND store_id = :storeId
          AND type = 'SALE' AND status = 'CONFIRMED'
    """)
    suspend fun getPartyOutstandingBalance(partyId: String, storeId: String): Long

    /** جمع مقدار برگشتی یک محصول از این فاکتور (همه‌ی برگشتی‌های قبلی) */
    @Query("""
        SELECT COALESCE(SUM(il.quantity), 0)
        FROM invoice_lines il
        INNER JOIN invoices i ON i.id = il.invoice_id
        WHERE i.parent_id = :originalInvoiceId
          AND il.product_id = :productId
          AND i.status != 'VOIDED'
    """)
    suspend fun getReturnedQuantity(originalInvoiceId: String, productId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(entity: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLines(lines: List<InvoiceLineEntity>)

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}