package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cash_accounts",
    indices = [Index(value = ["store_id", "name"], unique = true)]
)
data class CashAccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "bank_name") val bankName: String?,
    @ColumnInfo(name = "account_number") val accountNumber: String?,
    @ColumnInfo(name = "opening_balance") val openingBalance: Long,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "store_id") val storeId: String
)

@Entity(
    tableName = "cash_transactions",
    indices = [
        Index(value = ["cash_account_id", "occurred_at"]),
        Index(value = ["to_cash_account_id"]),
        Index(value = ["store_id"])
    ]
)
data class CashTransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "method") val method: String,
    @ColumnInfo(name = "cash_account_id") val cashAccountId: String,
    @ColumnInfo(name = "to_cash_account_id") val toCashAccountId: String?,
    @ColumnInfo(name = "party_id") val partyId: String?,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "store_id") val storeId: String
)