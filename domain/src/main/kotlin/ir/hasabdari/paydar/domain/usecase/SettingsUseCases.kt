package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.AppSettings
import ir.hasabdari.paydar.domain.model.StoreInfo
import ir.hasabdari.paydar.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = repo.observeSettings()
}

class ObserveStoreInfoUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<StoreInfo> = repo.observeStoreInfo()
}

class ObserveDarkModeUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repo.observeDarkMode()
}

class ObserveProModeUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<Boolean> = repo.observeProMode()
}

class GetSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(): AppSettings = repo.getSettings()
}

class UpdateStoreInfoUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(info: StoreInfo): AppResult<Unit> = repo.updateStoreInfo(info)
}

class SetDarkModeUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> = repo.setDarkMode(enabled)
}

class SetProModeUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> = repo.setProMode(enabled)
}

class SetDefaultTaxRateUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(basisPoints: Int): AppResult<Unit> = repo.setDefaultTaxRate(basisPoints)
}

class SetFontSizeUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(size: String): AppResult<Unit> = repo.setFontSize(size)
}

class SetAutoBackupUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> = repo.setAutoBackupEnabled(enabled)
}