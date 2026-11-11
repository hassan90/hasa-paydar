package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.DashboardData
import ir.hasabdari.paydar.domain.repository.DashboardRepository
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor(
    private val repo: DashboardRepository
) {
    suspend operator fun invoke(
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<DashboardData> = repo.getDashboardData(storeId)
}