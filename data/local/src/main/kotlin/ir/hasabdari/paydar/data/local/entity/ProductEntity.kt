package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"]),
        Index(value = ["store_id", "code"], unique = true)
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "barcode") val barcode: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "unit_id") val unitId: String,
    @ColumnInfo(name = "purchase_price") val purchasePrice: Long,
    @ColumnInfo(name = "sale_price") val salePrice: Long,
    @ColumnInfo(name = "min_stock") val minStockScaled: Long,
    @ColumnInfo(name = "max_stock") val maxStockScaled: Long?,
    @ColumnInfo(name = "is_service") val isService: Boolean,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "track_serial") val trackSerial: Boolean,
    @ColumnInfo(name = "track_expiry") val trackExpiry: Boolean,
    @ColumnInfo(name = "image_uri") val imageUri: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "store_id") val storeId: String
)