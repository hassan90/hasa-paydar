package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.AuthConfig
import ir.hasabdari.paydar.domain.model.AuthState
import ir.hasabdari.paydar.domain.model.LockoutState
import ir.hasabdari.paydar.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    operator fun invoke(): Flow<AuthState> = repo.observeAuthState()
}

class ObserveAuthConfigUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    operator fun invoke(): Flow<AuthConfig> = repo.observeConfig()
}

class ObserveLockoutUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    operator fun invoke(): Flow<LockoutState> = repo.observeLockout()
}

class IsPinSetupUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(): Boolean = repo.isPinSetup()
}

class SetupPinUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(pin: String): AppResult<Unit> {
        if (!pin.matches(Regex("^\\d{4,6}$"))) {
            return AppResult.Failure(DomainError.InvalidInput("رمز", "۴ تا ۶ رقم وارد کنید"))
        }
        return repo.setupPin(pin)
    }
}

class VerifyPinUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(pin: String): AppResult<Boolean> = repo.verifyPin(pin)
}

class ChangePinUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(oldPin: String, newPin: String): AppResult<Unit> {
        if (!newPin.matches(Regex("^\\d{4,6}$"))) {
            return AppResult.Failure(DomainError.InvalidInput("رمز جدید", "۴ تا ۶ رقم"))
        }
        return repo.changePin(oldPin, newPin)
    }
}

class SetPinEnabledUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> = repo.setPinEnabled(enabled)
}

class SetBiometricEnabledUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(enabled: Boolean): AppResult<Unit> = repo.setBiometricEnabled(enabled)
}

class SetAutoLockMinutesUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(minutes: Int): AppResult<Unit> = repo.setAutoLockMinutes(minutes)
}

class LockAppUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke() = repo.lock()
}

class UnlockWithBiometricUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(): AppResult<Unit> = repo.unlockWithBiometric()
}

class RecordLastActiveAtUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(timestamp: Long = System.currentTimeMillis()) =
        repo.recordLastActiveAt(timestamp)
}

class ShouldAutoLockUseCase @Inject constructor(
    private val repo: AuthRepository
) {
    suspend operator fun invoke(): Boolean = repo.shouldAutoLock()
}