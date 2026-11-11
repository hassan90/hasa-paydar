package ir.hasabdari.paydar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hasabdari.paydar.data.local.entity.CashAccountEntity
import ir.hasabdari.paydar.data.local.entity.CashTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashAccountDao {
    @Query("""
        SELECT * FROM cash_accounts
        WHERE store_id = :storeId AND is_active = 1
        ORDER BY kind, name COLLATE NOCASE
    """)
    fun observeAll(storeId: String): Flow<List<CashAccountEntity>>

    @Query("SELECT * FROM cash_accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CashAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CashAccountEntity)

    @Update
    suspend fun update(entity: CashAccountEntity)

    @Query("UPDATE cash_accounts SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)
}

@Dao
interface CashTransactionDao {
    @Query("""
        SELECT * FROM cash_transactions
        WHERE store_id = :storeId
        ORDER BY occurred_at DESC
    """)
    fun observeAll(storeId: String): Flow<List<CashTransactionEntity>>

    @Query("""
        SELECT * FROM cash_transactions
        WHERE store_id = :storeId
          AND (cash_account_id = :accountId OR to_cash_account_id = :accountId)
        ORDER BY occurred_at DESC
    """)
    fun observeByAccount(storeId: String, accountId: String): Flow<List<CashTransactionEntity>>

    @Query("SELECT * FROM cash_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CashTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CashTransactionEntity)

    @Query("DELETE FROM cash_transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("""
        SELECT
            (SELECT COALESCE(opening_balance, 0) FROM cash_accounts WHERE id = :accountId)
          + COALESCE((SELECT SUM(amount) FROM cash_transactions 
                      WHERE cash_account_id = :accountId AND type = 'RECEIPT'), 0)
          - COALESCE((SELECT SUM(amount) FROM cash_transactions 
                      WHERE cash_account_id = :accountId AND type = 'PAYMENT'), 0)
          - COALESCE((SELECT SUM(amount) FROM cash_transactions 
                      WHERE cash_account_id = :accountId AND type = 'TRANSFER'), 0)
          + COALESCE((SELECT SUM(amount) FROM cash_transactions 
                      WHERE to_cash_account_id = :accountId AND type = 'TRANSFER'), 0)
    """)
    suspend fun getBalance(accountId: String): Long?
}