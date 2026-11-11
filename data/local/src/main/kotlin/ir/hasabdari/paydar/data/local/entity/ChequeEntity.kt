package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cheques",
    indices = [
        Index(value = ["store_id", "kind"]),
        Index(value = ["store_id", "status"]),
        Index(value = ["due_date"]),
        Index(value = ["party_id"]),
        Index(value = ["number"])
    ]
)
data class ChequeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "number") val number: String,
    @ColumnInfo(name = "amount") val amount: Long,
    @ColumnInfo(name = "issue_date") val issueDate: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long,
    @ColumnInfo(name = "bank_name") val bankName: String?,
    @ColumnInfo(name = "branch") val branch: String?,
    @ColumnInfo(name = "account_holder") val accountHolder: String?,
    @ColumnInfo(name = "account_number") val accountNumber: String?,
    @ColumnInfo(name = "party_id") val partyId: String?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "related_invoice_id") val relatedInvoiceId: String?,
    @ColumnInfo(name = "note") val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "store_id") val storeId: String
)