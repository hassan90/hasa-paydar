package ir.hasabdari.paydar.ui.treasury

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashTransactionMethod
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.usecase.AddCashTransactionUseCase
import ir.hasabdari.paydar.domain.usecase.GetCashAccountsUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionFormState(
    val type: CashTransactionType = CashTransactionType.RECEIPT,
    val method: CashTransactionMethod = CashTransactionMethod.CASH,
    val cashAccountId: String = "",
    val toCashAccountId: String = "",
    val partyId: String? = null,
    val amountToman: String = "",
    val description: String = "",
    val submitting: Boolean = false,
    val submitError: String? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    getAccounts: GetCashAccountsUseCase,
    getParties: GetPartiesUseCase,
    private val addTxn: AddCashTransactionUseCase
) : ViewModel() {

    val accounts: StateFlow<List<CashAccount>> = getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<Party>> = getParties(kind = PartyKind.CUSTOMER)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val suppliers: StateFlow<List<Party>> = getParties(kind = PartyKind.SUPPLIER)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(AddTransactionFormState())
        private set

    fun initType(type: CashTransactionType) { state = state.copy(type = type) }
    fun onTypeChange(t: CashTransactionType) { state = state.copy(type = t) }
    fun onMethodChange(m: CashTransactionMethod) { state = state.copy(method = m) }
    fun onCashAccountChange(id: String) { state = state.copy(cashAccountId = id) }
    fun onToCashAccountChange(id: String) { state = state.copy(toCashAccountId = id) }
    fun onPartyChange(id: String?) { state = state.copy(partyId = id) }
    fun onAmountChange(v: String) { state = state.copy(amountToman = v.filter { it.isDigit() }) }
    fun onDescriptionChange(v: String) { state = state.copy(description = v) }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        val amount = (s.amountToman.toLongOrNull() ?: 0) * 10
        if (amount <= 0) { state = s.copy(submitError = "مبلغ باید بزرگ‌تر از صفر باشد"); return }
        if (s.cashAccountId.isBlank()) { state = s.copy(submitError = "حساب را انتخاب کنید"); return }
        if (s.type == CashTransactionType.TRANSFER && s.toCashAccountId.isBlank()) {
            state = s.copy(submitError = "حساب مقصد را انتخاب کنید"); return
        }

        viewModelScope.launch {
            state = state.copy(submitting = true, submitError = null)
            val result = addTxn(
                type = s.type,
                method = s.method,
                cashAccountId = s.cashAccountId,
                toCashAccountId = s.toCashAccountId.takeIf { it.isNotBlank() },
                partyId = s.partyId,
                amountRials = amount,
                description = s.description.takeIf { it.isNotBlank() }
            )
            when (result) {
                is AppResult.Success -> { state = AddTransactionFormState(); onSuccess() }
                is AppResult.Failure -> state = state.copy(submitting = false, submitError = result.error.message)
            }
        }
    }
}