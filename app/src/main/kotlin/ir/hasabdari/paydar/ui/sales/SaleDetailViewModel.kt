package ir.hasabdari.paydar.ui.sales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import ir.hasabdari.paydar.domain.usecase.GetSaleDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SaleDetailUiState(
    val data: InvoiceWithLines? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SaleDetailViewModel @Inject constructor(
    getDetail: GetSaleDetailUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val invoiceId: String = savedStateHandle["invoiceId"] ?: ""
    private val _state = MutableStateFlow(SaleDetailUiState())
    val state: StateFlow<SaleDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = getDetail(invoiceId)) {
                is AppResult.Success -> _state.value = SaleDetailUiState(data = r.data, isLoading = false)
                is AppResult.Failure -> _state.value = SaleDetailUiState(isLoading = false)
            }
        }
    }
}