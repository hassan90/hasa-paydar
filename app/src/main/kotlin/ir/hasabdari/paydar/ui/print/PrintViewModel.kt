package ir.hasabdari.paydar.ui.print

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.usecase.GetInvoiceDetailUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import ir.hasabdari.paydar.domain.usecase.GetProductsWithStockUseCase
import ir.hasabdari.paydar.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PrintUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PrintViewModel @Inject constructor(
    private val getInvoiceDetail: GetInvoiceDetailUseCase,
    private val getParties: GetPartiesUseCase,
    private val getProducts: GetProductsWithStockUseCase,
    private val getSettings: GetSettingsUseCase,
    private val printManager: InvoicePrintManager
) : ViewModel() {

    private val _ui = MutableStateFlow(PrintUiState())
    val ui = _ui.asStateFlow()

    fun printInvoice(invoiceId: String, activity: Activity) {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                // ─── ۱. فاکتور ───
                val detailResult = getInvoiceDetail(invoiceId)
                val detail = if (detailResult is AppResult.Success) detailResult.data else null
                if (detail == null) {
                    _ui.update {
                        it.copy(isLoading = false, error = "فاکتور یافت نشد")
                    }
                    return@launch
                }

                // ─── ۲. طرف حساب ───
                val parties = getParties().first()
                val party = parties.find { it.id == detail.invoice.partyId }

                // ─── ۳. نام کالاها ───
                val products = getProducts(AppConstants.DEFAULT_STORE_ID).first()
                val productNames = products.associate {
                    it.product.id to it.product.name
                }

                // ─── ۴. تنظیمات فروشگاه ───
                val settings = getSettings()

                // ─── ۵. ساخت HTML ───
                val html = InvoiceHtmlBuilder.build(
                    invoice = detail,
                    party = party,
                    productNames = productNames,
                    store = settings.storeInfo
                )

                _ui.update { it.copy(isLoading = false) }

                // ─── ۶. چاپ روی UI thread (نیاز Activity) ───
                withContext(Dispatchers.Main) {
                    printManager.print(
                        html = html,
                        jobName = "فاکتور ${detail.invoice.number}",
                        activity = activity
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        isLoading = false,
                        error = "خطا: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }
}