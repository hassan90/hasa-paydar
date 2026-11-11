package ir.hasabdari.paydar.ui.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.domain.model.JournalEntry
import ir.hasabdari.paydar.domain.usecase.GetJournalEntriesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class JournalListViewModel @Inject constructor(
    getEntries: GetJournalEntriesUseCase
) : ViewModel() {
    val entries: StateFlow<List<JournalEntry>> = getEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}