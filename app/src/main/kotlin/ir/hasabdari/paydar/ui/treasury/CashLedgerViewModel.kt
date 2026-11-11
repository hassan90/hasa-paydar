package ir.hasabdari.paydar.ui.treasury

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashTransaction
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CashLedgerUiState(
    val account: CashAccount? = null,
    val transactions: List<CashTransaction> = emptyList(),
    val balanceRials: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class CashLedgerViewModel @Inject constructor(
    private val accountRepo: CashAccountRepository,
    private val txnRepo: CashTransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: String = savedStateHandle["accountId"] ?: ""

    private val _state = MutableStateFlow(CashLedgerUiState())
    val state: StateFlow<CashLedgerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val acc = (accountRepo.getById(accountId) as? AppResult.Success)?.data
            _state.value = _state.value.copy(account = acc)
            txnRepo.observeByAccount(accountId, "store_default").collect { list ->
                val bal = txnRepo.getBalance(accountId)
                _state.value = _state.value.copy(
                    transactions = list,
                    balanceRials = (bal as? AppResult.Success)?.data ?: 0L,
                    isLoading = false
                )
            }
        }
    }
}