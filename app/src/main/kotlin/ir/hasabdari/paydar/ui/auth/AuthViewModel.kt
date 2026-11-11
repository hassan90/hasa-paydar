package ir.hasabdari.paydar.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.AuthConfig
import ir.hasabdari.paydar.domain.model.AuthState
import ir.hasabdari.paydar.domain.model.LockoutState
import ir.hasabdari.paydar.domain.usecase.ChangePinUseCase
import ir.hasabdari.paydar.domain.usecase.ObserveAuthConfigUseCase
import ir.hasabdari.paydar.domain.usecase.ObserveAuthStateUseCase
import ir.hasabdari.paydar.domain.usecase.ObserveLockoutUseCase
import ir.hasabdari.paydar.domain.usecase.SetupPinUseCase
import ir.hasabdari.paydar.domain.usecase.UnlockWithBiometricUseCase
import ir.hasabdari.paydar.domain.usecase.VerifyPinUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val authState: AuthState = AuthState.INITIAL,
    val lockout: LockoutState = LockoutState(),
    val config: AuthConfig = AuthConfig(),
    val pinBuffer: String = "",
    val error: String? = null,
    val isVerifying: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    observeAuthState: ObserveAuthStateUseCase,
    observeLockout: ObserveLockoutUseCase,
    observeConfig: ObserveAuthConfigUseCase,
    private val setupPin: SetupPinUseCase,
    private val verifyPin: VerifyPinUseCase,
    private val changePinUseCase: ChangePinUseCase,
    private val unlockWithBiometric: UnlockWithBiometricUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeAuthState(),
                observeLockout(),
                observeConfig()
            ) { auth, lock, cfg ->
                Triple(auth, lock, cfg)
            }.collect { (auth, lock, cfg) ->
                _uiState.update {
                    it.copy(authState = auth, lockout = lock, config = cfg)
                }
            }
        }
    }

    // ═══ PIN entry ═══
    fun onDigit(digit: String) {
        if (_uiState.value.pinBuffer.length < 6) {
            _uiState.update { it.copy(pinBuffer = it.pinBuffer + digit, error = null) }
        }
    }

    fun onBackspace() {
        _uiState.update { it.copy(pinBuffer = it.pinBuffer.dropLast(1), error = null) }
    }

    fun clearBuffer() {
        _uiState.update { it.copy(pinBuffer = "", error = null) }
    }

    fun submitPin() {
        val pin = _uiState.value.pinBuffer
        if (pin.length < 4) {
            _uiState.update { it.copy(error = "حداقل ۴ رقم وارد کنید") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isVerifying = true) }
            when (val r = verifyPin(pin)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            pinBuffer = "",
                            isVerifying = false,
                            error = if (r.data) null else "رمز اشتباه است"
                        )
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(pinBuffer = "", isVerifying = false, error = r.error.message)
                }
            }
        }
    }

    // ═══ Setup / Change ═══
    fun setupPin(pin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val r = setupPin(pin)) {
                is AppResult.Success -> onDone()
                is AppResult.Failure -> _uiState.update { it.copy(error = r.error.message) }
            }
        }
    }

    fun changePin(oldPin: String, newPin: String, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val r = changePinUseCase(oldPin, newPin)) {
                is AppResult.Success -> onDone()
                is AppResult.Failure -> _uiState.update { it.copy(error = r.error.message) }
            }
        }
    }

    // ═══ Biometric ═══
    fun unlockBiometric() {
        viewModelScope.launch {
            when (val r = unlockWithBiometric()) {
                is AppResult.Success -> _uiState.update { it.copy(pinBuffer = "", error = null) }
                is AppResult.Failure -> _uiState.update { it.copy(error = r.error.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}