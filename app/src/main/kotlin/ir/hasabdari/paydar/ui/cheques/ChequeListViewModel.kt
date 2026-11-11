package ir.hasabdari.paydar.ui.cheques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.usecase.GetChequesUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChequeWithParty(
    val cheque: Cheque,
    val partyName: String
)

data class ChequeListUiState(
    val items: List<ChequeWithParty> = emptyList(),
    val filter: ChequeKind? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChequeListViewModel @Inject constructor(
    private val getCheques: GetChequesUseCase,
    getParties: GetPartiesUseCase
) : ViewModel() {

    private val filterFlow = MutableStateFlow<ChequeKind?>(null)

    private val partyNamesFlow = getParties()
        .map { list -> list.associate { it.id to it.name } }

    private val filteredChequesFlow = filterFlow
        .flatMapLatest { filter ->
            getCheques(kind = filter).map { cheques -> cheques to filter }
        }

    val state: StateFlow<ChequeListUiState> = combine(
        filteredChequesFlow,
        partyNamesFlow
    ) { (cheques, filter), partyMap ->
        ChequeListUiState(
            items = cheques.map { c ->
                ChequeWithParty(
                    cheque = c,
                    partyName = c.partyId?.let { partyMap[it] } ?: "—"
                )
            },
            filter = filter,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChequeListUiState())

    fun onFilterChange(kind: ChequeKind?) {
        filterFlow.value = kind
    }
}