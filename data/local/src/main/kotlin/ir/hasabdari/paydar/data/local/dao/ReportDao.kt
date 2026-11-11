package ir.hasabdari.paydar.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query

// ═══ Result types ═══

data class AccountBalanceRaw(
    @ColumnInfo(name = "account_code") val accountCode: String,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "total_debit") val totalDebit: Long,
    @ColumnInfo(name = "total_credit") val totalCredit: Long
)

data class SaleInvoiceRaw(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "issue_date") val issueDate: Long,
    @ColumnInfo(name = "grand_total") val grandTotal: Long,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long
)

data class SalesByProductRaw(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "total_quantity") val totalQuantity: Long,
    @ColumnInfo(name = "total_revenue") val totalRevenue: Long,
    @ColumnInfo(name = "total_cost") val totalCost: Long
)

data class PartyBalanceRaw(
    @ColumnInfo(name = "party_id") val partyId: String,
    @ColumnInfo(name = "party_name") val partyName: String,
    @ColumnInfo(name = "mobile") val mobile: String?,
    @ColumnInfo(name = "invoice_count") val invoiceCount: Int,
    @ColumnInfo(name = "receivable") val receivable: Long,
    @ColumnInfo(name = "payable") val payable: Long
)

data class InventoryValueRaw(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "product_name") val productName: String,
    @ColumnInfo(name = "product_code") val productCode: String,
    @ColumnInfo(name = "min_stock") val minStock: Long,
    @ColumnInfo(name = "quantity") val quantity: Long,
    @ColumnInfo(name = "total_value") val totalValue: Long
)

data class PartyOpeningRaw(
    @ColumnInfo(name = "total_debit") val totalDebit: Long,
    @ColumnInfo(name = "total_credit") val totalCredit: Long
)

data class PartyTransactionRaw(
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "entry_date") val entryDate: Long,
    @ColumnInfo(name = "entry_number") val entryNumber: Long,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "total_debit") val totalDebit: Long,
    @ColumnInfo(name = "total_credit") val totalCredit: Long)

@Dao
interface ReportDao {

    // ═══ جمع بدهکار/بستانکار هر حساب در بازه ═══
    @Query("""
        SELECT jl.account_code as account_code,
               a.name as account_name,
               a.category as category,
               COALESCE(SUM(jl.debit), 0) as total_debit,
               COALESCE(SUM(jl.credit), 0) as total_credit
        FROM journal_lines jl
        INNER JOIN journal_entries je ON je.id = jl.entry_id
        INNER JOIN accounts a ON a.code = jl.account_code
        WHERE je.store_id = :storeId AND je.status = 'POSTED'
          AND je.entry_date BETWEEN :fromDate AND :toDate
        GROUP BY jl.account_code
        ORDER BY jl.account_code
    """)
    suspend fun getAccountBalances(storeId: String, fromDate: Long, toDate: Long): List<AccountBalanceRaw>

    // ═══ فاکتورهای فروش در بازه (برای گزارش تاریخ‌دار) ═══
    @Query("""
        SELECT id, issue_date, grand_total, paid_amount
        FROM invoices
        WHERE store_id = :storeId AND type = 'SALE' AND status = 'CONFIRMED'
          AND issue_date BETWEEN :fromDate AND :toDate
    """)
    suspend fun getSaleInvoices(storeId: String, fromDate: Long, toDate: Long): List<SaleInvoiceRaw>

    // ═══ فروش به تفکیک کالا ═══
    @Query("""
        SELECT il.product_id as product_id,
               p.name as product_name,
               COALESCE(SUM(il.quantity), 0) as total_quantity,
               COALESCE(SUM(il.line_total), 0) as total_revenue,
               COALESCE(SUM(COALESCE(il.cost_at_sale, 0)), 0) as total_cost
        FROM invoice_lines il
        INNER JOIN invoices i ON i.id = il.invoice_id
        INNER JOIN products p ON p.id = il.product_id
        WHERE i.store_id = :storeId AND i.type = 'SALE' AND i.status = 'CONFIRMED'
          AND i.issue_date BETWEEN :fromDate AND :toDate
        GROUP BY il.product_id
        ORDER BY total_revenue DESC
        LIMIT 20
    """)
    suspend fun getSalesByProduct(storeId: String, fromDate: Long, toDate: Long): List<SalesByProductRaw>

