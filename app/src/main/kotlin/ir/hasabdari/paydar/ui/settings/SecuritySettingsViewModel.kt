package ir.hasabdari.paydar.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.domain.model.AuthConfig
import ir.hasabdari.paydar.domain.usecase.ObserveAuthConfigUseCase
import ir.hasabdari.paydar.domain.usecase.SetAutoLockMinutesUseCase
import ir.hasabdari.paydar.domain.usecase.SetBiometricEnabledUseCase
import ir.hasabdari.paydar.domain.usecase.SetPinEnabledUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    observeConfig: ObserveAuthConfigUseCase,
    private val setPinEnabled: SetPinEnabledUseCase,
    private val setBiometric: SetBiometricEnabledUseCase,
    private val setAutoLock: SetAutoLockMinutesUseCase
) : ViewModel() {

    val config: StateFlow<AuthConfig> = observeConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthConfig()
        )

    fun togglePin(enabled: Boolean) {
        viewModelScope.launch { setPinEnabled(enabled) }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { setBiometric(enabled) }
    }

    fun changeAutoLock(minutes: Int) {
        viewModelScope.launch { setAutoLock(minutes) }
    }
}