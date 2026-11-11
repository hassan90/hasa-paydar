package ir.hasabdari.paydar.ui.accounting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.JournalEntryWithLines
import ir.hasabdari.paydar.domain.usecase.GetJournalDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalDetailUiState(
    val entry: JournalEntryWithLines? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class JournalDetailViewModel @Inject constructor(
    getDetail: GetJournalDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val entryId: String = savedStateHandle["entryId"] ?: ""
    private val _state = MutableStateFlow(JournalDetailUiState())
    val state: StateFlow<JournalDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = getDetail(entryId)) {
                is AppResult.Success -> _state.value = JournalDetailUiState(entry = r.data, isLoading = false)
                is AppResult.Failure -> _state.value = JournalDetailUiState(isLoading = false)
            }
        }
    }
}