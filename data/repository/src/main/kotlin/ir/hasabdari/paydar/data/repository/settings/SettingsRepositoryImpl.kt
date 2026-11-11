package ir.hasabdari.paydar.data.repository.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.AppSettings
import ir.hasabdari.paydar.domain.model.StoreInfo
import ir.hasabdari.paydar.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object K {
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_LOGO_URI = stringPreferencesKey("store_logo_uri")
        val STORE_PHONE = stringPreferencesKey("store_phone")
        val STORE_MOBILE = stringPreferencesKey("store_mobile")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val STORE_PROVINCE = stringPreferencesKey("store_province")
        val STORE_CITY = stringPreferencesKey("store_city")
        val STORE_NATIONAL_ID = stringPreferencesKey("store_national_id")
        val STORE_ECONOMIC_CODE = stringPreferencesKey("store_economic_code")
        val STORE_POSTAL_CODE = stringPreferencesKey("store_postal_code")

        val DEFAULT_TAX_RATE = intPreferencesKey("default_tax_rate")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PRO_MODE = booleanPreferencesKey("pro_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val PRICING_METHOD = stringPreferencesKey("pricing_method")
    }

    // ═══════════════════════════════════════════════════════════
    // Observers
    // ═══════════════════════════════════════════════════════════
    override fun observeSettings(): Flow<AppSettings> =
        dataStore.data.map { prefs -> prefs.toAppSettings() }

    override fun observeStoreInfo(): Flow<StoreInfo> =
        dataStore.data.map { prefs -> prefs.toStoreInfo() }

    override fun observeDarkMode(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[K.DARK_MODE] ?: false }

    override fun observeProMode(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[K.PRO_MODE] ?: false }

    override suspend fun getSettings(): AppSettings =
        dataStore.data.first().toAppSettings()

    // ═══════════════════════════════════════════════════════════
    // Writes
    // ═══════════════════════════════════════════════════════════
    override suspend fun updateStoreInfo(info: StoreInfo): AppResult<Unit> = try {
        dataStore.edit { prefs ->
            prefs[K.STORE_NAME] = info.name
            prefs[K.STORE_PHONE] = info.phone
            prefs[K.STORE_MOBILE] = info.mobile
            prefs[K.STORE_ADDRESS] = info.address
            prefs[K.STORE_PROVINCE] = info.province
            prefs[K.STORE_CITY] = info.city
            prefs[K.STORE_NATIONAL_ID] = info.nationalId
            prefs[K.STORE_ECONOMIC_CODE] = info.economicCode
            prefs[K.STORE_POSTAL_CODE] = info.postalCode

            val logo = info.logoUri
            if (logo.isNullOrBlank()) {
                prefs.remove(K.STORE_LOGO_URI)
            } else {
                prefs[K.STORE_LOGO_URI] = logo
            }
        }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setDefaultTaxRate(basisPoints: Int): AppResult<Unit> = try {
        dataStore.edit { prefs ->
            prefs[K.DEFAULT_TAX_RATE] = basisPoints.coerceIn(0, 10_000)
        }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setDarkMode(enabled: Boolean): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.DARK_MODE] = enabled }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setProMode(enabled: Boolean): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.PRO_MODE] = enabled }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setFontSize(size: String): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.FONT_SIZE] = size }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setBiometricLock(enabled: Boolean): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.BIOMETRIC_LOCK] = enabled }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.AUTO_BACKUP_ENABLED] = enabled }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun recordBackupCompleted(timestamp: Long): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.LAST_BACKUP_AT] = timestamp }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun setPricingMethod(method: String): AppResult<Unit> = try {
        dataStore.edit { prefs -> prefs[K.PRICING_METHOD] = method }
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══════════════════════════════════════════════════════════
    // Mappers
    // ═══════════════════════════════════════════════════════════
    private fun Preferences.toStoreInfo(): StoreInfo = StoreInfo(
        name = this[K.STORE_NAME] ?: "",
        logoUri = this[K.STORE_LOGO_URI],
        phone = this[K.STORE_PHONE] ?: "",
        mobile = this[K.STORE_MOBILE] ?: "",
        address = this[K.STORE_ADDRESS] ?: "",
        province = this[K.STORE_PROVINCE] ?: "",
        city = this[K.STORE_CITY] ?: "",
        nationalId = this[K.STORE_NATIONAL_ID] ?: "",
        economicCode = this[K.STORE_ECONOMIC_CODE] ?: "",
        postalCode = this[K.STORE_POSTAL_CODE] ?: ""
    )

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        storeInfo = toStoreInfo(),
        defaultTaxRateBasisPoints = this[K.DEFAULT_TAX_RATE] ?: 1000,
        darkMode = this[K.DARK_MODE] ?: false,
        proMode = this[K.PRO_MODE] ?: false,
        fontSize = this[K.FONT_SIZE] ?: "MEDIUM",
        biometricLock = this[K.BIOMETRIC_LOCK] ?: false,
        autoBackupEnabled = this[K.AUTO_BACKUP_ENABLED] ?: true,
        lastBackupAt = this[K.LAST_BACKUP_AT],
        pricingMethod = this[K.PRICING_METHOD] ?: "AVERAGE"
    )
}