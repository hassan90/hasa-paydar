package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "parties",
    indices = [
        Index(value = ["store_id", "code"], unique = false),
        Index(value = ["mobile"])
    ]
)
data class PartyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "code") val code: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "mobile") val mobile: String?,
    @ColumnInfo(name = "national_id") val nationalId: String?,
    @ColumnInfo(name = "economic_code") val economicCode: String? = null,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "province") val province: String? = null,
    @ColumnInfo(name = "city") val city: String? = null,
    @ColumnInfo(name = "credit_limit") val creditLimit: Long,
    @ColumnInfo(name = "discount_percent") val discountPercent: Int,
    @ColumnInfo(name = "group_name") val groupName: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
    @ColumnInfo(name = "sync_state") val syncState: String,
    @ColumnInfo(name = "store_id") val storeId: String
)