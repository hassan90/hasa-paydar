package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.ProductDao
import ir.hasabdari.paydar.data.local.dao.ProductWithStockRaw
import ir.hasabdari.paydar.data.local.entity.ProductEntity
import ir.hasabdari.paydar.domain.model.Product
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val dao: ProductDao
) : ProductRepository {

    override fun observeAll(storeId: String): Flow<List<Product>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override fun observeAllWithStock(storeId: String): Flow<List<ProductWithStock>> =
        dao.observeAllWithStock(storeId).map { list -> list.map { it.toProductWithStock() } }

    override fun search(storeId: String, query: String): Flow<List<Product>> =
        dao.searchWithStock(storeId, query).map { list -> list.map { it.product.toDomain() } }

    override fun searchWithStock(storeId: String, query: String): Flow<List<ProductWithStock>> =
        dao.searchWithStock(storeId, query).map { list -> list.map { it.toProductWithStock() } }

    override suspend fun getById(id: String): AppResult<Product?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun save(product: Product): AppResult<String> = try {
        val now = System.currentTimeMillis()
        val entity = product.toEntity(now)
        val existing = dao.getById(product.id)
        if (existing == null) dao.insert(entity) else dao.update(entity.copy(createdAt = existing.createdAt))
        AppResult.Success(product.id)
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

private fun ProductWithStockRaw.toProductWithStock(): ProductWithStock =
    ProductWithStock(
        product = product.toDomain(),
        totalStockScaled = totalStockScaled,
        totalValueRials = totalValueRials
    )

private fun ProductEntity.toDomain(): Product = Product(
    id = id, code = code, barcode = barcode, name = name,
    groupId = groupId, unitId = unitId,
    purchasePrice = Money.fromRials(purchasePrice),
    salePrice = Money.fromRials(salePrice),
    minStockScaled = minStockScaled, maxStockScaled = maxStockScaled,
    isService = isService, isActive = isActive,
    trackSerial = trackSerial, trackExpiry = trackExpiry,
    imageUri = imageUri, notes = notes, storeId = storeId
)

private fun Product.toEntity(now: Long): ProductEntity = ProductEntity(
    id = id, code = code, barcode = barcode, name = name,
    groupId = groupId, unitId = unitId,
    purchasePrice = purchasePrice.rials, salePrice = salePrice.rials,
    minStockScaled = minStockScaled, maxStockScaled = maxStockScaled,
    isService = isService, isActive = isActive,
    trackSerial = trackSerial, trackExpiry = trackExpiry,
    imageUri = imageUri, notes = notes,
    createdAt = now, updatedAt = now, deletedAt = null,
    syncState = "LOCAL", storeId = storeId
)