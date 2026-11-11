package ir.hasabdari.paydar.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.BalanceSheet
import ir.hasabdari.paydar.domain.model.IncomeStatement
import ir.hasabdari.paydar.domain.model.InventoryValueReport
import ir.hasabdari.paydar.domain.model.PartyBalanceReport
import ir.hasabdari.paydar.domain.model.SalesReport
import ir.hasabdari.paydar.domain.usecase.GetBalanceSheetUseCase
import ir.hasabdari.paydar.domain.usecase.GetIncomeStatementUseCase
import ir.hasabdari.paydar.domain.usecase.GetInventoryValueReportUseCase
import ir.hasabdari.paydar.domain.usecase.GetPartyBalancesUseCase
import ir.hasabdari.paydar.domain.usecase.GetSalesReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReportPeriod {
    TODAY, LAST_7_DAYS, LAST_30_DAYS, THIS_MONTH, ALL
}

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.LAST_30_DAYS,
    val incomeStatement: IncomeStatement? = null,
    val balanceSheet: BalanceSheet? = null,
    val salesReport: SalesReport? = null,
    val partyBalances: PartyBalanceReport? = null,
    val inventoryReport: InventoryValueReport? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getIncomeStatement: GetIncomeStatementUseCase,
    private val getBalanceSheet: GetBalanceSheetUseCase,
    private val getSalesReport: GetSalesReportUseCase,
    private val getPartyBalances: GetPartyBalancesUseCase,
    private val getInventoryReport: GetInventoryValueReportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init { loadAll() }

    fun onPeriodChange(p: ReportPeriod) {
        _state.update { it.copy(period = p) }
        loadAll()
    }

    fun refresh() = loadAll()

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val (from, to) = periodRange(_state.value.period)
            val now = System.currentTimeMillis()

            val inc = getIncomeStatement(from, to)
            val bal = getBalanceSheet(now)
            val sales = getSalesReport(from, to)
            val parties = getPartyBalances()
            val inv = getInventoryReport()

            var error: String? = null
            (inc as? AppResult.Failure)?.let { error = it.error.message }
            if (error == null) (bal as? AppResult.Failure)?.let { error = it.error.message }
            if (error == null) (sales as? AppResult.Failure)?.let { error = it.error.message }
            if (error == null) (parties as? AppResult.Failure)?.let { error = it.error.message }
            if (error == null) (inv as? AppResult.Failure)?.let { error = it.error.message }

            _state.update {
                it.copy(
                    incomeStatement = (inc as? AppResult.Success)?.data,
                    balanceSheet = (bal as? AppResult.Success)?.data,
                    salesReport = (sales as? AppResult.Success)?.data,
                    partyBalances = (parties as? AppResult.Success)?.data,
                    inventoryReport = (inv as? AppResult.Success)?.data,
                    isLoading = false,
                    error = error
                )
            }
        }
    }

    private fun periodRange(p: ReportPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val from = when (p) {
            ReportPeriod.TODAY -> now - day
            ReportPeriod.LAST_7_DAYS -> now - 7 * day
            ReportPeriod.LAST_30_DAYS -> now - 30 * day
            ReportPeriod.THIS_MONTH -> now - 30 * day
            ReportPeriod.ALL -> 0L
        }
        return from to now
    }
}