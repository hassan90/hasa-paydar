package ir.hasabdari.paydar.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.hasabdari.paydar.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

data class ProductWithStockRaw(
    @Embedded val product: ProductEntity,
    @ColumnInfo(name = "total_stock_scaled") val totalStockScaled: Long,
    @ColumnInfo(name = "total_value_rials") val totalValueRials: Long
)

@Dao
interface ProductDao {

    // ═════ متدهای ساده (F0) — برای سازگاری با ProductRepository ═════

    @Query("""
        SELECT * FROM products
        WHERE store_id = :storeId AND deleted_at IS NULL
        ORDER BY name COLLATE NOCASE
    """)
    fun observeAll(storeId: String): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE store_id = :storeId
          AND deleted_at IS NULL
          AND (name LIKE '%' || :query || '%'
               OR code LIKE '%' || :query || '%'
               OR barcode LIKE '%' || :query || '%')
        ORDER BY name COLLATE NOCASE
    """)
    fun search(storeId: String, query: String): Flow<List<ProductEntity>>

    // ═════ متدهای با موجودی (F1.5) ═════

    @Query("""
        SELECT p.*,
            (SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE product_id = p.id) as total_stock_scaled,
            (SELECT COALESCE(SUM(quantity * avg_cost / 1000), 0) FROM stock WHERE product_id = p.id) as total_value_rials
        FROM products p
        WHERE p.store_id = :storeId AND p.deleted_at IS NULL
        ORDER BY p.name COLLATE NOCASE
    """)
    fun observeAllWithStock(storeId: String): Flow<List<ProductWithStockRaw>>

    @Query("""
        SELECT p.*,
            (SELECT COALESCE(SUM(quantity), 0) FROM stock WHERE product_id = p.id) as total_stock_scaled,
            (SELECT COALESCE(SUM(quantity * avg_cost / 1000), 0) FROM stock WHERE product_id = p.id) as total_value_rials
        FROM products p
        WHERE p.store_id = :storeId
          AND p.deleted_at IS NULL
          AND (p.name LIKE '%' || :query || '%'
               OR p.code LIKE '%' || :query || '%'
               OR p.barcode LIKE '%' || :query || '%')
        ORDER BY p.name COLLATE NOCASE
    """)
    fun searchWithStock(storeId: String, query: String): Flow<List<ProductWithStockRaw>>

    // ═════ CRUD ═════

    @Query("SELECT * FROM products WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND store_id = :storeId AND deleted_at IS NULL LIMIT 1")
    suspend fun getByBarcode(storeId: String, barcode: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ProductEntity): Long

    @Update
    suspend fun update(entity: ProductEntity)

    @Query("UPDATE products SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)
}