package ir.hasabdari.paydar.data.repository.auth

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.security.SecurePrefs
import ir.hasabdari.paydar.domain.model.AuthConfig
import ir.hasabdari.paydar.domain.model.AuthState
import ir.hasabdari.paydar.domain.model.LockoutState
import ir.hasabdari.paydar.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    // ─── تعیین وضعیت اولیه از روی ذخیره‌سازی (PIN فعال؟ رمز ست شده؟) ───
    private val _authState = MutableStateFlow(computeInitialState())

    override fun observeAuthState(): Flow<AuthState> = _authState.asStateFlow()

    private val _lockout = MutableStateFlow(LockoutState())
    override fun observeLockout(): Flow<LockoutState> = _lockout.asStateFlow()

    override fun observeConfig(): Flow<AuthConfig> = _authState.map { loadConfig() }

    private fun computeInitialState(): AuthState {
        val pinEnabled = SecurePrefs.getBoolean(KEY_PIN_ENABLED, false)
        val pinSetup = SecurePrefs.getString(KEY_PIN_HASH) != null
        return when {
            !pinEnabled -> AuthState.UNLOCKED
            !pinSetup -> AuthState.NEEDS_PIN_SETUP
            else -> AuthState.LOCKED
        }
    }

    // ═══════ PIN ═══════
    override suspend fun isPinSetup(): Boolean =
        SecurePrefs.getString(KEY_PIN_HASH) != null

    override suspend fun setupPin(pin: String): AppResult<Unit> = try {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        SecurePrefs.putString(KEY_PIN_SALT, salt.toBase64())
        SecurePrefs.putString(KEY_PIN_HASH, hash.toBase64())
        SecurePrefs.putBoolean(KEY_PIN_ENABLED, true)
        SecurePrefs.putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
        _authState.value = AuthState.UNLOCKED
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun verifyPin(pin: String): AppResult<Boolean> {
        val lockout = _lockout.value
        if (lockout.isLocked) {
            return AppResult.Failure(
                DomainError.InvalidInput(
                    "قفل موقت",
                    "لطفاً ${lockout.remainingSeconds} ثانیه دیگر امتحان کنید"
                )
            )
        }

        val saltB64 = SecurePrefs.getString(KEY_PIN_SALT) ?: return AppResult.Success(false)
        val storedHashB64 = SecurePrefs.getString(KEY_PIN_HASH) ?: return AppResult.Success(false)
        val salt = saltB64.fromBase64() ?: return AppResult.Success(false)
        val storedHash = storedHashB64.fromBase64() ?: return AppResult.Success(false)
        val inputHash = hashPin(pin, salt)

        return if (MessageDigest.isEqual(storedHash, inputHash)) {
            _lockout.value = LockoutState()
            SecurePrefs.putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
            _authState.value = AuthState.UNLOCKED
            AppResult.Success(true)
        } else {
            val newAttempts = lockout.failedAttempts + 1
            val lockUntil = when {
                newAttempts >= 5 -> System.currentTimeMillis() + 5 * 60_000L
                newAttempts >= 3 -> System.currentTimeMillis() + 30_000L
                else -> 0L
            }
            _lockout.value = LockoutState(newAttempts, lockUntil)
            AppResult.Success(false)
        }
    }

    override suspend fun changePin(oldPin: String, newPin: String): AppResult<Unit> {
        when (val r = verifyPin(oldPin)) {
            is AppResult.Failure -> return r
            is AppResult.Success -> if (!r.data) {
                return AppResult.Failure(DomainError.InvalidInput("رمز قدیم", "اشتباه است"))
            }
        }
        return setupPin(newPin)
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // ═══════ Config ═══════
    override suspend fun setPinEnabled(enabled: Boolean): AppResult<Unit> = try {
        SecurePrefs.putBoolean(KEY_PIN_ENABLED, enabled)
        if (!enabled) {
            _authState.value = AuthState.UNLOCKED
        } else if (isPinSetup()) {
            _authState.value = AuthState.LOCKED
        } else {
            _authState.value = AuthState.NEEDS_PIN_SETUP
        }
        AppResult.Success(Unit)
    } catch (e: Exception) { AppResult.Failure(DomainError.Unknown(e)) }

    override suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit> = try {
        SecurePrefs.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
        AppResult.Success(Unit)
    } catch (e: Exception) { AppResult.Failure(DomainError.Unknown(e)) }

    override suspend fun setAutoLockMinutes(minutes: Int): AppResult<Unit> = try {
        SecurePrefs.putLong(KEY_AUTO_LOCK_MINUTES, minutes.toLong())
        AppResult.Success(Unit)
    } catch (e: Exception) { AppResult.Failure(DomainError.Unknown(e)) }

    // ═══════ Lock state ═══════
    override suspend fun lock() {
        _authState.value = if (isPinSetup() && SecurePrefs.getBoolean(KEY_PIN_ENABLED, false))
            AuthState.LOCKED else AuthState.UNLOCKED
    }

    override suspend fun unlockWithBiometric(): AppResult<Unit> {
        if (!SecurePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)) {
            return AppResult.Failure(DomainError.InvalidInput("بیومتریک", "فعال نیست"))
        }
        SecurePrefs.putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
        _authState.value = AuthState.UNLOCKED
        return AppResult.Success(Unit)
    }

    override suspend fun recordLastActiveAt(timestamp: Long) {
        SecurePrefs.putLong(KEY_LAST_ACTIVE, timestamp)
    }

    override suspend fun shouldAutoLock(): Boolean {
        val pinEnabled = SecurePrefs.getBoolean(KEY_PIN_ENABLED, false)
        if (!pinEnabled) return false
        val lastActive = SecurePrefs.getLong(KEY_LAST_ACTIVE, 0L)
        val autoLockMinutes = SecurePrefs.getLong(KEY_AUTO_LOCK_MINUTES, 5L).coerceAtLeast(1L)
        return System.currentTimeMillis() - lastActive > autoLockMinutes * 60_000L
    }

    private fun loadConfig(): AuthConfig = AuthConfig(
        pinEnabled = SecurePrefs.getBoolean(KEY_PIN_ENABLED, false),
        biometricEnabled = SecurePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
        autoLockMinutes = SecurePrefs.getLong(KEY_AUTO_LOCK_MINUTES, 5L).toInt()
    )

    private fun ByteArray.toBase64(): String =
        android.util.Base64.encodeToString(this, android.util.Base64.NO_WRAP)

    private fun String.fromBase64(): ByteArray? =
        runCatching { android.util.Base64.decode(this, android.util.Base64.NO_WRAP) }.getOrNull()

    companion object {
        private const val ITERATIONS = 200_000
        private const val KEY_PIN_HASH = "auth_pin_hash"
        private const val KEY_PIN_SALT = "auth_pin_salt"
        private const val KEY_PIN_ENABLED = "auth_pin_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "auth_biometric_enabled"
        private const val KEY_AUTO_LOCK_MINUTES = "auth_auto_lock_minutes"
        private const val KEY_LAST_ACTIVE = "auth_last_active"
    }
}