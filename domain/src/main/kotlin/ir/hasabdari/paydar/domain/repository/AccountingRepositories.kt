package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Account
import ir.hasabdari.paydar.domain.model.FiscalYear
import ir.hasabdari.paydar.domain.model.JournalDraft
import ir.hasabdari.paydar.domain.model.JournalEntry
import ir.hasabdari.paydar.domain.model.JournalEntryWithLines
import ir.hasabdari.paydar.domain.model.TrialBalance
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observePostable(): Flow<List<Account>>
    suspend fun getByCode(code: String): AppResult<Account?>
    suspend fun getAllCodes(): AppResult<Set<String>>
}

interface FiscalYearRepository {
    fun observeAll(storeId: String): Flow<List<FiscalYear>>
    suspend fun getCurrent(storeId: String): AppResult<FiscalYear?>
    suspend fun save(fy: FiscalYear): AppResult<String>
}

interface JournalRepository {
    fun observeAll(storeId: String): Flow<List<JournalEntry>>
    suspend fun getById(id: String): AppResult<JournalEntryWithLines?>
    suspend fun findBySource(sourceType: String, sourceId: String, storeId: String): AppResult<JournalEntry?>
    suspend fun post(draft: JournalDraft, fiscalYearId: String): AppResult<String>

    suspend fun trialBalance(storeId: String, fromDate: Long, toDate: Long): AppResult<TrialBalance>
}