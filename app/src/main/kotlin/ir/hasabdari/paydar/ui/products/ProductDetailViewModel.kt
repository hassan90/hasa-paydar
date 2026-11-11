package ir.hasabdari.paydar.ui.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Product
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.model.WarehouseStock
import ir.hasabdari.paydar.domain.repository.ProductRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import ir.hasabdari.paydar.domain.usecase.AddStockMovementUseCase
import ir.hasabdari.paydar.domain.usecase.GetStockByProductUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val stockByWarehouse: List<WarehouseStock> = emptyList(),
    val warehouses: List<Warehouse> = emptyList(),
    val isLoading: Boolean = true,
    val actionMessage: String? = null
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepo: ProductRepository,
    private val warehouseRepo: WarehouseRepository,
    private val getStock: GetStockByProductUseCase,
    private val addMovement: AddStockMovementUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String = savedStateHandle["productId"] ?: ""

    private val _state = MutableStateFlow(ProductDetailUiState())
    val state: StateFlow<ProductDetailUiState> = _state.asStateFlow()

    init {
        loadProduct()
        observeStock()
        observeWarehouses()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            when (val r = productRepo.getById(productId)) {
                is AppResult.Success -> _state.update { it.copy(product = r.data, isLoading = false) }
                is AppResult.Failure -> _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun observeStock() {
        viewModelScope.launch {
            getStock(productId, AppConstants.DEFAULT_STORE_ID).collect { list ->
                _state.update { it.copy(stockByWarehouse = list) }
            }
        }
    }

    private fun observeWarehouses() {
        viewModelScope.launch {
            warehouseRepo.observeAll(AppConstants.DEFAULT_STORE_ID).collect { list ->
                _state.update { it.copy(warehouses = list) }
            }
        }
    }

    fun addStock(
        warehouseId: String,
        isIn: Boolean,
        quantity: Long,
        unitCostToman: Long
    ) {
        val deltaScaled = if (isIn) quantity * 1000 else -(quantity * 1000)
        val type = if (isIn) StockMovementType.ADJUST_IN else StockMovementType.ADJUST_OUT
        viewModelScope.launch {
            val r = addMovement(
                productId = productId,
                warehouseId = warehouseId,
                type = type,
                quantityDeltaScaled = deltaScaled,
                unitCostRials = unitCostToman * 10,
                note = if (isIn) "افزایش دستی موجودی" else "کاهش دستی موجودی"
            )
            when (r) {
                is AppResult.Success -> _state.update { it.copy(actionMessage = "موجودی با موفقیت به‌روزرسانی شد") }
                is AppResult.Failure -> _state.update { it.copy(actionMessage = r.error.message) }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(actionMessage = null) }
    }
}