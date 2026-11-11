package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.ChequeDao
import ir.hasabdari.paydar.data.local.entity.ChequeEntity
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.ChequeStatus
import ir.hasabdari.paydar.domain.repository.ChequeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChequeRepositoryImpl @Inject constructor(
    private val dao: ChequeDao
) : ChequeRepository {

    override fun observeAll(storeId: String): Flow<List<Cheque>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override fun observeByKind(storeId: String, kind: ChequeKind): Flow<List<Cheque>> =
        dao.observeByKind(storeId, kind.name).map { list -> list.map { it.toDomain() } }

    override fun observeOpen(storeId: String): Flow<List<Cheque>> =
        dao.observeOpen(storeId).map { list -> list.map { it.toDomain() } }

    override fun observeDueSoon(storeId: String, fromMillis: Long, toMillis: Long): Flow<List<Cheque>> =
        dao.observeDueSoon(storeId, fromMillis, toMillis).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<Cheque?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun save(cheque: Cheque): AppResult<String> = try {
        val existing = dao.getById(cheque.id)
        if (existing == null) dao.insert(cheque.toEntity()) else dao.update(cheque.toEntity())
        AppResult.Success(cheque.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun updateStatus(id: String, status: ChequeStatus): AppResult<Unit> = try {
        dao.updateStatus(id, status.name)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

private fun ChequeEntity.toDomain(): Cheque = Cheque(
    id = id,
    kind = ChequeKind.valueOf(kind),
    number = number,
    amount = Money.fromRials(amount),
    issueDate = issueDate,
    dueDate = dueDate,
    bankName = bankName,
    branch = branch,
    accountHolder = accountHolder,
    accountNumber = accountNumber,
    partyId = partyId,
    status = ChequeStatus.valueOf(status),
    relatedInvoiceId = relatedInvoiceId,
    note = note,
    createdAt = createdAt,
    storeId = storeId
)

private fun Cheque.toEntity(): ChequeEntity = ChequeEntity(
    id = id,
    kind = kind.name,
    number = number,
    amount = amount.rials,
    issueDate = issueDate,
    dueDate = dueDate,
    bankName = bankName,
    branch = branch,
    accountHolder = accountHolder,
    accountNumber = accountNumber,
    partyId = partyId,
    status = status.name,
    relatedInvoiceId = relatedInvoiceId,
    note = note,
    createdAt = createdAt,
    storeId = storeId
)