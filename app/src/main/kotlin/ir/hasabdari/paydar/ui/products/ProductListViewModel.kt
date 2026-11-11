package ir.hasabdari.paydar.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.usecase.EnsureDefaultWarehouseUseCase
import ir.hasabdari.paydar.domain.usecase.GetProductsWithStockUseCase
import ir.hasabdari.paydar.domain.usecase.SearchProductsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<ProductWithStock> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductListViewModel @Inject constructor(
    getProducts: GetProductsWithStockUseCase,
    private val searchProducts: SearchProductsUseCase,
    ensureDefaultWarehouse: EnsureDefaultWarehouseUseCase
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val state: StateFlow<ProductListUiState> = queryFlow
        .flatMapLatest { query ->
            val flow = if (query.isBlank()) {
                getProducts(AppConstants.DEFAULT_STORE_ID)
            } else {
                searchProducts(AppConstants.DEFAULT_STORE_ID, query)
            }
            flow.map { ProductListUiState(products = it, query = query, isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    init {
        viewModelScope.launch { ensureDefaultWarehouse() }
    }

    fun onQueryChange(newQuery: String) {
        queryFlow.value = newQuery
    }
}