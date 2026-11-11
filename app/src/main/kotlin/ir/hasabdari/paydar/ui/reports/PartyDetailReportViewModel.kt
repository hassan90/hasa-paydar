package ir.hasabdari.paydar.ui.reports

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import ir.hasabdari.paydar.domain.usecase.GeneratePartyDetailReportUseCase
import ir.hasabdari.paydar.domain.usecase.GetSettingsUseCase
import ir.hasabdari.paydar.ui.print.InvoicePrintManager
import ir.hasabdari.paydar.ui.print.ReportFileExporter
import ir.hasabdari.paydar.ui.print.ReportHtmlBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

enum class ReportDateRange {
    THIS_MONTH, LAST_MONTH, THIS_YEAR, LAST_90_DAYS, ALL_TIME
}

data class PartyDetailReportUiState(
    val report: PartyDetailReport? = null,
    val isLoading: Boolean = true,
    val range: ReportDateRange = ReportDateRange.LAST_90_DAYS,
    val isExporting: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class PartyDetailReportViewModel @Inject constructor(
    private val generateReport: GeneratePartyDetailReportUseCase,
    private val getSettings: GetSettingsUseCase,
    private val printManager: InvoicePrintManager,
    private val fileExporter: ReportFileExporter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val partyId: String = savedStateHandle["partyId"] ?: ""

    private val _state = MutableStateFlow(PartyDetailReportUiState())
    val state = _state.asStateFlow()

    init { load() }

    fun onRangeChange(range: ReportDateRange) {
        _state.update { it.copy(range = range) }
        load()
    }

    fun refresh() = load()

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val (from, to) = calculateRange(_state.value.range)
            when (val result = generateReport(partyId, from, to)) {
                is AppResult.Success ->
                    _state.update { it.copy(report = result.data, isLoading = false) }
                is AppResult.Failure ->
                    _state.update {
                        it.copy(isLoading = false, message = result.error.message)
                    }
            }
        }
    }

    fun printReport(activity: Activity) {
        val report = _state.value.report ?: return
        viewModelScope.launch {
            try {
                val store = getSettings().storeInfo
                val html = ReportHtmlBuilder.buildPartyReport(report, store)
                _state.update { it.copy(isExporting = true) }
                withContext(Dispatchers.Main) {
                    printManager.print(
                        html = html,
                        jobName = "گزارش ${report.partyName}",
                        activity = activity
                    )
                }
                _state.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isExporting = false, message = "خطا: ${e.message}")
                }
            }
        }
    }

    fun exportExcel() {
        val report = _state.value.report ?: return
        viewModelScope.launch {
            try {
                _state.update { it.copy(isExporting = true) }
                val csv = ReportFileExporter.buildPartyCsv(report)
                val fileName = "party_${report.partyName.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
                fileExporter.saveCsvToDownloads(csv, fileName)
                _state.update {
                    it.copy(
                        isExporting = false,
                        message = "✓ فایل Excel در Downloads ذخیره شد"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isExporting = false, message = "خطا: ${e.message}")
                }
            }
        }
    }

    private fun calculateRange(range: ReportDateRange): Pair<Long, Long> {
        val to = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = to }
        val from = when (range) {
            ReportDateRange.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            ReportDateRange.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.add(Calendar.MONTH, -1)
                cal.timeInMillis
            }
            ReportDateRange.THIS_YEAR -> to - 365L * 86_400_000L
            ReportDateRange.LAST_90_DAYS -> to - 90L * 86_400_000L
            ReportDateRange.ALL_TIME -> 0L
        }
        return from to to
    }
}