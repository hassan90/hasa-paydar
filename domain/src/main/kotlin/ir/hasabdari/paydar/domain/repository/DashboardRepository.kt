package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.DashboardData

interface DashboardRepository {
    suspend fun getDashboardData(storeId: String): AppResult<DashboardData>
}