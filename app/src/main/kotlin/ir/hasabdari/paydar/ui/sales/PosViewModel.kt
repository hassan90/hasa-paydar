package ir.hasabdari.paydar.ui.sales

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.NewSaleCommand
import ir.hasabdari.paydar.domain.model.NewSaleLine
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.model.SaleResult
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.usecase.EnsureDefaultWarehouseUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartiesUseCase
import ir.hasabdari.paydar.domain.usecase.GetProductsWithStockUseCase
import ir.hasabdari.paydar.domain.usecase.GetWarehousesUseCase
import ir.hasabdari.paydar.domain.usecase.RegisterSaleUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CartLine(
    val productId: String,
    val name: String,
    val unitId: String,
    val quantity: Long,
    val unitPriceRials: Long,
    val maxAvailable: Long
) {
    val lineTotalRials: Long get() = unitPriceRials * quantity
}

data class PosState(
    val cart: List<CartLine> = emptyList(),
    val query: String = "",
    val partyId: String? = null,
    val partyName: String = "",
    val settlementType: SettlementType = SettlementType.CASH,
    val receivedText: String = "",
    val discountText: String = "",
    val submitting: Boolean = false,
    val error: String? = null,
    val lastSale: SaleResult? = null
) {
    val subtotalRials: Long get() = cart.sumOf { it.lineTotalRials }
    val discountRials: Long get() = (discountText.toLongOrNull() ?: 0) * 10
    val grandTotalRials: Long get() = (subtotalRials - discountRials).coerceAtLeast(0)
    val receivedRials: Long get() = (receivedText.toLongOrNull() ?: 0) * 10
    val changeRials: Long get() = (receivedRials - grandTotalRials).coerceAtLeast(0)
    val remainingRials: Long get() = (grandTotalRials - receivedRials).coerceAtLeast(0)
}