    // ═══ مانده مشتریان (طلب از مشتری) ═══
    @Query("""
        SELECT p.id as party_id,
               p.name as party_name,
               p.mobile as mobile,
               COUNT(i.id) as invoice_count,
               COALESCE(SUM(i.grand_total - i.paid_amount), 0) as receivable,
               0 as payable
        FROM parties p
        INNER JOIN invoices i ON i.party_id = p.id AND i.status = 'CONFIRMED' AND i.type = 'SALE'
        WHERE p.store_id = :storeId AND p.deleted_at IS NULL
          AND (p.kind = 'CUSTOMER' OR p.kind = 'BOTH')
        GROUP BY p.id
        HAVING receivable > 0
        ORDER BY receivable DESC
    """)
    suspend fun getCustomerBalances(storeId: String): List<PartyBalanceRaw>

    // ═══ مانده تأمین‌کنندگان (بدهی ما به آن‌ها) ═══
    @Query("""
        SELECT p.id as party_id,
               p.name as party_name,
               p.mobile as mobile,
               COUNT(i.id) as invoice_count,
               0 as receivable,
               COALESCE(SUM(i.grand_total - i.paid_amount), 0) as payable
        FROM parties p
        INNER JOIN invoices i ON i.party_id = p.id AND i.status = 'CONFIRMED' AND i.type = 'PURCHASE'
        WHERE p.store_id = :storeId AND p.deleted_at IS NULL
          AND (p.kind = 'SUPPLIER' OR p.kind = 'BOTH')
        GROUP BY p.id
        HAVING payable > 0
        ORDER BY payable DESC
    """)
    suspend fun getSupplierBalances(storeId: String): List<PartyBalanceRaw>

    // ═══ ارزش موجودی هر کالا ═══
    @Query("""
        SELECT p.id as product_id,
               p.name as product_name,
               p.code as product_code,
               p.min_stock as min_stock,
               COALESCE(SUM(s.quantity), 0) as quantity,
               COALESCE(SUM(s.quantity * s.avg_cost / 1000), 0) as total_value
        FROM products p
        LEFT JOIN stock s ON s.product_id = p.id
        WHERE p.store_id = :storeId AND p.deleted_at IS NULL
        GROUP BY p.id
        ORDER BY total_value DESC
    """)
    suspend fun getInventoryValues(storeId: String): List<InventoryValueRaw>

    @Query("""
    SELECT COALESCE(SUM(jl.debit), 0) as total_debit,
           COALESCE(SUM(jl.credit), 0) as total_credit
    FROM journal_lines jl
    INNER JOIN journal_entries je ON je.id = jl.entry_id
    WHERE jl.party_id = :partyId
      AND je.store_id = :storeId
      AND je.status = 'POSTED'
      AND je.entry_date < :fromDate
      AND (jl.account_code LIKE '110201%' OR jl.account_code LIKE '210101%')
""")
    suspend fun getPartyOpeningBalance(
        partyId: String, storeId: String, fromDate: Long
    ): PartyOpeningRaw

    /** تراکنش‌های دوره */
    @Query("""
    SELECT je.id as entry_id,
           je.entry_date as entry_date,
           je.number as entry_number,
           je.source_type as source_type,
           je.source_id as source_id,
           je.description as description,
           COALESCE(SUM(jl.debit), 0) as total_debit,
           COALESCE(SUM(jl.credit), 0) as total_credit
    FROM journal_lines jl
    INNER JOIN journal_entries je ON je.id = jl.entry_id
    WHERE jl.party_id = :partyId
      AND je.store_id = :storeId
      AND je.status = 'POSTED'
      AND je.entry_date BETWEEN :fromDate AND :toDate
      AND (jl.account_code LIKE '110201%' OR jl.account_code LIKE '210101%')
    GROUP BY je.id
    ORDER BY je.entry_date ASC, je.number ASC
""")
    suspend fun getPartyTransactions(
        partyId: String, storeId: String, fromDate: Long, toDate: Long
    ): List<PartyTransactionRaw>

    @Query("SELECT * FROM parties WHERE id = :partyId LIMIT 1")
    suspend fun getPartyById(partyId: String): ir.hasabdari.paydar.data.local.entity.PartyEntity?
}