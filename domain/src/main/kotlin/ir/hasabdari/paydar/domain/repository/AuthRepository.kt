package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.AuthConfig
import ir.hasabdari.paydar.domain.model.AuthState
import ir.hasabdari.paydar.domain.model.LockoutState
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeAuthState(): Flow<AuthState>
    fun observeConfig(): Flow<AuthConfig>
    fun observeLockout(): Flow<LockoutState>

    suspend fun isPinSetup(): Boolean
    suspend fun setupPin(pin: String): AppResult<Unit>
    suspend fun verifyPin(pin: String): AppResult<Boolean>
    suspend fun changePin(oldPin: String, newPin: String): AppResult<Unit>

    suspend fun setPinEnabled(enabled: Boolean): AppResult<Unit>
    suspend fun setBiometricEnabled(enabled: Boolean): AppResult<Unit>
    suspend fun setAutoLockMinutes(minutes: Int): AppResult<Unit>

    suspend fun lock()
    suspend fun unlockWithBiometric(): AppResult<Unit>

    suspend fun recordLastActiveAt(timestamp: Long)
    suspend fun shouldAutoLock(): Boolean
}