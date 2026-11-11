package ir.hasabdari.paydar.ui.parties

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.usecase.AddPartyUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPartyFormState(
    val kind: PartyKind = PartyKind.CUSTOMER,
    val name: String = "",
    val phone: String = "",
    val mobile: String = "",
    val nationalId: String = "",
    val economicCode: String = "",
    val address: String = "",
    val province: String = "",
    val city: String = "",
    val creditLimitToman: String = "",
    val discountPercent: String = "0",
    val groupName: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val mobileError: String? = null,
    val submitting: Boolean = false,
    val submitError: String? = null
)

@HiltViewModel
class AddPartyViewModel @Inject constructor(
    private val addParty: AddPartyUseCase
) : ViewModel() {

    var state by mutableStateOf(AddPartyFormState())
        private set

    fun initKind(kind: PartyKind) {
        state = state.copy(kind = kind)
    }

    fun onKindChange(k: PartyKind) { state = state.copy(kind = k) }
    fun onNameChange(v: String) { state = state.copy(name = v, nameError = null) }
    fun onPhoneChange(v: String) { state = state.copy(phone = v.filterDigitOrDash()) }
    fun onMobileChange(v: String) { state = state.copy(mobile = v.filterDigits(), mobileError = null) }
    fun onNationalIdChange(v: String) { state = state.copy(nationalId = v.filterDigits()) }
    fun onEconomicCodeChange(v: String) { state = state.copy(economicCode = v.filterDigits()) }
    fun onAddressChange(v: String) { state = state.copy(address = v) }
    fun onProvinceChange(v: String) { state = state.copy(province = v) }
    fun onCityChange(v: String) { state = state.copy(city = v) }
    fun onCreditLimitChange(v: String) { state = state.copy(creditLimitToman = v.filterDigits()) }
    fun onDiscountChange(v: String) { state = state.copy(discountPercent = v.filterDigits()) }
    fun onGroupNameChange(v: String) { state = state.copy(groupName = v) }
    fun onNotesChange(v: String) { state = state.copy(notes = v) }

    fun submit(onSuccess: () -> Unit) {
        val s = state
        val nameErr = if (s.name.isBlank()) "نام الزامی است" else null
        val mobileErr = if (s.mobile.isNotBlank() && !s.mobile.matches(Regex("^09\\d{9}$")))
            "موبایل باید ۱۱ رقم با ۰۹ باشد" else null

        if (nameErr != null || mobileErr != null) {
            state = s.copy(nameError = nameErr, mobileError = mobileErr)
            return
        }

        viewModelScope.launch {
            state = state.copy(submitting = true, submitError = null)
            val result = addParty(
                kind = s.kind,
                name = s.name.trim(),
                phone = s.phone.takeIf { it.isNotBlank() },
                mobile = s.mobile.takeIf { it.isNotBlank() },
                nationalId = s.nationalId.takeIf { it.isNotBlank() },
                economicCode = s.economicCode.takeIf { it.isNotBlank() },
                address = s.address.takeIf { it.isNotBlank() },
                province = s.province.takeIf { it.isNotBlank() },
                city = s.city.takeIf { it.isNotBlank() },
                creditLimitRials = (s.creditLimitToman.toLongOrNull() ?: 0) * 10,
                discountPercent = s.discountPercent.toIntOrNull() ?: 0,
                groupName = s.groupName.takeIf { it.isNotBlank() },
                notes = s.notes.takeIf { it.isNotBlank() }
            )
            when (result) {
                is AppResult.Success -> { state = AddPartyFormState(); onSuccess() }
                is AppResult.Failure -> state = state.copy(submitting = false, submitError = result.error.message)
            }
        }
    }
}

private fun String.filterDigits(): String = filter { it.isDigit() }
private fun String.filterDigitOrDash(): String = filter { it.isDigit() || it == '-' || it == '+' }