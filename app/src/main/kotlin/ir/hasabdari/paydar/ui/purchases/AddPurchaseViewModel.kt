package ir.hasabdari.paydar.ui.purchases

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.NewPurchaseCommand
import ir.hasabdari.paydar.domain.model.NewPurchaseLine
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import ir.hasabdari.paydar.domain.usecase.GetProductsWithStockUseCase
import ir.hasabdari.paydar.domain.usecase.GetWarehousesUseCase
import ir.hasabdari.paydar.domain.usecase.RegisterPurchaseUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PurchaseLineInput(
    val productId: String = "",
    val productName: String = "",
    val unitId: String = "unit_piece",
    val quantityText: String = "",
    val priceText: String = "",
    val discountText: String = ""
)

data class AddPurchaseFormState(
    val partyId: String? = null,
    val partyName: String = "",
    val warehouseId: String = "",
    val warehouseName: String = "",
    val settlementType: SettlementType = SettlementType.CREDIT,
    val paidAmountText: String = "",
    val discountTotalText: String = "",
    val shippingText: String = "",
    val otherCostsText: String = "",
    val notes: String = "",
    val lines: List<PurchaseLineInput> = listOf(PurchaseLineInput()),
    val submitting: Boolean = false,
    val error: String? = null
) {
    val subtotalRials: Long get() = lines.sumOf { ln ->
        val qtyScaled = (ln.quantityText.toLongOrNull() ?: 0) * 1000
        val priceRials = (ln.priceText.toLongOrNull() ?: 0) * 10
        (priceRials * qtyScaled) / 1000
    }
    val lineDiscountsRials: Long get() = lines.sumOf { ln ->
        (ln.discountText.toLongOrNull() ?: 0) * 10
    }
    val invoiceDiscountRials: Long get() = (discountTotalText.toLongOrNull() ?: 0) * 10
    val shippingRials: Long get() = (shippingText.toLongOrNull() ?: 0) * 10
    val otherRials: Long get() = (otherCostsText.toLongOrNull() ?: 0) * 10
    val taxRials: Long get() = 0L  // فعلاً بدون مالیات
    val grandTotalRials: Long get() =
        subtotalRials - invoiceDiscountRials + taxRials + shippingRials + otherRials
    val paidRials: Long get() = (paidAmountText.toLongOrNull() ?: 0) * 10
}

@HiltViewModel
class AddPurchaseViewModel @Inject constructor(
    getSuppliers: GetPartiesUseCase,
    getWarehouses: GetWarehousesUseCase,
    getProducts: GetProductsWithStockUseCase,
    private val registerPurchase: RegisterPurchaseUseCase
) : ViewModel() {

    val suppliers: StateFlow<List<Party>> = getSuppliers(kind = PartyKind.SUPPLIER)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val warehouses: StateFlow<List<Warehouse>> = getWarehouses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<ProductWithStock>> = getProducts(AppConstants.DEFAULT_STORE_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(AddPurchaseFormState())
        private set

    fun onPartySelected(party: Party) {
        state = state.copy(partyId = party.id, partyName = party.name)
    }

    fun onWarehouseSelected(wh: Warehouse) {
        state = state.copy(warehouseId = wh.id, warehouseName = wh.name)
    }

    fun onSettlementTypeChange(t: SettlementType) {
        state = state.copy(
            settlementType = t,
            paidAmountText = when (t) {
                SettlementType.CREDIT -> ""
                SettlementType.CASH -> state.grandTotalRials.let { (it / 10).toString() }
                SettlementType.MIXED -> state.paidAmountText
            }
        )
    }

    fun onPaidAmountChange(v: String) { state = state.copy(paidAmountText = v.filter { it.isDigit() }) }
    fun onDiscountTotalChange(v: String) { state = state.copy(discountTotalText = v.filter { it.isDigit() }) }
    fun onShippingChange(v: String) { state = state.copy(shippingText = v.filter { it.isDigit() }) }
    fun onOtherCostsChange(v: String) { state = state.copy(otherCostsText = v.filter { it.isDigit() }) }
    fun onNotesChange(v: String) { state = state.copy(notes = v) }

    fun updateLine(idx: Int, update: (PurchaseLineInput) -> PurchaseLineInput) {
        val newLines = state.lines.toMutableList()
        newLines[idx] = update(newLines[idx])
        state = state.copy(lines = newLines)
    }

    fun addLine() { state = state.copy(lines = state.lines + PurchaseLineInput()) }
    fun removeLine(idx: Int) {
        if (state.lines.size > 1) {
            state = state.copy(lines = state.lines.filterIndexed { i, _ -> i != idx })
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        if (s.lines.isEmpty() || s.lines.all { it.productId.isBlank() }) {
            state = s.copy(error = "حداقل یک کالا انتخاب کنید"); return
        }
        if (s.warehouseId.isBlank()) {
            state = s.copy(error = "انبار را انتخاب کنید"); return
        }
        if (s.settlementType != SettlementType.CASH && s.partyId.isNullOrBlank()) {
            state = s.copy(error = "برای خرید نسیه، تأمین‌کننده الزامی است"); return
        }
        val validLines = s.lines.filter { it.productId.isNotBlank() && (it.quantityText.toLongOrNull() ?: 0) > 0 }
        if (validLines.isEmpty()) {
            state = s.copy(error = "هر کالا باید تعداد مثبت داشته باشد"); return
        }

        viewModelScope.launch {
            state = state.copy(submitting = true, error = null)
            val cmd = NewPurchaseCommand(
                issueDate = System.currentTimeMillis(),
                partyId = s.partyId,
                warehouseId = s.warehouseId,
                settlementType = s.settlementType,
                paidAmountRials = when (s.settlementType) {
                    SettlementType.CASH -> s.grandTotalRials
                    SettlementType.CREDIT -> 0L
                    SettlementType.MIXED -> s.paidRials
                },
                discountTotalRials = s.invoiceDiscountRials,
                shippingCostRials = s.shippingRials,
                otherCostsRials = s.otherRials,
                notes = s.notes.takeIf { it.isNotBlank() },
                lines = validLines.map { ln ->
                    NewPurchaseLine(
                        productId = ln.productId,
                        productName = ln.productName,
                        unitId = ln.unitId,
                        quantityScaled = (ln.quantityText.toLongOrNull() ?: 0) * 1000,
                        unitPriceRials = (ln.priceText.toLongOrNull() ?: 0) * 10,
                        discountRials = (ln.discountText.toLongOrNull() ?: 0) * 10,
                        taxRate = 0
                    )
                },
                storeId = AppConstants.DEFAULT_STORE_ID
            )
            when (val result = registerPurchase(cmd)) {
                is AppResult.Success -> { state = AddPurchaseFormState(); onSuccess() }
                is AppResult.Failure -> state = state.copy(submitting = false, error = result.error.message)
            }
        }
    }
}