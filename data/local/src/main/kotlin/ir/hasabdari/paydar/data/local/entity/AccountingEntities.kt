package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent_code") val parentCode: String?,
    @ColumnInfo(name = "level") val level: Int,
    @ColumnInfo(name = "nature") val nature: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "is_postable") val isPostable: Boolean,
    @ColumnInfo(name = "is_active") val isActive: Boolean
)

@Entity(
    tableName = "fiscal_years",
    indices = [Index(value = ["store_id", "start_date"])]
)
data class FiscalYearEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long,
    @ColumnInfo(name = "is_closed") val isClosed: Boolean,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["store_id", "fiscal_year_id", "number"], unique = true),
        Index(value = ["source_type", "source_id"]),
        Index(value = ["entry_date"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "number") val number: Long,
    @ColumnInfo(name = "fiscal_year_id") val fiscalYearId: String,
    @ColumnInfo(name = "entry_date") val entryDate: Long,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "source_type") val sourceType: String,
    @ColumnInfo(name = "source_id") val sourceId: String?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "reversal_of") val reversalOf: String?,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "store_id") val storeId: String
)

@Entity(
    tableName = "journal_lines",
    indices = [
        Index(value = ["entry_id"]),
        Index(value = ["account_code"]),
        Index(value = ["party_id"])
    ]
)
data class JournalLineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "line_no") val lineNo: Int,
    @ColumnInfo(name = "account_code") val accountCode: String,
    @ColumnInfo(name = "party_id") val partyId: String?,
    @ColumnInfo(name = "debit") val debit: Long,
    @ColumnInfo(name = "credit") val credit: Long,
    @ColumnInfo(name = "description") val description: String?
)