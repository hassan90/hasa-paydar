package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "warehouses",
    indices = [Index(value = ["store_id", "name"], unique = true)]
)
data class WarehouseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "stock",
    primaryKeys = ["product_id", "warehouse_id"],
    indices = [Index(value = ["warehouse_id"])]
)
data class StockEntity(
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "warehouse_id") val warehouseId: String,
    @ColumnInfo(name = "quantity") val quantityScaled: Long,
    @ColumnInfo(name = "avg_cost") val avgCostRials: Long,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["product_id", "warehouse_id", "occurred_at"]),
        Index(value = ["store_id"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "warehouse_id") val warehouseId: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "quantity_delta") val quantityDeltaScaled: Long,
    @ColumnInfo(name = "unit_cost") val unitCostRials: Long,
    @ColumnInfo(name = "reference_type") val referenceType: String?,
    @ColumnInfo(name = "reference_id") val referenceId: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "store_id") val storeId: String
)