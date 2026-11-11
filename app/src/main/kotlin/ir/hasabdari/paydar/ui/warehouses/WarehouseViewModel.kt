package ir.hasabdari.paydar.ui.warehouses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.usecase.AddWarehouseUseCase
import ir.hasabdari.paydar.domain.usecase.GetWarehousesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WarehouseUiState(
    val submitting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class WarehouseViewModel @Inject constructor(
    getWarehouses: GetWarehousesUseCase,
    private val addWarehouse: AddWarehouseUseCase
) : ViewModel() {

    val warehouses: StateFlow<List<Warehouse>> = getWarehouses()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(WarehouseUiState())
    val uiState: StateFlow<WarehouseUiState> = _uiState.asStateFlow()

    fun add(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(submitting = true) }
            when (val r = addWarehouse(name.trim())) {
                is AppResult.Success ->
                    _uiState.value = WarehouseUiState(message = "انبار «$name» افزوده شد")
                is AppResult.Failure ->
                    _uiState.value = WarehouseUiState(message = r.error.message)
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}