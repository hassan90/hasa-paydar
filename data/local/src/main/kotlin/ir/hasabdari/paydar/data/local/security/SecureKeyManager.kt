package ir.hasabdari.paydar.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureKeyManager @Inject constructor() {

    private val keystore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getOrCreateKeystoreKey(): SecretKey {
        val existing = keystore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    fun getOrCreateDatabasePassword(): ByteArray {
        val encrypted = SecurePrefs.get(DB_PASSWORD_KEY)
        if (encrypted != null) {
            decrypt(encrypted)?.let { return it }
        }
        val newPassword = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encryptedBlob = encrypt(newPassword)
        SecurePrefs.set(DB_PASSWORD_KEY, encryptedBlob)
        return newPassword
    }

    fun rotateDatabasePassword(): ByteArray {
        val newPassword = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val encryptedBlob = encrypt(newPassword)
        SecurePrefs.set(DB_PASSWORD_KEY, encryptedBlob)
        return newPassword
    }

    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return iv + encrypted
    }

    private fun decrypt(blob: ByteArray): ByteArray? {
        return try {
            if (blob.size < IV_SIZE) return null
            val iv = blob.copyOfRange(0, IV_SIZE)
            val ciphertext = blob.copyOfRange(IV_SIZE, blob.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "hasabdari_master_key_v1"
        private const val DB_PASSWORD_KEY = "db_password_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
    }
}