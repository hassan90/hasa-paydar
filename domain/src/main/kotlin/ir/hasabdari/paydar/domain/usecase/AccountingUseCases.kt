package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Account
import ir.hasabdari.paydar.domain.model.JournalDraft
import ir.hasabdari.paydar.domain.model.JournalEntry
import ir.hasabdari.paydar.domain.model.JournalEntryWithLines
import ir.hasabdari.paydar.domain.model.TrialBalance
import ir.hasabdari.paydar.domain.repository.AccountRepository
import ir.hasabdari.paydar.domain.repository.FiscalYearRepository
import ir.hasabdari.paydar.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountsUseCase @Inject constructor(
    private val repo: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> = repo.observeAll()
}

class GetPostableAccountsUseCase @Inject constructor(
    private val repo: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> = repo.observePostable()
}

/**
 * قلب سیستم: ثبت سند حسابداری
 * - Idempotent: اگر سند از این منبع قبلاً ثبت شده، همان id برمی‌گردد
 * - سال مالی جاری تضمین می‌شود
 * - توازن بدهکار/بستانکار اجباری (در JournalDraft.init)
 */
class PostJournalEntryUseCase @Inject constructor(
    private val journalRepo: JournalRepository,
    private val fiscalYearRepo: FiscalYearRepository,
    private val ensureFiscalYear: EnsureFiscalYearUseCase
) {
    suspend operator fun invoke(draft: JournalDraft): AppResult<String> {
        // ─── Idempotency ───
        val sourceId = draft.sourceId
        if (!sourceId.isNullOrBlank()) {
            val existing = journalRepo.findBySource(draft.sourceType.name, sourceId, draft.storeId)
            if (existing is AppResult.Success) {
                // ⚠️ استخراج به متغیر محلی برای smart cast
                val existingEntry = existing.data
                if (existingEntry != null) {
                    return AppResult.Success(existingEntry.id)
                }
            }
        }

        // ─── اطمینان از سال مالی جاری ───
        val fyResult = ensureFiscalYear(draft.storeId)
        if (fyResult is AppResult.Failure) {
            return AppResult.Failure(fyResult.error)
        }
        val fySuccess = fyResult as AppResult.Success
        val fiscalYear = fySuccess.data
        if (fiscalYear == null) {
            return AppResult.Failure(DomainError.Unknown())
        }

        return journalRepo.post(draft, fiscalYear.id)
    }
}

class GetJournalEntriesUseCase @Inject constructor(
    private val repo: JournalRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<JournalEntry>> =
        repo.observeAll(storeId)
}

class GetJournalDetailUseCase @Inject constructor(
    private val repo: JournalRepository
) {
    suspend operator fun invoke(id: String): AppResult<JournalEntryWithLines?> = repo.getById(id)
}

class GetTrialBalanceUseCase @Inject constructor(
    private val repo: JournalRepository
) {
    suspend operator fun invoke(
        fromDate: Long,
        toDate: Long,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<TrialBalance> = repo.trialBalance(storeId, fromDate, toDate)
}