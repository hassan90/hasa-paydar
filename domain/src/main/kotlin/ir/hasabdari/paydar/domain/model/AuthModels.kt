package ir.hasabdari.paydar.domain.model

enum class AuthState {
    INITIAL,
    NEEDS_PIN_SETUP,
    LOCKED,
    UNLOCKED
}

data class LockoutState(
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0L
) {
    val isLocked: Boolean get() = lockedUntil > System.currentTimeMillis()
    val remainingSeconds: Long get() = ((lockedUntil - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
}

data class AuthConfig(
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 5
)