package ir.hasabdari.paydar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hasabdari.paydar.data.local.entity.ChequeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChequeDao {

    @Query("""
        SELECT * FROM cheques
        WHERE store_id = :storeId
        ORDER BY due_date ASC, created_at DESC
    """)
    fun observeAll(storeId: String): Flow<List<ChequeEntity>>

    @Query("""
        SELECT * FROM cheques
        WHERE store_id = :storeId AND kind = :kind
        ORDER BY due_date ASC, created_at DESC
    """)
    fun observeByKind(storeId: String, kind: String): Flow<List<ChequeEntity>>

    @Query("""
        SELECT * FROM cheques
        WHERE store_id = :storeId
          AND status IN ('IN_SAFE', 'DEPOSITED', 'SPENT')
        ORDER BY due_date ASC
    """)
    fun observeOpen(storeId: String): Flow<List<ChequeEntity>>

    @Query("""
        SELECT * FROM cheques
        WHERE store_id = :storeId
          AND status IN ('IN_SAFE', 'DEPOSITED', 'SPENT')
          AND due_date BETWEEN :fromMillis AND :toMillis
        ORDER BY due_date ASC
    """)
    fun observeDueSoon(storeId: String, fromMillis: Long, toMillis: Long): Flow<List<ChequeEntity>>

    @Query("SELECT * FROM cheques WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChequeEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ChequeEntity)

    @Update
    suspend fun update(entity: ChequeEntity)

    @Query("UPDATE cheques SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}