package ir.hasabdari.paydar.ui.treasury

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.domain.usecase.AddCashAccountUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCashAccountFormState(
    val kind: CashAccountKind = CashAccountKind.CASHBOX,
    val name: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val openingBalanceToman: String = "",
    val submitting: Boolean = false,
    val nameError: String? = null,
    val submitError: String? = null
)

@HiltViewModel
class AddCashAccountViewModel @Inject constructor(
    private val addAccount: AddCashAccountUseCase
) : ViewModel() {

    var state by mutableStateOf(AddCashAccountFormState())
        private set

    fun onKindChange(k: CashAccountKind) { state = state.copy(kind = k) }
    fun onNameChange(v: String) { state = state.copy(name = v, nameError = null) }
    fun onBankNameChange(v: String) { state = state.copy(bankName = v) }
    fun onAccountNumberChange(v: String) { state = state.copy(accountNumber = v.filter { it.isDigit() }) }
    fun onOpeningBalanceChange(v: String) { state = state.copy(openingBalanceToman = v.filter { it.isDigit() }) }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        val nameErr = if (s.name.isBlank()) "نام حساب الزامی است" else null
        if (nameErr != null) { state = s.copy(nameError = nameErr); return }

        viewModelScope.launch {
            state = state.copy(submitting = true, submitError = null)
            val result = addAccount(
                kind = s.kind,
                name = s.name,
                bankName = s.bankName.takeIf { it.isNotBlank() },
                accountNumber = s.accountNumber.takeIf { it.isNotBlank() },
                openingBalanceRials = (s.openingBalanceToman.toLongOrNull() ?: 0) * 10
            )
            when (result) {
                is AppResult.Success -> { state = AddCashAccountFormState(); onSuccess() }
                is AppResult.Failure -> state = state.copy(submitting = false, submitError = result.error.message)
            }
        }
    }
}