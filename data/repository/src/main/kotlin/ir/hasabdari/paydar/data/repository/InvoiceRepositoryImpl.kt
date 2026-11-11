package ir.hasabdari.paydar.data.repository

import androidx.room.withTransaction
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.PaydarDatabase
import ir.hasabdari.paydar.data.local.dao.InvoiceDao
import ir.hasabdari.paydar.data.local.entity.InvoiceEntity
import ir.hasabdari.paydar.data.local.entity.InvoiceLineEntity
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.model.InvoiceLine
import ir.hasabdari.paydar.domain.model.InvoiceStatus
import ir.hasabdari.paydar.domain.model.InvoiceType
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val db: PaydarDatabase,
    private val dao: InvoiceDao
) : InvoiceRepository {

    override fun observeAll(storeId: String): Flow<List<Invoice>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override fun observeByType(storeId: String, type: InvoiceType): Flow<List<Invoice>> =
        dao.observeByType(storeId, type.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<InvoiceWithLines?> = try {
        val entity = dao.getById(id)
        if (entity == null) {
            AppResult.Success(null)
        } else {
            val lines = dao.getLines(id)
            AppResult.Success(
                InvoiceWithLines(
                    invoice = entity.toDomain(),
                    lines = lines.map { it.toDomain() }
                )
            )
        }
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun getNextNumber(
        storeId: String,
        fiscalYearId: String,
        type: InvoiceType
    ): AppResult<String> = try {
        val count = dao.countByType(storeId, fiscalYearId, type.name)
        val next = count + 1
        val prefix = when (type) {
            InvoiceType.PURCHASE -> "PUR"
            InvoiceType.SALE -> "SAL"
            InvoiceType.PURCHASE_RETURN -> "PRT"
            InvoiceType.SALE_RETURN -> "SRT"
            InvoiceType.PROFORMA -> "PRF"
        }
        AppResult.Success("$prefix-${"%04d".format(next)}")
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun saveInvoiceWithLines(
        invoice: Invoice,
        lines: List<InvoiceLine>
    ): AppResult<String> = try {
        db.withTransaction {
            dao.insertInvoice(invoice.toEntity())
            dao.insertLines(lines.map { it.toEntity() })
        }
        AppResult.Success(invoice.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun getPartyOutstandingBalance(partyId: String, storeId: String): AppResult<Long> = try {
        AppResult.Success(dao.getPartyOutstandingBalance(partyId, storeId))
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override fun observeByTypes(storeId: String, types: List<InvoiceType>): Flow<List<Invoice>> =
        dao.observeByTypes(storeId, types.map { it.name }).map { list -> list.map { it.toDomain() } }

    override suspend fun getReturnedQuantity(originalInvoiceId: String, productId: String): AppResult<Long> = try {
        AppResult.Success(dao.getReturnedQuantity(originalInvoiceId, productId))
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

// ═══════════════════════════════════════════════════════════
// Mappers
// ═══════════════════════════════════════════════════════════

private fun InvoiceEntity.toDomain(): Invoice = Invoice(
    id = id,
    type = InvoiceType.valueOf(type),
    number = number,
    issueDate = issueDate,
    dueDate = dueDate,
    partyId = partyId,
    warehouseId = warehouseId,
    settlementType = SettlementType.valueOf(settlementType),
    paidAmount = Money.fromRials(paidAmount),
    subtotal = Money.fromRials(subtotal),
    discountTotal = Money.fromRials(discountTotal),
    taxTotal = Money.fromRials(taxTotal),
    shippingCost = Money.fromRials(shippingCost),
    otherCosts = Money.fromRials(otherCosts),
    grandTotal = Money.fromRials(grandTotal),
    status = InvoiceStatus.valueOf(status),
    parentId = parentId,
    notes = notes,
    createdBy = createdBy,
    createdAt = createdAt,
    storeId = storeId,
    fiscalYearId = fiscalYearId
)

private fun Invoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    type = type.name,
    number = number,
    issueDate = issueDate,
    dueDate = dueDate,
    partyId = partyId,
    warehouseId = warehouseId,
    settlementType = settlementType.name,
    paidAmount = paidAmount.rials,
    subtotal = subtotal.rials,
    discountTotal = discountTotal.rials,
    taxTotal = taxTotal.rials,
    shippingCost = shippingCost.rials,
    otherCosts = otherCosts.rials,
    grandTotal = grandTotal.rials,
    status = status.name,
    parentId = parentId,
    notes = notes,
    createdBy = createdBy,
    createdAt = createdAt,
    storeId = storeId,
    fiscalYearId = fiscalYearId
)

private fun InvoiceLineEntity.toDomain(): InvoiceLine = InvoiceLine(
    id = id,
    invoiceId = invoiceId,
    lineNo = lineNo,
    productId = productId,
    description = description,
    unitId = unitId,
    quantityScaled = quantityScaled,
    unitPrice = Money.fromRials(unitPrice),
    discount = Money.fromRials(discount),
    taxRate = taxRate,
    lineTotal = Money.fromRials(lineTotal),
    costAtSale = costAtSale?.let { Money.fromRials(it) }
)

private fun InvoiceLine.toEntity(): InvoiceLineEntity = InvoiceLineEntity(
    id = id,
    invoiceId = invoiceId,
    lineNo = lineNo,
    productId = productId,
    description = description,
    unitId = unitId,
    quantityScaled = quantityScaled,
    unitPrice = unitPrice.rials,
    discount = discount.rials,
    taxRate = taxRate,
    lineTotal = lineTotal.rials,
    costAtSale = costAtSale?.rials
)