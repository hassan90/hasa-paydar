package ir.hasabdari.paydar.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hasabdari.paydar.data.local.entity.ProductGroupEntity
import kotlinx.coroutines.flow.Flow

data class GroupWithCountRaw(
    @androidx.room.Embedded val group: ProductGroupEntity,
    @ColumnInfo(name = "product_count") val productCount: Int
)

@Dao
interface ProductGroupDao {

    @Query("""
        SELECT * FROM product_groups
        WHERE store_id = :storeId AND deleted_at IS NULL AND is_active = 1
        ORDER BY name COLLATE NOCASE
    """)
    fun observeAll(storeId: String): Flow<List<ProductGroupEntity>>

    @Query("""
        SELECT g.*,
               (SELECT COUNT(*) FROM products p
                WHERE p.group_id = g.id AND p.deleted_at IS NULL) as product_count
        FROM product_groups g
        WHERE g.store_id = :storeId AND g.deleted_at IS NULL AND g.is_active = 1
        ORDER BY g.name COLLATE NOCASE
    """)
    fun observeAllWithCount(storeId: String): Flow<List<GroupWithCountRaw>>

    @Query("SELECT * FROM product_groups WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getById(id: String): ProductGroupEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProductGroupEntity)

    @Update
    suspend fun update(entity: ProductGroupEntity)

    @Query("UPDATE product_groups SET deleted_at = :ts, updated_at = :ts WHERE id = :id")
    suspend fun softDelete(id: String, ts: Long)
}