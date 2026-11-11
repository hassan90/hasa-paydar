package ir.hasabdari.paydar.ui.returns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.InvoiceLine
import ir.hasabdari.paydar.domain.model.InvoiceType
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import ir.hasabdari.paydar.domain.model.NewReturnCommand
import ir.hasabdari.paydar.domain.model.NewReturnLine
import ir.hasabdari.paydar.domain.model.ReturnResult
import ir.hasabdari.paydar.domain.usecase.GetInvoiceDetailUseCase
import ir.hasabdari.paydar.domain.usecase.RegisterPurchaseReturnUseCase
import ir.hasabdari.paydar.domain.usecase.RegisterSaleReturnUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReturnLineInput(
    val originalLine: InvoiceLine,
    val productName: String = "",
    val quantityText: String = "0",
    val maxReturnableScaled: Long = 0L
) {
    val qtyScaled: Long get() = (quantityText.toLongOrNull() ?: 0) * 1000
    val maxReturnable: Long get() = maxReturnableScaled / 1000
}

data class AddReturnUiState(
    val originalInvoice: InvoiceWithLines? = null,
    val productNames: Map<String, String> = emptyMap(),
    val returnLines: List<ReturnLineInput> = emptyList(),
    val reason: String = "",
    val isLoading: Boolean = true,
    val submitting: Boolean = false,
    val error: String? = null,
    val success: ReturnResult? = null
) {
    val isSaleReturn: Boolean get() = originalInvoice?.invoice?.type == InvoiceType.SALE
    val totalRefundRials: Long
        get() = returnLines.sumOf { ln ->
            val ratio = if (ln.originalLine.quantityScaled > 0)
                ln.qtyScaled.toDouble() / ln.originalLine.quantityScaled.toDouble()
            else 0.0
            val net = (ln.originalLine.lineTotal.rials * ratio).toLong()
            val tax = (net * ln.originalLine.taxRate) / 10_000
            net + tax
        }
}

@HiltViewModel
class AddReturnViewModel @Inject constructor(
    private val getDetail: GetInvoiceDetailUseCase,                // ← private val اضافه شد
    private val registerSaleReturn: RegisterSaleReturnUseCase,
    private val registerPurchaseReturn: RegisterPurchaseReturnUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val originalInvoiceId: String = savedStateHandle["originalInvoiceId"] ?: ""

    private val _state = MutableStateFlow(AddReturnUiState())
    val state: StateFlow<AddReturnUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val r = getDetail(originalInvoiceId)) {
                is AppResult.Success -> {
                    val data = r.data
                    if (data == null) {
                        _state.value = _state.value.copy(isLoading = false, error = "فاکتور یافت نشد")
                    } else {
                        val lines = data.lines.map { line ->
                            ReturnLineInput(
                                originalLine = line,
                                productName = "",
                                quantityText = "0",
                                maxReturnableScaled = line.quantityScaled
                            )
                        }
                        _state.value = _state.value.copy(
                            originalInvoice = data,
                            returnLines = lines,
                            isLoading = false
                        )
                    }
                }
                is AppResult.Failure -> _state.value = _state.value.copy(isLoading = false, error = r.error.message)
            }
        }
    }

    fun onQuantityChange(idx: Int, v: String) {
        val digits = v.filter { it.isDigit() }
        val newLines = _state.value.returnLines.toMutableList()
        newLines[idx] = newLines[idx].copy(quantityText = digits)
        _state.value = _state.value.copy(returnLines = newLines)
    }

    fun onReasonChange(v: String) {
        _state.value = _state.value.copy(reason = v)
    }

    fun submit(onSuccess: (ReturnResult) -> Unit) {
        val s = _state.value
        val invoice = s.originalInvoice?.invoice ?: return
        val validLines = s.returnLines.filter { it.qtyScaled > 0 }
        if (validLines.isEmpty()) {
            _state.value = s.copy(error = "حداقل یک مقدار برگشتی وارد کنید")
            return
        }
        viewModelScope.launch {
            _state.value = s.copy(submitting = true, error = null)
            val cmd = NewReturnCommand(
                originalInvoiceId = invoice.id,
                returnDate = System.currentTimeMillis(),
                reason = s.reason.takeIf { it.isNotBlank() },
                lines = validLines.map { rl ->
                    NewReturnLine(
                        originalLineId = rl.originalLine.id,
                        productId = rl.originalLine.productId,
                        productName = rl.productName.ifBlank { rl.originalLine.productId.take(8) },
                        unitId = rl.originalLine.unitId,
                        quantityScaled = rl.qtyScaled,
                        unitPriceRials = rl.originalLine.unitPrice.rials
                    )
                },
                storeId = AppConstants.DEFAULT_STORE_ID
            )
            val result = if (s.isSaleReturn) registerSaleReturn(cmd) else registerPurchaseReturn(cmd)
            when (result) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(submitting = false, success = result.data)
                    onSuccess(result.data)
                }
                is AppResult.Failure -> {
                    _state.value = _state.value.copy(submitting = false, error = result.error.message)
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}