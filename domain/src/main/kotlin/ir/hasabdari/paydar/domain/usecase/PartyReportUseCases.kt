package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import ir.hasabdari.paydar.domain.repository.ReportRepository
import javax.inject.Inject

class GeneratePartyDetailReportUseCase @Inject constructor(
    private val repo: ReportRepository
) {
    suspend operator fun invoke(
        partyId: String,
        fromDate: Long,
        toDate: Long,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<PartyDetailReport> = repo.partyDetailReport(storeId, partyId, fromDate, toDate)
}