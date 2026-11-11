package ir.hasabdari.paydar.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.usecase.GetSaleInvoicesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SaleListViewModel @Inject constructor(
    getSales: GetSaleInvoicesUseCase
) : ViewModel() {
    val invoices: StateFlow<List<Invoice>> = getSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}