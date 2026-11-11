package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.AppSettings
import ir.hasabdari.paydar.domain.model.StoreInfo
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    fun observeStoreInfo(): Flow<StoreInfo>
    fun observeDarkMode(): Flow<Boolean>
    fun observeProMode(): Flow<Boolean>

    suspend fun getSettings(): AppSettings

    suspend fun updateStoreInfo(info: StoreInfo): AppResult<Unit>
    suspend fun setDefaultTaxRate(basisPoints: Int): AppResult<Unit>
    suspend fun setDarkMode(enabled: Boolean): AppResult<Unit>
    suspend fun setProMode(enabled: Boolean): AppResult<Unit>
    suspend fun setFontSize(size: String): AppResult<Unit>
    suspend fun setBiometricLock(enabled: Boolean): AppResult<Unit>
    suspend fun setAutoBackupEnabled(enabled: Boolean): AppResult<Unit>
    suspend fun recordBackupCompleted(timestamp: Long): AppResult<Unit>
    suspend fun setPricingMethod(method: String): AppResult<Unit>
}