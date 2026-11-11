package ir.hasabdari.paydar.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePrefs {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            "hasabdari_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(key: String): ByteArray? {
        if (!::prefs.isInitialized) return null
        val encoded = prefs.getString(key, null) ?: return null
        return runCatching { Base64.decode(encoded, Base64.NO_WRAP) }.getOrNull()
    }

    fun set(key: String, value: ByteArray) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(key, Base64.encodeToString(value, Base64.NO_WRAP)).apply()
    }

    fun remove(key: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(key).apply()
    }

    fun getString(key: String): String? {
        if (!::prefs.isInitialized) return null
        return prefs.getString(key, null)
    }

    fun putString(key: String, value: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        if (!::prefs.isInitialized) return default
        return prefs.getBoolean(key, default)
    }

    fun putBoolean(key: String, value: Boolean) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getLong(key: String, default: Long = 0L): Long {
        if (!::prefs.isInitialized) return default
        return prefs.getLong(key, default)
    }

    fun putLong(key: String, value: Long) {
        if (!::prefs.isInitialized) return
        prefs.edit().putLong(key, value).apply()
    }
}