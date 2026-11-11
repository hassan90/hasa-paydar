package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.PartyDao
import ir.hasabdari.paydar.data.local.entity.PartyEntity
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyRepositoryImpl @Inject constructor(
    private val dao: PartyDao
) : PartyRepository {

    override fun observeAll(storeId: String): Flow<List<Party>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override fun observeByKind(storeId: String, kind: PartyKind): Flow<List<Party>> =
        dao.observeByKind(storeId, kind.name).map { list -> list.map { it.toDomain() } }

    override fun search(storeId: String, query: String): Flow<List<Party>> =
        dao.search(storeId, query).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<Party?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun save(party: Party): AppResult<String> = try {
        val now = System.currentTimeMillis()
        val entity = party.toEntity(now)
        val existing = dao.getById(party.id)
        if (existing == null) dao.insert(entity) else dao.update(entity.copy(createdAt = existing.createdAt))
        AppResult.Success(party.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun delete(id: String): AppResult<Unit> = try {
        dao.softDelete(id, System.currentTimeMillis())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

private fun PartyEntity.toDomain(): Party = Party(
    id = id, kind = PartyKind.valueOf(kind), code = code, name = name,
    phone = phone, mobile = mobile, nationalId = nationalId,
    economicCode = economicCode, address = address, province = province, city = city,
    creditLimit = Money.fromRials(creditLimit), discountPercent = discountPercent,
    groupName = groupName, isActive = isActive, notes = notes, storeId = storeId
)

private fun Party.toEntity(now: Long): PartyEntity = PartyEntity(
    id = id, kind = kind.name, code = code, name = name,
    phone = phone, mobile = mobile, nationalId = nationalId,
    economicCode = economicCode, address = address, province = province, city = city,
    creditLimit = creditLimit.rials, discountPercent = discountPercent,
    groupName = groupName, isActive = isActive, notes = notes,
    createdAt = now, updatedAt = now, deletedAt = null, syncState = "LOCAL", storeId = storeId
)