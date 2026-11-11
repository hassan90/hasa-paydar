package ir.hasabdari.paydar.ui.cheques

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.usecase.GetChequesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChequeCalendarUiState(
    val year: Int = 1403,
    val month: Int = 1,
    val selectedDay: Int? = null,
    val cheques: List<Cheque> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ChequeCalendarViewModel @Inject constructor(
    getCheques: GetChequesUseCase
) : ViewModel() {

    private val today = JalaliDate.today()
    private val _state = MutableStateFlow(
        ChequeCalendarUiState(year = today.year, month = today.month)
    )
    val state: StateFlow<ChequeCalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCheques().collect { list ->
                _state.value = _state.value.copy(cheques = list, isLoading = false)
            }
        }
    }

    fun onPrevMonth() {
        val m = _state.value.month - 1
        _state.value = if (m < 1) {
            _state.value.copy(year = _state.value.year - 1, month = 12, selectedDay = null)
        } else {
            _state.value.copy(month = m, selectedDay = null)
        }
    }

    fun onNextMonth() {
        val m = _state.value.month + 1
        _state.value = if (m > 12) {
            _state.value.copy(year = _state.value.year + 1, month = 1, selectedDay = null)
        } else {
            _state.value.copy(month = m, selectedDay = null)
        }
    }

    fun onDayClick(day: Int) {
        _state.value = _state.value.copy(selectedDay = day)
    }

    fun chequesOnDay(day: Int): List<Cheque> {
        val s = _state.value
        return s.cheques.filter {
            val d = JalaliDate.fromEpochMillis(it.dueDate)
            d.year == s.year && d.month == s.month && d.day == day
        }
    }
}