package ir.hasabdari.paydar.ui.cheques

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.usecase.AddChequeUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddChequeFormState(
    val kind: ChequeKind = ChequeKind.RECEIVED,
    val number: String = "",
    val amountText: String = "",
    val partyId: String? = null,
    val partyName: String = "",
    val dueInDays: Int = 30,
    val bankName: String = "",
    val branch: String = "",
    val accountNumber: String = "",
    val note: String = "",
    val submitting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddChequeViewModel @Inject constructor(
    getParties: GetPartiesUseCase,
    private val addCheque: AddChequeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialKind: ChequeKind = runCatching {
        ChequeKind.valueOf(savedStateHandle["kind"] ?: "RECEIVED")
    }.getOrDefault(ChequeKind.RECEIVED)

    val parties: StateFlow<List<Party>> = getParties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(AddChequeFormState(kind = initialKind))
        private set

    fun onKindChange(k: ChequeKind) {
        if (k != state.kind) {
            state = state.copy(kind = k, partyId = null, partyName = "")
        }
    }
    fun onNumberChange(v: String) { state = state.copy(number = v) }
    fun onAmountChange(v: String) { state = state.copy(amountText = v.filter { it.isDigit() }) }
    fun onPartyChange(p: Party) { state = state.copy(partyId = p.id, partyName = p.name) }
    fun onDueInDaysChange(d: Int) { state = state.copy(dueInDays = d.coerceIn(1, 365)) }
    fun onBankNameChange(v: String) { state = state.copy(bankName = v) }
    fun onBranchChange(v: String) { state = state.copy(branch = v) }
    fun onAccountNumberChange(v: String) { state = state.copy(accountNumber = v.filter { it.isDigit() }) }
    fun onNoteChange(v: String) { state = state.copy(note = v) }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        if (s.number.isBlank()) { state = s.copy(error = "شماره چک الزامی است"); return }
        val amountRials = (s.amountText.toLongOrNull() ?: 0) * 10
        if (amountRials <= 0) { state = s.copy(error = "مبلغ چک الزامی است"); return }
        if (s.partyId.isNullOrBlank()) { state = s.copy(error = "طرف حساب را انتخاب کنید"); return }

        viewModelScope.launch {
            state = state.copy(submitting = true, error = null)
            val now = System.currentTimeMillis()
            val due = now + s.dueInDays.toLong() * 86_400_000L
            val result = addCheque(
                kind = s.kind,
                number = s.number.trim(),
                amountRials = amountRials,
                issueDate = now,
                dueDate = due,
                bankName = s.bankName.takeIf { it.isNotBlank() },
                branch = s.branch.takeIf { it.isNotBlank() },
                accountNumber = s.accountNumber.takeIf { it.isNotBlank() },
                partyId = s.partyId,
                note = s.note.takeIf { it.isNotBlank() }
            )
            when (result) {
                is AppResult.Success -> { state = AddChequeFormState(); onSuccess() }
                is AppResult.Failure -> state = state.copy(submitting = false, error = result.error.message)
            }
        }
    }
}