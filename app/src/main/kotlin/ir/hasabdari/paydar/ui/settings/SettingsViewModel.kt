package ir.hasabdari.paydar.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.backup.BackupManager
import ir.hasabdari.paydar.domain.model.AppSettings
import ir.hasabdari.paydar.domain.model.StoreInfo
import ir.hasabdari.paydar.domain.repository.SettingsRepository
import ir.hasabdari.paydar.domain.usecase.ObserveSettingsUseCase
import ir.hasabdari.paydar.domain.usecase.SetDarkModeUseCase
import ir.hasabdari.paydar.domain.usecase.SetDefaultTaxRateUseCase
import ir.hasabdari.paydar.domain.usecase.SetProModeUseCase
import ir.hasabdari.paydar.domain.usecase.UpdateStoreInfoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isEditingStore: Boolean = false,
    val storeDraft: StoreInfo? = null,
    val isSavingStore: Boolean = false,
    val isBackupInProgress: Boolean = false,
    val isRestoreInProgress: Boolean = false,
    val pendingRestoreUri: Uri? = null,
    val showRestoreConfirm: Boolean = false,
    val message: String? = null,
    val shouldRestart: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeSettings: ObserveSettingsUseCase,
    private val updateStoreInfo: UpdateStoreInfoUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val setProMode: SetProModeUseCase,
    private val setDefaultTaxRate: SetDefaultTaxRateUseCase,
    private val settingsRepo: SettingsRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    val settings: StateFlow<AppSettings> = observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ═══ ویرایش فروشگاه ═══
    fun startEditStore() {
        val current = settings.value.storeInfo
        _uiState.update { it.copy(isEditingStore = true, storeDraft = current) }
    }

    fun cancelEditStore() {
        _uiState.update { it.copy(isEditingStore = false, storeDraft = null) }
    }

    fun updateDraft(update: (StoreInfo) -> StoreInfo) {
        _uiState.update { it.copy(storeDraft = it.storeDraft?.let(update)) }
    }

    fun saveStore() {
        val draft = _uiState.value.storeDraft ?: return
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(message = "نام فروشگاه الزامی است") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingStore = true) }
            when (val r = updateStoreInfo(draft)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(isSavingStore = false, isEditingStore = false, storeDraft = null, message = "✓ ذخیره شد")
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isSavingStore = false, message = "⚠ ${r.error.message}")
                }
            }
        }
    }

    // ═══ تغییرات سریع ═══
    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { setDarkMode(enabled) }
    }

    fun toggleProMode(enabled: Boolean) {
        viewModelScope.launch { setProMode(enabled) }
    }

    fun changeTaxRate(basisPoints: Int) {
        viewModelScope.launch {
            setDefaultTaxRate(basisPoints)
            _uiState.update { it.copy(message = "✓ نرخ مالیات به‌روزرسانی شد") }
        }
    }

    // ═══ پشتیبان‌گیری ═══
    fun performBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupInProgress = true, message = null) }
            when (val r = backupManager.backupTo(uri)) {
                is AppResult.Success -> {
                    settingsRepo.recordBackupCompleted(r.data)
                    _uiState.update {
                        it.copy(isBackupInProgress = false, message = "✓ پشتیبان با موفقیت ذخیره شد")
                    }
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isBackupInProgress = false, message = "⚠ ${r.error.message}")
                }
            }
        }
    }

    fun requestRestore(uri: Uri) {
        _uiState.update { it.copy(pendingRestoreUri = uri, showRestoreConfirm = true) }
    }

    fun cancelRestore() {
        _uiState.update { it.copy(showRestoreConfirm = false, pendingRestoreUri = null) }
    }

    fun confirmRestore() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        _uiState.update { it.copy(showRestoreConfirm = false, isRestoreInProgress = true) }
        viewModelScope.launch {
            when (val r = backupManager.restoreFrom(uri)) {
                is AppResult.Success -> _uiState.update { it.copy(shouldRestart = true) }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isRestoreInProgress = false, pendingRestoreUri = null, message = "⚠ ${r.error.message}")
                }
            }
        }
    }

    fun doRestart() { backupManager.restartApp() }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }
}