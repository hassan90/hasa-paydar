package ir.hasabdari.paydar.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.hasabdari.paydar.data.local.entity.AccountEntity
import ir.hasabdari.paydar.data.local.entity.FiscalYearEntity
import ir.hasabdari.paydar.data.local.entity.JournalEntryEntity
import ir.hasabdari.paydar.data.local.entity.JournalLineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY code")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_postable = 1 AND is_active = 1 ORDER BY code")
    fun observePostable(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): AccountEntity?

    @Query("SELECT code FROM accounts")
    suspend fun getAllCodes(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(accounts: List<AccountEntity>)
}

@Dao
interface FiscalYearDao {
    @Query("SELECT * FROM fiscal_years WHERE store_id = :storeId ORDER BY start_date DESC")
    fun observeAll(storeId: String): Flow<List<FiscalYearEntity>>

    @Query("""
        SELECT * FROM fiscal_years
        WHERE store_id = :storeId AND is_closed = 0
        ORDER BY start_date DESC
        LIMIT 1
    """)
    suspend fun getCurrent(storeId: String): FiscalYearEntity?

    @Query("SELECT * FROM fiscal_years WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FiscalYearEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: FiscalYearEntity)
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries WHERE store_id = :storeId ORDER BY entry_date DESC, number DESC")
    fun observeAll(storeId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): JournalEntryEntity?

    @Query("""
        SELECT * FROM journal_entries
        WHERE source_type = :sourceType AND source_id = :sourceId AND store_id = :storeId
        LIMIT 1
    """)
    suspend fun findBySource(sourceType: String, sourceId: String, storeId: String): JournalEntryEntity?

    @Query("SELECT COALESCE(MAX(number), 0) FROM journal_entries WHERE fiscal_year_id = :fyId")
    suspend fun maxNumber(fyId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: JournalEntryEntity)
}

@Dao
interface JournalLineDao {
    @Query("SELECT * FROM journal_lines WHERE entry_id = :entryId ORDER BY line_no")
    suspend fun getByEntry(entryId: String): List<JournalLineEntity>

    @Query("SELECT * FROM journal_lines WHERE entry_id = :entryId ORDER BY line_no")
    fun observeByEntry(entryId: String): Flow<List<JournalLineEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(lines: List<JournalLineEntity>)

    /** تراز آزمایشی: جمع بدهکار/بستانکار هر حساب */
    @Query("""
        SELECT jl.account_code as account_code,
               a.name as account_name,
               SUM(jl.debit) as total_debit,
               SUM(jl.credit) as total_credit
        FROM journal_lines jl
        INNER JOIN journal_entries je ON je.id = jl.entry_id
        INNER JOIN accounts a ON a.code = jl.account_code
        WHERE je.store_id = :storeId
          AND je.status = 'POSTED'
          AND je.entry_date >= :fromDate
          AND je.entry_date <= :toDate
        GROUP BY jl.account_code
        ORDER BY jl.account_code
    """)
    suspend fun trialBalanceRaw(storeId: String, fromDate: Long, toDate: Long): List<TrialBalanceRaw>
}

data class TrialBalanceRaw(
    @ColumnInfo(name = "account_code") val accountCode: String,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "total_debit") val totalDebit: Long,
    @ColumnInfo(name = "total_credit") val totalCredit: Long
)