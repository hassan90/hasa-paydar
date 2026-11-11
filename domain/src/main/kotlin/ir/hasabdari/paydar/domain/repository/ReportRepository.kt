package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.BalanceSheet
import ir.hasabdari.paydar.domain.model.IncomeStatement
import ir.hasabdari.paydar.domain.model.InventoryValueReport
import ir.hasabdari.paydar.domain.model.PartyBalanceReport
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import ir.hasabdari.paydar.domain.model.SalesReport

interface ReportRepository {
    suspend fun incomeStatement(storeId: String, fromDate: Long, toDate: Long): AppResult<IncomeStatement>
    suspend fun balanceSheet(storeId: String, asOfDate: Long): AppResult<BalanceSheet>
    suspend fun salesReport(storeId: String, fromDate: Long, toDate: Long): AppResult<SalesReport>
    suspend fun partyBalances(storeId: String): AppResult<PartyBalanceReport>
    suspend fun inventoryValues(storeId: String): AppResult<InventoryValueReport>
    suspend fun partyDetailReport(
        storeId: String,
        partyId: String,
        fromDate: Long,
        toDate: Long
    ): AppResult<PartyDetailReport>
}