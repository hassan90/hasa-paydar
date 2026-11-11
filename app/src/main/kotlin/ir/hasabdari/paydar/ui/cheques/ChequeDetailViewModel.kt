package ir.hasabdari.paydar.ui.cheques

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.usecase.BounceChequeUseCase
import ir.hasabdari.paydar.domain.usecase.ClearChequeUseCase
import ir.hasabdari.paydar.domain.usecase.DepositChequeUseCase
import ir.hasabdari.paydar.domain.usecase.GetChequeDetailUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import ir.hasabdari.paydar.domain.usecase.ReturnChequeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChequeDetailUiState(
    val cheque: Cheque? = null,
    val partyName: String = "—",
    val isLoading: Boolean = true,
    val actionInProgress: Boolean = false,
    val actionMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class ChequeDetailViewModel @Inject constructor(
    private val getCheque: GetChequeDetailUseCase,
    private val getParties: GetPartiesUseCase,
    private val clearCheque: ClearChequeUseCase,
    private val bounceCheque: BounceChequeUseCase,
    private val depositCheque: DepositChequeUseCase,
    private val returnCheque: ReturnChequeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chequeId: String = savedStateHandle["chequeId"] ?: ""

    private val _state = MutableStateFlow(ChequeDetailUiState())
    val state: StateFlow<ChequeDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // ═══ استخراج داده‌ی non-null قبل از استفاده ═══
            val result = getCheque(chequeId)
            val cheque: Cheque? = if (result is AppResult.Success) result.data else null

            if (cheque == null) {
                _state.value = ChequeDetailUiState(
                    isLoading = false,
                    error = if (result is AppResult.Failure) result.error.message else "چک یافت نشد"
                )
                return@launch
            }

            // ═══ نام طرف حساب ═══
            val partyId = cheque.partyId
            val partyName = if (partyId != null) {
                val parties = getParties().first()
                parties.find { it.id == partyId }?.name ?: "—"
            } else "—"

            _state.value = ChequeDetailUiState(
                cheque = cheque,
                partyName = partyName,
                isLoading = false
            )
        }
    }

    fun clear() = performAction { clearCheque(chequeId) }
    fun bounce() = performAction { bounceCheque(chequeId) }
    fun deposit() = performAction { depositCheque(chequeId) }
    fun returnToCustomer() = performAction { returnCheque(chequeId) }

    fun clearMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }

    private fun performAction(action: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            _state.value = _state.value.copy(actionInProgress = true, actionMessage = null)
            when (val r = action()) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        actionInProgress = false,
                        actionMessage = "✓ عملیات موفق"
                    )
                    load()
                }
                is AppResult.Failure -> {
                    _state.value = _state.value.copy(
                        actionInProgress = false,
                        actionMessage = "⚠ ${r.error.message}"
                    )
                }
            }
        }
    }
}