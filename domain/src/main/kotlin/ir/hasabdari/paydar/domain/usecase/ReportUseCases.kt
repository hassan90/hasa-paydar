package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.BalanceSheet
import ir.hasabdari.paydar.domain.model.IncomeStatement
import ir.hasabdari.paydar.domain.model.InventoryValueReport
import ir.hasabdari.paydar.domain.model.PartyBalanceReport
import ir.hasabdari.paydar.domain.model.SalesReport
import ir.hasabdari.paydar.domain.repository.ReportRepository
import javax.inject.Inject

class GetIncomeStatementUseCase @Inject constructor(
    private val repo: ReportRepository
) {
    suspend operator fun invoke(
        fromDate: Long,
        toDate: Long,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<IncomeStatement> = repo.incomeStatement(storeId, fromDate, toDate)
}

class GetBalanceSheetUseCase @Inject constructor(
    private val repo: ReportRepository
) {
    suspend operator fun invoke(
        asOfDate: Long = System.currentTimeMillis(),
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<BalanceSheet> = repo.balanceSheet(storeId, asOfDate)
}

class GetSalesReportUseCase @Inject constructor(
    private val repo: ReportRepository
) {
    suspend operator fun invoke(
        fromDate: Long,
        toDate: Long,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<SalesReport> = repo.salesReport(storeId, fromDate, toDate)
}

class GetPartyBalancesUseCase @Inject constructor(
    private val repo: ReportRepository
) {
    suspend operator fun invoke(
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<PartyBalanceReport> = repo.partyBalances(storeId)
}

class GetInventoryValueReportUseCase @Inject constructor(
    private val repo: ReportRepository
) {
    suspend operator fun invoke(
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<InventoryValueReport> = repo.inventoryValues(storeId)
}