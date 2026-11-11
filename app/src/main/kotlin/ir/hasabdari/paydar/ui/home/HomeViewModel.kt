package ir.hasabdari.paydar.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.DashboardData
import ir.hasabdari.paydar.domain.usecase.EnsureDefaultWarehouseUseCase
import ir.hasabdari.paydar.domain.usecase.GetDashboardDataUseCase
import ir.hasabdari.paydar.domain.usecase.RetrofitLegacyCashTransactionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val data: DashboardData = DashboardData(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDashboardData: GetDashboardDataUseCase,
    private val ensureWarehouse: EnsureDefaultWarehouseUseCase,
    private val retrofitLegacy: RetrofitLegacyCashTransactionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    // ─── retrofit فقط یک‌بار در طول عمر ViewModel اجرا می‌شود ───
    private var retrofitDone = false

    init {
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            // ═══ ۱. اطمینان از وجود انبار پیش‌فرض (idempotent) ═══
            runCatching { ensureWarehouse() }
                .onFailure { Timber.w(it, "Ensure default warehouse failed (non-fatal)") }

            // ═══ ۲. Retrofit اسناد حسابداری برای تراکنش‌های قدیمی F3 ═══
            // PostJournalEntryUseCase idempotent است — اجرای مکرر مشکلی ندارد
            if (!retrofitDone) {
                runCatching { retrofitLegacy() }
                    .onSuccess { result ->
                        when (result) {
                            is AppResult.Success ->
                                Timber.i("Retrofit completed: ${result.data} journal entries created/verified")
                            is AppResult.Failure ->
                                Timber.w("Retrofit failed: ${result.error.message}")
                        }
                        retrofitDone = true
                    }
                    .onFailure { Timber.w(it, "Retrofit threw exception") }
            }

            // ═══ ۳. بارگذاری داده داشبورد ═══
            loadDashboard()
        }
    }

    fun load() {
        viewModelScope.launch { loadDashboard() }
    }

    private suspend fun loadDashboard() {
        _state.value = _state.value.copy(isLoading = true)
        when (val result = getDashboardData()) {
            is AppResult.Success ->
                _state.value = HomeUiState(data = result.data, isLoading = false)
            is AppResult.Failure ->
                _state.value = HomeUiState(isLoading = false)
        }
    }
}