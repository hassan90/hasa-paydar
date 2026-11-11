package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetPartiesUseCase @Inject constructor(
    private val repo: PartyRepository
) {
    operator fun invoke(
        storeId: String = AppConstants.DEFAULT_STORE_ID,
        kind: PartyKind? = null
    ): Flow<List<Party>> = if (kind == null) {
        repo.observeAll(storeId)
    } else {
        repo.observeByKind(storeId, kind)
    }
}

class SearchPartiesUseCase @Inject constructor(
    private val repo: PartyRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID, query: String): Flow<List<Party>> =
        repo.search(storeId, query)
}

class AddPartyUseCase @Inject constructor(
    private val repo: PartyRepository
) {
    suspend operator fun invoke(
        kind: PartyKind,
        name: String,
        phone: String? = null,
        mobile: String? = null,
        nationalId: String? = null,
        economicCode: String? = null,
        address: String? = null,
        province: String? = null,
        city: String? = null,
        creditLimitRials: Long = 0,
        discountPercent: Int = 0,
        groupName: String? = null,
        notes: String? = null,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (name.isBlank()) {
            return AppResult.Failure(DomainError.EmptyField("نام طرف حساب"))
        }
        if (mobile != null && mobile.isNotBlank() && !mobile.matches(Regex("^09\\d{9}$"))) {
            return AppResult.Failure(DomainError.InvalidInput("موبایل", "باید با ۰۹ شروع و ۱۱ رقم باشد"))
        }
        if (creditLimitRials < 0) {
            return AppResult.Failure(DomainError.NegativeAmount("سقف اعتبار"))
        }

        val party = Party(
            id = UUID.randomUUID().toString(),
            kind = kind,
            name = name.trim(),
            phone = phone?.trim()?.takeIf { it.isNotBlank() },
            mobile = mobile?.trim()?.takeIf { it.isNotBlank() },
            nationalId = nationalId?.trim()?.takeIf { it.isNotBlank() },
            economicCode = economicCode?.trim()?.takeIf { it.isNotBlank() },
            address = address?.trim()?.takeIf { it.isNotBlank() },
            province = province?.trim()?.takeIf { it.isNotBlank() },
            city = city?.trim()?.takeIf { it.isNotBlank() },
            creditLimit = Money.fromRials(creditLimitRials),
            discountPercent = discountPercent.coerceIn(0, 100),
            groupName = groupName?.trim()?.takeIf { it.isNotBlank() },
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            storeId = storeId
        )
        return repo.save(party)
    }
}

class DeletePartyUseCase @Inject constructor(
    private val repo: PartyRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repo.delete(id)
}