@HiltViewModel
class PosViewModel @Inject constructor(
    getProducts: GetProductsWithStockUseCase,
    getWarehouses: GetWarehousesUseCase,
    getCustomers: GetPartiesUseCase,
    private val ensureWarehouse: EnsureDefaultWarehouseUseCase,
    private val registerSale: RegisterSaleUseCase
) : ViewModel() {

    val products: StateFlow<List<ProductWithStock>> = getProducts(AppConstants.DEFAULT_STORE_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val warehouses: StateFlow<List<Warehouse>> = getWarehouses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<Party>> = getCustomers(kind = PartyKind.CUSTOMER)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var state by mutableStateOf(PosState())
        private set

    init {
        // اطمینان از وجود انبار پیش‌فرض — همان لحظه‌ی باز شدن POS
        viewModelScope.launch {
            runCatching {
                when (val r = ensureWarehouse()) {
                    is AppResult.Success -> Timber.i("POS: default warehouse ready (${r.data.name})")
                    is AppResult.Failure -> Timber.w("POS: ensureWarehouse failed: ${r.error.message}")
                }
            }.onFailure { Timber.e(it, "POS: ensureWarehouse threw") }
        }
    }

    fun onQueryChange(q: String) { state = state.copy(query = q) }

    fun addProduct(p: ProductWithStock) {
        Timber.d("POS: addProduct ${p.product.name} (stock=${p.totalStockScaled}, salePrice=${p.product.salePrice.rials})")

        if (p.totalStockScaled <= 0) {
            state = state.copy(error = "«${p.product.name}» ناموجود است")
            return
        }
        if (p.product.salePrice.rials <= 0) {
            state = state.copy(error = "قیمت فروش «${p.product.name}» صفر است — ابتدا در کالاها اصلاح کنید")
            return
        }
        val existing = state.cart.find { it.productId == p.product.id }
        if (existing != null) {
            val newQty = existing.quantity + 1
            if (newQty > p.totalStockScaled) {
                state = state.copy(error = "موجودی «${p.product.name}» کافی نیست")
                return
            }
            state = state.copy(cart = state.cart.map {
                if (it.productId == p.product.id) it.copy(quantity = newQty) else it
            }, error = null)
        } else {
            state = state.copy(
                cart = state.cart + CartLine(
                    productId = p.product.id,
                    name = p.product.name,
                    unitId = p.product.unitId,
                    quantity = 1,
                    unitPriceRials = p.product.salePrice.rials,
                    maxAvailable = p.totalStockScaled
                ),
                error = null
            )
        }
    }

    fun incrementLine(productId: String) {
        state = state.copy(cart = state.cart.map {
            if (it.productId == productId && it.quantity < it.maxAvailable)
                it.copy(quantity = it.quantity + 1) else it
        })
    }

    fun decrementLine(productId: String) {
        state = state.copy(cart = state.cart.mapNotNull {
            if (it.productId == productId) {
                if (it.quantity <= 1) null else it.copy(quantity = it.quantity - 1)
            } else it
        })
    }

    fun removeLine(productId: String) {
        state = state.copy(cart = state.cart.filterNot { it.productId == productId })
    }

    fun selectCustomer(p: Party?) {
        state = state.copy(partyId = p?.id, partyName = p?.name ?: "")
    }

    fun onSettlementTypeChange(t: SettlementType) {
        state = state.copy(
            settlementType = t,
            receivedText = when (t) {
                SettlementType.CASH -> (state.grandTotalRials / 10).toString()
                SettlementType.CREDIT -> ""
                SettlementType.MIXED -> state.receivedText
            }
        )
    }

    fun onReceivedChange(v: String) {
        state = state.copy(receivedText = v.filter { it.isDigit() })
    }

    fun onDiscountChange(v: String) {
        state = state.copy(discountText = v.filter { it.isDigit() })
    }

    fun clearError() { state = state.copy(error = null) }
    fun clearLastSale() { state = PosState() }

    fun submitSale() {
        val s = state
        Timber.d("POS: submitSale cart=${s.cart.size}, total=${s.grandTotalRials}, settlement=${s.settlementType}")

        // ─── اعتبارسنجی UI ───
        if (s.cart.isEmpty()) {
            state = s.copy(error = "سبد خرید خالی است")
            return
        }
        if (s.grandTotalRials <= 0) {
            state = s.copy(error = "جمع فاکتور باید بیشتر از صفر باشد (قیمت فروش کالاها را بررسی کنید)")
            return
        }
        if (s.settlementType != SettlementType.CASH && s.partyId == null) {
            state = s.copy(error = "برای فروش نسیه، مشتری انتخاب کنید")
            return
        }
        if (s.settlementType == SettlementType.MIXED && s.receivedRials <= 0) {
            state = s.copy(error = "برای فروش ترکیبی، مبلغ دریافتی الزامی است")
            return
        }

        viewModelScope.launch {
            state = state.copy(submitting = true, error = null)

            // ─── اطمینان از انبار (مستقیم از UseCase، نه از flow) ───
            val warehouseId = try {
                when (val wh = ensureWarehouse()) {
                    is AppResult.Success -> wh.data.id
                    is AppResult.Failure -> {
                        val msg = "خطا در ایجاد انبار: ${wh.error.message}"
                        Timber.e(msg)
                        state = state.copy(submitting = false, error = msg)
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "ensureWarehouse threw")
                state = state.copy(submitting = false, error = "خطا در آماده‌سازی: ${e.message}")
                return@launch
            }

            val cmd = NewSaleCommand(
                issueDate = System.currentTimeMillis(),
                partyId = s.partyId,
                warehouseId = warehouseId,
                settlementType = s.settlementType,
                paidAmountRials = when (s.settlementType) {
                    SettlementType.CASH -> s.grandTotalRials
                    SettlementType.CREDIT -> 0L
                    SettlementType.MIXED -> s.receivedRials
                },
                discountTotalRials = s.discountRials,
                taxRate = 0,
                notes = null,
                lines = s.cart.map { ln ->
                    NewSaleLine(
                        productId = ln.productId,
                        productName = ln.name,
                        unitId = ln.unitId,
                        quantityScaled = ln.quantity * 1000,
                        unitPriceRials = ln.unitPriceRials,
                        discountRials = 0,
                        costAtSaleRials = 0
                    )
                },
                storeId = AppConstants.DEFAULT_STORE_ID
            )

            try {
                when (val result = registerSale(cmd)) {
                    is AppResult.Success -> {
                        Timber.i("POS: sale success ${result.data.invoiceNumber} total=${result.data.grandTotalRials}")
                        state = state.copy(submitting = false, lastSale = result.data)
                    }
                    is AppResult.Failure -> {
                        Timber.w("POS: sale failed: ${result.error.message}")
                        state = state.copy(submitting = false, error = result.error.message)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "POS: registerSale threw")
                state = state.copy(submitting = false, error = "خطای غیرمنتظره: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }
}