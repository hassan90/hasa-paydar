package ir.hasabdari.paydar.domain.model

data class StoreInfo(
    val name: String = "",
    val logoUri: String? = null,
    val phone: String = "",
    val mobile: String = "",
    val address: String = "",
    val province: String = "",
    val city: String = "",
    val nationalId: String = "",
    val economicCode: String = "",
    val postalCode: String = ""
) {
    val isConfigured: Boolean get() = name.isNotBlank()
}

data class AppSettings(
    val storeInfo: StoreInfo = StoreInfo(),
    val defaultTaxRateBasisPoints: Int = 1000,   // 1000 = 10%
    val darkMode: Boolean = false,
    val proMode: Boolean = false,
    val fontSize: String = "MEDIUM",             // SMALL, MEDIUM, LARGE
    val biometricLock: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val lastBackupAt: Long? = null,
    val pricingMethod: String = "AVERAGE"        // AVERAGE or FIFO
)