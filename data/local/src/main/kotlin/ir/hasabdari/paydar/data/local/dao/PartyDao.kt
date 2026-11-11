package ir.hasabdari.paydar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hasabdari.paydar.data.local.entity.PartyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {

    @Query("""
        SELECT * FROM parties
        WHERE store_id = :storeId AND deleted_at IS NULL
        ORDER BY name COLLATE NOCASE
    """)
    fun observeAll(storeId: String): Flow<List<PartyEntity>>

    @Query("""
        SELECT * FROM parties
        WHERE store_id = :storeId AND deleted_at IS NULL
          AND (kind = :kind OR kind = 'BOTH')
        ORDER BY name COLLATE NOCASE
    """)
    fun observeByKind(storeId: String, kind: String): Flow<List<PartyEntity>>

    @Query("""
        SELECT * FROM parties
        WHERE store_id = :storeId AND deleted_at IS NULL
          AND (name LIKE '%' || :query || '%'
               OR mobile LIKE '%' || :query || '%'
               OR code LIKE '%' || :query || '%'
               OR phone LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE
    """)
    fun search(storeId: String, query: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getById(id: String): PartyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: PartyEntity)

    @Update
    suspend fun update(entity: PartyEntity)

    @Query("UPDATE parties SET deleted_at = :ts, updated_at = :ts WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)

    @Query("SELECT COUNT(*) FROM parties WHERE store_id = :storeId AND deleted_at IS NULL AND (kind = :kind OR kind = 'BOTH')")
    suspend fun countByKind(storeId: String, kind: String): Int
}