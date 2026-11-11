package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.model.InvoiceLine
import ir.hasabdari.paydar.domain.model.InvoiceType
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    fun observeAll(storeId: String): Flow<List<Invoice>>
    fun observeByType(storeId: String, type: InvoiceType): Flow<List<Invoice>>
    fun observeByTypes(storeId: String, types: List<InvoiceType>): Flow<List<Invoice>>
    suspend fun getById(id: String): AppResult<InvoiceWithLines?>
    suspend fun getNextNumber(storeId: String, fiscalYearId: String, type: InvoiceType): AppResult<String>
    suspend fun saveInvoiceWithLines(invoice: Invoice, lines: List<InvoiceLine>): AppResult<String>
    suspend fun getPartyOutstandingBalance(partyId: String, storeId: String): AppResult<Long>

    /** مقدار برگشت‌شده‌ی یک کالا از این فاکتور (جمع همه‌ی برگشتی‌های قبلی) */
    suspend fun getReturnedQuantity(originalInvoiceId: String, productId: String): AppResult<Long>
}