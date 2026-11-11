package ir.hasabdari.paydar.ui.treasury

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.domain.model.TreasuryOverview
import ir.hasabdari.paydar.domain.usecase.GetTreasuryOverviewUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TreasuryViewModel @Inject constructor(
    getOverview: GetTreasuryOverviewUseCase
) : ViewModel() {
    val overview: StateFlow<TreasuryOverview> = getOverview()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TreasuryOverview())
}