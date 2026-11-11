package ir.hasabdari.paydar.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    indices = [
        Index(value = ["store_id", "fiscal_year_id", "type", "number"], unique = true),
        Index(value = ["party_id"]),
        Index(value = ["issue_date"]),
        Index(value = ["store_id", "type"])
    ]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "number") val number: String,
    @ColumnInfo(name = "issue_date") val issueDate: Long,
    @ColumnInfo(name = "due_date") val dueDate: Long?,
    @ColumnInfo(name = "party_id") val partyId: String?,
    @ColumnInfo(name = "warehouse_id") val warehouseId: String,
    @ColumnInfo(name = "settlement_type") val settlementType: String,
    @ColumnInfo(name = "paid_amount") val paidAmount: Long,
    @ColumnInfo(name = "subtotal") val subtotal: Long,
    @ColumnInfo(name = "discount_total") val discountTotal: Long,
    @ColumnInfo(name = "tax_total") val taxTotal: Long,
    @ColumnInfo(name = "shipping_cost") val shippingCost: Long,
    @ColumnInfo(name = "other_costs") val otherCosts: Long,
    @ColumnInfo(name = "grand_total") val grandTotal: Long,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_by") val createdBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "fiscal_year_id") val fiscalYearId: String
)

@Entity(
    tableName = "invoice_lines",
    indices = [
        Index(value = ["invoice_id"]),
        Index(value = ["product_id"])
    ]
)
data class InvoiceLineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "invoice_id") val invoiceId: String,
    @ColumnInfo(name = "line_no") val lineNo: Int,
    @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "unit_id") val unitId: String,
    @ColumnInfo(name = "quantity") val quantityScaled: Long,
    @ColumnInfo(name = "unit_price") val unitPrice: Long,
    @ColumnInfo(name = "discount") val discount: Long,
    @ColumnInfo(name = "tax_rate") val taxRate: Int,
    @ColumnInfo(name = "line_total") val lineTotal: Long,
    @ColumnInfo(name = "cost_at_sale") val costAtSale: Long?
)