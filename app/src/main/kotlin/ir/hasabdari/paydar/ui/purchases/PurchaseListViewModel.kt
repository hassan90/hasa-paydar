package ir.hasabdari.paydar.ui.purchases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.usecase.GetPurchaseInvoicesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PurchaseListViewModel @Inject constructor(
    getPurchases: GetPurchaseInvoicesUseCase
) : ViewModel() {
    val invoices: StateFlow<List<Invoice>> = getPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}