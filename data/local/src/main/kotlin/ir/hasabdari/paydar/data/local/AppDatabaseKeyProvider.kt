package ir.hasabdari.paydar.data.local

import ir.hasabdari.paydar.data.local.security.SecureKeyManager   // ← import جدید
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDatabaseKeyProvider @Inject constructor(
    private val keyManager: SecureKeyManager
) {
    fun passphrase(): ByteArray = keyManager.getOrCreateDatabasePassword()
}