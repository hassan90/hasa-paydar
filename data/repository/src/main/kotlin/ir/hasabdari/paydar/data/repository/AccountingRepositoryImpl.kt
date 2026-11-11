package ir.hasabdari.paydar.data.repository

import androidx.room.withTransaction
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.PaydarDatabase
import ir.hasabdari.paydar.data.local.dao.*
import ir.hasabdari.paydar.data.local.entity.*
import ir.hasabdari.paydar.domain.model.*
import ir.hasabdari.paydar.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val dao: AccountDao
) : AccountRepository {
    override fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { it.map { e -> e.toDomain() } }

    override fun observePostable(): Flow<List<Account>> =
        dao.observePostable().map { it.map { e -> e.toDomain() } }

    override suspend fun getByCode(code: String): AppResult<Account?> =
        AppResult.Success(dao.getByCode(code)?.toDomain())

    override suspend fun getAllCodes(): AppResult<Set<String>> =
        AppResult.Success(dao.getAllCodes().toSet())
}

@Singleton
class FiscalYearRepositoryImpl @Inject constructor(
    private val dao: FiscalYearDao
) : FiscalYearRepository {
    override fun observeAll(storeId: String): Flow<List<FiscalYear>> =
        dao.observeAll(storeId).map { it.map { e -> e.toDomain() } }

    override suspend fun getCurrent(storeId: String): AppResult<FiscalYear?> =
        AppResult.Success(dao.getCurrent(storeId)?.toDomain())

    override suspend fun save(fy: FiscalYear): AppResult<String> = try {
        dao.insert(FiscalYearEntity(
            id = fy.id, title = fy.title, startDate = fy.startDate, endDate = fy.endDate,
            isClosed = fy.isClosed, storeId = fy.storeId, createdAt = System.currentTimeMillis()
        ))
        AppResult.Success(fy.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val db: PaydarDatabase,
    private val entryDao: JournalEntryDao,
    private val lineDao: JournalLineDao
) : JournalRepository {

    override fun observeAll(storeId: String): Flow<List<JournalEntry>> =
        entryDao.observeAll(storeId).map { it.map { e -> e.toDomain() } }

    override suspend fun getById(id: String): AppResult<JournalEntryWithLines?> = try {
        val entry = entryDao.getById(id)
        if (entry == null) AppResult.Success(null)
        else {
            val lines = lineDao.getByEntry(id)
            AppResult.Success(JournalEntryWithLines(entry.toDomain(), lines.map { it.toDomain() }))
        }
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun findBySource(sourceType: String, sourceId: String, storeId: String): AppResult<JournalEntry?> =
        AppResult.Success(entryDao.findBySource(sourceType, sourceId, storeId)?.toDomain())

    override suspend fun post(draft: JournalDraft, fiscalYearId: String): AppResult<String> = try {
        db.withTransaction {
            val entryId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val nextNumber = entryDao.maxNumber(fiscalYearId) + 1

            entryDao.insert(JournalEntryEntity(
                id = entryId,
                number = nextNumber,
                fiscalYearId = fiscalYearId,
                entryDate = draft.entryDate,
                description = draft.description,
                sourceType = draft.sourceType.name,
                sourceId = draft.sourceId,
                status = "POSTED",
                reversalOf = null,
                createdBy = draft.createdBy,
                createdAt = now,
                storeId = draft.storeId
            ))

            val lines = draft.lines.mapIndexed { idx, d ->
                JournalLineEntity(
                    id = UUID.randomUUID().toString(),
                    entryId = entryId,
                    lineNo = idx + 1,
                    accountCode = d.accountCode,
                    partyId = d.partyId,
                    debit = d.debit,
                    credit = d.credit,
                    description = d.description
                )
            }
            lineDao.insertAll(lines)
            entryId
        }.let { AppResult.Success(it) }
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun trialBalance(storeId: String, fromDate: Long, toDate: Long): AppResult<TrialBalance> = try {
        val raw = lineDao.trialBalanceRaw(storeId, fromDate, toDate)
        val rows = raw.map {
            TrialBalanceRow(
                accountCode = it.accountCode,
                accountName = it.accountName,
                debit = it.totalDebit,
                credit = it.totalCredit
            )
        }
        val totalDebit = rows.sumOf { it.debit }
        val totalCredit = rows.sumOf { it.credit }
        AppResult.Success(TrialBalance(
            fromDate = fromDate, toDate = toDate, rows = rows,
            totalDebit = totalDebit, totalCredit = totalCredit,
            isBalanced = totalDebit == totalCredit
        ))
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

// Mappers
private fun AccountEntity.toDomain() = Account(
    code, name, parentCode, level,
    AccountNature.valueOf(nature), AccountCategory.valueOf(category),
    isPostable, isActive
)

private fun FiscalYearEntity.toDomain() = FiscalYear(
    id, title, startDate, endDate, isClosed, storeId
)

private fun JournalEntryEntity.toDomain() = JournalEntry(
    id, number, fiscalYearId, entryDate, description,
    JournalSourceType.valueOf(sourceType), sourceId,
    JournalStatus.valueOf(status), reversalOf, createdBy, createdAt, storeId
)

private fun JournalLineEntity.toDomain() = JournalLine(
    id, entryId, lineNo, accountCode, partyId, debit, credit, description
)