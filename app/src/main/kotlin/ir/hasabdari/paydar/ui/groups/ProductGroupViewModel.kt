package ir.hasabdari.paydar.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.ProductGroupNode
import ir.hasabdari.paydar.domain.usecase.AddProductGroupUseCase
import ir.hasabdari.paydar.domain.usecase.DeleteProductGroupUseCase
import ir.hasabdari.paydar.domain.usecase.GetProductGroupTreeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductGroupUiState(val message: String? = null)

@HiltViewModel
class ProductGroupViewModel @Inject constructor(
    getTree: GetProductGroupTreeUseCase,
    private val addGroup: AddProductGroupUseCase,
    private val deleteGroup: DeleteProductGroupUseCase
) : ViewModel() {

    val tree: StateFlow<List<ProductGroupNode>> = getTree()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ProductGroupUiState())
    val uiState: StateFlow<ProductGroupUiState> = _uiState.asStateFlow()

    fun add(name: String, parentId: String?) {
        viewModelScope.launch {
            when (val r = addGroup(name = name, parentId = parentId)) {
                is AppResult.Success -> _uiState.value = ProductGroupUiState(message = "گروه «$name» افزوده شد")
                is AppResult.Failure -> _uiState.value = ProductGroupUiState(message = r.error.message)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteGroup(id) }
    }

    fun clearMessage() { _uiState.update { it.copy(message = null) } }
}