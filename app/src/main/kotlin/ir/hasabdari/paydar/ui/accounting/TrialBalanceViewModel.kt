package ir.hasabdari.paydar.ui.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.TrialBalance
import ir.hasabdari.paydar.domain.usecase.GetTrialBalanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrialBalanceViewModel @Inject constructor(
    private val getTrialBalance: GetTrialBalanceUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<TrialBalance?>(null)
    val state: StateFlow<TrialBalance?> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val yearAgo = now - 365L * 24 * 60 * 60 * 1000
            when (val r = getTrialBalance(yearAgo, now)) {
                is AppResult.Success -> _state.value = r.data
                else -> {}
            }
        }
    }
}