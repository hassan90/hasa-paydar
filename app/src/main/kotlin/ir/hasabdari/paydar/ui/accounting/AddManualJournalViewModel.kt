package ir.hasabdari.paydar.ui.accounting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Account
import ir.hasabdari.paydar.domain.model.JournalDraft
import ir.hasabdari.paydar.domain.model.JournalLineDraft
import ir.hasabdari.paydar.domain.model.JournalSourceType
import ir.hasabdari.paydar.domain.usecase.GetPostableAccountsUseCase
import ir.hasabdari.paydar.domain.usecase.PostJournalEntryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalLineInput(
    val accountCode: String = "",
    val debit: String = "",
    val credit: String = "",
    val description: String = ""
)

data class AddManualJournalState(
    val dateMillis: Long = System.currentTimeMillis(),
    val description: String = "",
    val lines: List<JournalLineInput> = listOf(JournalLineInput(), JournalLineInput()),
    val submitting: Boolean = false,
    val error: String? = null
) {
    val totalDebit: Long get() = lines.sumOf { (it.debit.toLongOrNull() ?: 0) * 10 }
    val totalCredit: Long get() = lines.sumOf { (it.credit.toLongOrNull() ?: 0) * 10 }
    val isBalanced: Boolean get() = totalDebit == totalCredit && totalDebit > 0
}

@HiltViewModel
class AddManualJournalViewModel @Inject constructor(
    getAccounts: GetPostableAccountsUseCase,
    private val postJournal: PostJournalEntryUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(AddManualJournalState())
        private set

    fun onDescriptionChange(v: String) { state = state.copy(description = v) }

    fun updateLine(idx: Int, update: (JournalLineInput) -> JournalLineInput) {
        val newLines = state.lines.toMutableList()
        newLines[idx] = update(newLines[idx])
        state = state.copy(lines = newLines)
    }

    fun addLine() { state = state.copy(lines = state.lines + JournalLineInput()) }
    fun removeLine(idx: Int) {
        if (state.lines.size > 2) {
            state = state.copy(lines = state.lines.filterIndexed { i, _ -> i != idx })
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        if (!s.isBalanced) { state = s.copy(error = "سند نامتوازن است"); return }
        if (s.description.isBlank()) { state = s.copy(error = "شرح سند الزامی است"); return }
        if (s.lines.any { it.accountCode.isBlank() }) { state = s.copy(error = "همه ردیف‌ها باید حساب داشته باشند"); return }

        viewModelScope.launch {
            state = state.copy(submitting = true, error = null)
            val draft = JournalDraft(
                entryDate = s.dateMillis,
                description = s.description.trim(),
                sourceType = JournalSourceType.MANUAL,
                sourceId = null,
                lines = s.lines.map {
                    JournalLineDraft(
                        accountCode = it.accountCode,
                        debit = (it.debit.toLongOrNull() ?: 0) * 10,
                        credit = (it.credit.toLongOrNull() ?: 0) * 10,
                        description = it.description.takeIf { d -> d.isNotBlank() }
                    )
                },
                storeId = AppConstants.DEFAULT_STORE_ID
            )
            when (val r = postJournal(draft)) {
                is AppResult.Success -> { state = AddManualJournalState(); onSuccess() }
                is AppResult.Failure -> state = state.copy(submitting = false, error = r.error.message)
            }
        }
    }
}