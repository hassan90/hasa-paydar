package ir.hasabdari.paydar.ui.parties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.usecase.DeletePartyUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import ir.hasabdari.paydar.domain.usecase.SearchPartiesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PartyListUiState(
    val parties: List<Party> = emptyList(),
    val query: String = "",
    val kind: PartyKind = PartyKind.CUSTOMER,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PartyListViewModel @Inject constructor(
    private val getParties: GetPartiesUseCase,
    private val searchParties: SearchPartiesUseCase,
    private val deleteParty: DeletePartyUseCase
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val kindFlow = MutableStateFlow(PartyKind.CUSTOMER)

    val state: StateFlow<PartyListUiState> = combine(queryFlow, kindFlow) { query, kind -> query to kind }
        .flatMapLatest { (query, kind) ->
            val flow = if (query.isBlank()) getParties(kind = kind) else searchParties(query = query)
            flow.let { f ->
                combine(f, queryFlow, kindFlow) { list, q, k ->
                    PartyListUiState(parties = list, query = q, kind = k, isLoading = false)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartyListUiState())

    fun onQueryChange(q: String) { queryFlow.value = q }
    fun onKindChange(kind: PartyKind) { kindFlow.value = kind }

    fun delete(id: String) {
        viewModelScope.launch { deleteParty(id) }
    }
}