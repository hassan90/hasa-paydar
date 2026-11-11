package ir.hasabdari.paydar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import ir.hasabdari.paydar.data.local.entity.StockEntity
import ir.hasabdari.paydar.data.local.entity.StockMovementEntity
import ir.hasabdari.paydar.data.local.entity.WarehouseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarehouseDao {
    @Query("""
        SELECT * FROM warehouses
        WHERE store_id = :storeId AND is_active = 1
        ORDER BY is_default DESC, name COLLATE NOCASE
    """)
    fun observeAll(storeId: String): Flow<List<WarehouseEntity>>

    @Query("SELECT * FROM warehouses WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WarehouseEntity?

    @Query("""
        SELECT * FROM warehouses
        WHERE store_id = :storeId AND is_default = 1 AND is_active = 1
        LIMIT 1
    """)
    suspend fun getDefault(storeId: String): WarehouseEntity?

    @Query("SELECT COUNT(*) FROM warehouses WHERE store_id = :storeId AND is_active = 1")
    suspend fun count(storeId: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: WarehouseEntity)

    @Update
    suspend fun update(entity: WarehouseEntity)

    @Query("UPDATE warehouses SET is_active = 0, updated_at = :ts WHERE id = :id")
    suspend fun deactivate(id: String, ts: Long)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock WHERE product_id = :productId AND warehouse_id = :warehouseId LIMIT 1")
    suspend fun get(productId: String, warehouseId: String): StockEntity?

    @Upsert
    suspend fun upsert(entity: StockEntity)

    /**
     * نمایش موجودی هر انبار برای یک کالا.
     */
    @Query("""
        SELECT w.id as warehouse_id, w.name as warehouse_name,
               COALESCE(s.quantity, 0) as quantity,
               COALESCE(s.avg_cost, 0) as avg_cost
        FROM warehouses w
        LEFT JOIN stock s ON s.product_id = :productId AND s.warehouse_id = w.id
        WHERE w.store_id = :storeId AND w.is_active = 1
        ORDER BY w.is_default DESC, w.name COLLATE NOCASE
    """)
    fun observeStockByWarehouse(productId: String, storeId: String): Flow<List<WarehouseStockRaw>>
}

data class WarehouseStockRaw(
    @androidx.room.ColumnInfo(name = "warehouse_id") val warehouseId: String,
    @androidx.room.ColumnInfo(name = "warehouse_name") val warehouseName: String,
    @androidx.room.ColumnInfo(name = "quantity") val quantityScaled: Long,
    @androidx.room.ColumnInfo(name = "avg_cost") val avgCostRials: Long
)

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: StockMovementEntity)

    @Query("""
        SELECT * FROM stock_movements
        WHERE product_id = :productId AND store_id = :storeId
        ORDER BY occurred_at DESC
        LIMIT 100
    """)
    suspend fun getByProduct(productId: String, storeId: String): List<StockMovementEntity>
}