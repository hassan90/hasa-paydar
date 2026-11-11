package ir.hasabdari.paydar.ui.products

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.usecase.AddProductUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddProductFormState(
    val name: String = "",
    val code: String = "",
    val barcode: String = "",
    val purchasePriceToman: String = "",
    val salePriceToman: String = "",
    val minStock: String = "0",
    val initialStock: String = "0",
    val notes: String = "",
    val nameError: String? = null,
    val codeError: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null
)

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val addProduct: AddProductUseCase
) : ViewModel() {

    var state by mutableStateOf(AddProductFormState())
        private set

    fun onNameChange(v: String) { state = state.copy(name = v, nameError = null) }
    fun onCodeChange(v: String) { state = state.copy(code = v, codeError = null) }
    fun onBarcodeChange(v: String) { state = state.copy(barcode = v) }
    fun onPurchasePriceChange(v: String) { state = state.copy(purchasePriceToman = v.filterDigits()) }
    fun onSalePriceChange(v: String) { state = state.copy(salePriceToman = v.filterDigits()) }
    fun onMinStockChange(v: String) { state = state.copy(minStock = v.filterDigits()) }
    fun onInitialStockChange(v: String) { state = state.copy(initialStock = v.filterDigits()) }
    fun onNotesChange(v: String) { state = state.copy(notes = v) }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        val nameError = if (s.name.isBlank()) "نام کالا الزامی است" else null
        val codeError = if (s.code.isBlank()) "کد کالا الزامی است" else null
        if (nameError != null || codeError != null) {
            state = s.copy(nameError = nameError, codeError = codeError)
            return
        }

        viewModelScope.launch {
            state = state.copy(submitting = true, submitError = null)
            val result = addProduct(
                name = s.name.trim(),
                code = s.code.trim(),
                barcode = s.barcode.trim().takeIf { it.isNotBlank() },
                purchasePriceRials = (s.purchasePriceToman.toLongOrNull() ?: 0) * 10,
                salePriceRials = (s.salePriceToman.toLongOrNull() ?: 0) * 10,
                minStockScaled = (s.minStock.toLongOrNull() ?: 0) * 1000,
                initialStockScaled = (s.initialStock.toLongOrNull() ?: 0) * 1000,
                notes = s.notes.trim().takeIf { it.isNotBlank() }
            )
            when (result) {
                is AppResult.Success -> {
                    state = AddProductFormState()
                    onSuccess()
                }
                is AppResult.Failure -> {
                    state = state.copy(submitting = false, submitError = result.error.message)
                }
            }
        }
    }
}

private fun String.filterDigits(): String = filter { it.isDigit() }