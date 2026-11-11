package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.CashAccountDao
import ir.hasabdari.paydar.data.local.dao.CashTransactionDao
import ir.hasabdari.paydar.data.local.entity.CashAccountEntity
import ir.hasabdari.paydar.data.local.entity.CashTransactionEntity
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.domain.model.CashTransaction
import ir.hasabdari.paydar.domain.model.CashTransactionMethod
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CashAccountRepositoryImpl @Inject constructor(
    private val dao: CashAccountDao
) : CashAccountRepository {

    override fun observeAll(storeId: String): Flow<List<CashAccount>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<CashAccount?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun save(account: CashAccount): AppResult<String> = try {
        val now = System.currentTimeMillis()
        val entity = account.toEntity(now)
        val existing = dao.getById(account.id)
        if (existing == null) dao.insert(entity) else dao.update(entity.copy(createdAt = existing.createdAt))
        AppResult.Success(account.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun delete(id: String): AppResult<Unit> = try {
        dao.deactivate(id, System.currentTimeMillis())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

@Singleton
class CashTransactionRepositoryImpl @Inject constructor(
    private val dao: CashTransactionDao
) : CashTransactionRepository {

    override fun observeAll(storeId: String): Flow<List<CashTransaction>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override fun observeByAccount(accountId: String, storeId: String): Flow<List<CashTransaction>> =
        dao.observeByAccount(storeId, accountId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<CashTransaction?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun save(transaction: CashTransaction): AppResult<String> = try {
        val now = System.currentTimeMillis()
        dao.insert(transaction.toEntity(now))
        AppResult.Success(transaction.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun delete(id: String): AppResult<Unit> = try {
        dao.delete(id)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun getBalance(accountId: String): AppResult<Long> = try {
        AppResult.Success(dao.getBalance(accountId) ?: 0L)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

private fun CashAccountEntity.toDomain(): CashAccount = CashAccount(
    id = id, kind = CashAccountKind.valueOf(kind), name = name,
    bankName = bankName, accountNumber = accountNumber,
    openingBalance = Money.fromRials(openingBalance),
    isActive = isActive, storeId = storeId
)

private fun CashAccount.toEntity(now: Long): CashAccountEntity = CashAccountEntity(
    id = id, kind = kind.name, name = name,
    bankName = bankName, accountNumber = accountNumber,
    openingBalance = openingBalance.rials,
    isActive = isActive, createdAt = now, updatedAt = now, storeId = storeId
)

private fun CashTransactionEntity.toDomain(): CashTransaction = CashTransaction(
    id = id, type = CashTransactionType.valueOf(type), method = CashTransactionMethod.valueOf(method),
    cashAccountId = cashAccountId, toCashAccountId = toCashAccountId, partyId = partyId,
    amount = Money.fromRials(amount), occurredAt = occurredAt,
    description = description, storeId = storeId
)

private fun CashTransaction.toEntity(now: Long): CashTransactionEntity = CashTransactionEntity(
    id = id, type = type.name, method = method.name,
    cashAccountId = cashAccountId, toCashAccountId = toCashAccountId, partyId = partyId,
    amount = amount.rials, occurredAt = occurredAt, description = description,
    createdAt = now, storeId = storeId
)