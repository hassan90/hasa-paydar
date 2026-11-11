package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.ProductGroupDao
import ir.hasabdari.paydar.data.local.entity.ProductGroupEntity
import ir.hasabdari.paydar.domain.model.ProductGroup
import ir.hasabdari.paydar.domain.model.ProductGroupNode
import ir.hasabdari.paydar.domain.repository.ProductGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductGroupRepositoryImpl @Inject constructor(
    private val dao: ProductGroupDao
) : ProductGroupRepository {

    override fun observeAll(storeId: String): Flow<List<ProductGroup>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<ProductGroup?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun save(group: ProductGroup): AppResult<String> = try {
        val now = System.currentTimeMillis()
        val entity = group.toEntity(now)
        val existing = dao.getById(group.id)
        if (existing == null) dao.insert(entity) else dao.update(entity.copy(createdAt = existing.createdAt))
        AppResult.Success(group.id)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun delete(id: String): AppResult<Unit> = try {
        dao.softDelete(id, System.currentTimeMillis())
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override fun observeTree(storeId: String): Flow<List<ProductGroupNode>> =
        dao.observeAllWithCount(storeId).map { rows ->
            val groups = rows.map {
                ProductGroupNode(group = it.group.toDomain(), productCount = it.productCount)
            }
            buildTree(groups)
        }

    private fun buildTree(nodes: List<ProductGroupNode>): List<ProductGroupNode> {
        val byParent = nodes.groupBy { it.group.parentId }
        fun buildFor(parentId: String?): List<ProductGroupNode> =
            (byParent[parentId] ?: emptyList()).map { node ->
                node.copy(children = buildFor(node.group.id))
            }
        return buildFor(null)
    }
}

private fun ProductGroupEntity.toDomain(): ProductGroup = ProductGroup(
    id = id, name = name, parentId = parentId, isActive = isActive, storeId = storeId
)

private fun ProductGroup.toEntity(now: Long): ProductGroupEntity = ProductGroupEntity(
    id = id, name = name, parentId = parentId, isActive = isActive, storeId = storeId,
    createdAt = now, updatedAt = now, deletedAt = null
)