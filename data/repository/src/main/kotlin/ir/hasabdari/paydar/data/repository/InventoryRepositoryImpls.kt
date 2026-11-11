package ir.hasabdari.paydar.data.repository

import androidx.room.withTransaction
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.PaydarDatabase
import ir.hasabdari.paydar.data.local.dao.StockDao
import ir.hasabdari.paydar.data.local.dao.StockMovementDao
import ir.hasabdari.paydar.data.local.dao.WarehouseDao
import ir.hasabdari.paydar.data.local.entity.StockEntity
import ir.hasabdari.paydar.data.local.entity.StockMovementEntity
import ir.hasabdari.paydar.data.local.entity.WarehouseEntity
import ir.hasabdari.paydar.domain.model.StockItem
import ir.hasabdari.paydar.domain.model.StockMovement
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.model.WarehouseStock
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import ir.hasabdari.paydar.domain.stock.StockCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WarehouseRepositoryImpl @Inject constructor(
    private val dao: WarehouseDao
) : WarehouseRepository {

    override fun observeAll(storeId: String): Flow<List<Warehouse>> =
        dao.observeAll(storeId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): AppResult<Warehouse?> =
        AppResult.Success(dao.getById(id)?.toDomain())

    override suspend fun getDefault(storeId: String): AppResult<Warehouse?> =
        AppResult.Success(dao.getDefault(storeId)?.toDomain())

    override suspend fun save(warehouse: Warehouse): AppResult<String> = try {
        val now = System.currentTimeMillis()
        val entity = WarehouseEntity(
            id = warehouse.id,
            name = warehouse.name,
            isDefault = warehouse.isDefault,
            isActive = warehouse.isActive,
            storeId = warehouse.storeId,
            createdAt = now,
            updatedAt = now
        )
        val existing = dao.getById(warehouse.id)
        if (existing == null) dao.insert(entity) else dao.update(entity)
        AppResult.Success(warehouse.id)
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
class StockRepositoryImpl @Inject constructor(
    private val db: PaydarDatabase,
    private val stockDao: StockDao,
    private val movementDao: StockMovementDao
) : StockRepository {

    override fun observeByProductAndStore(
        productId: String,
        storeId: String
    ): Flow<List<WarehouseStock>> =
        stockDao.observeStockByWarehouse(productId, storeId).map { list ->
            list.map {
                WarehouseStock(
                    warehouseId = it.warehouseId,
                    warehouseName = it.warehouseName,
                    quantityScaled = it.quantityScaled,
                    avgCost = Money.fromRials(it.avgCostRials)
                )
            }
        }

    override suspend fun getItem(productId: String, warehouseId: String): AppResult<StockItem?> {
        val entity = stockDao.get(productId, warehouseId) ?: return AppResult.Success(null)
        return AppResult.Success(
            StockItem(
                productId = entity.productId,
                warehouseId = entity.warehouseId,
                quantityScaled = entity.quantityScaled,
                avgCost = Money.fromRials(entity.avgCostRials),
                lastUpdated = entity.lastUpdated
            )
        )
    }

    override suspend fun addMovement(
        productId: String,
        warehouseId: String,
        type: StockMovementType,
        quantityDeltaScaled: Long,
        unitCost: Long,
        referenceType: String?,
        referenceId: String?,
        note: String?,
        storeId: String
    ): AppResult<String> = try {
        val movementId: String = db.withTransaction {
            val now = System.currentTimeMillis()
            val existing = stockDao.get(productId, warehouseId)

            val currentQty = existing?.quantityScaled ?: 0L
            val currentAvg = existing?.avgCostRials ?: 0L

            val isOutgoing = type == StockMovementType.SALE_OUT ||
                    type == StockMovementType.RETURN_OUT ||
                    type == StockMovementType.TRANSFER_OUT ||
                    type == StockMovementType.ADJUST_OUT

            if (isOutgoing && currentQty + quantityDeltaScaled < 0) {
                throw IllegalStateException(
                    "موجودی کافی نیست. درخواستی: ${-quantityDeltaScaled / 1000.0}، موجود: ${currentQty / 1000.0}"
                )
            }

            val newQty = currentQty + quantityDeltaScaled
            val newAvg = if (quantityDeltaScaled > 0) {
                StockCalculator.newAverageCost(
                    currentQtyScaled = currentQty,
                    currentAvgCostRials = currentAvg,
                    addQtyScaled = quantityDeltaScaled,
                    addUnitCostRials = unitCost
                )
            } else {
                currentAvg
            }

            stockDao.upsert(
                StockEntity(
                    productId = productId,
                    warehouseId = warehouseId,
                    quantityScaled = newQty,
                    avgCostRials = newAvg,
                    lastUpdated = now
                )
            )

            val newMovementId = UUID.randomUUID().toString()
            movementDao.insert(
                StockMovementEntity(
                    id = newMovementId,
                    productId = productId,
                    warehouseId = warehouseId,
                    type = type.name,
                    quantityDeltaScaled = quantityDeltaScaled,
                    unitCostRials = unitCost,
                    referenceType = referenceType,
                    referenceId = referenceId,
                    note = note,
                    occurredAt = now,
                    storeId = storeId
                )
            )
            newMovementId
        }
        AppResult.Success(movementId)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    override suspend fun getMovements(
        productId: String,
        storeId: String
    ): AppResult<List<StockMovement>> = try {
        val entities = movementDao.getByProduct(productId, storeId)
        val movements = entities.map { entity ->
            StockMovement(
                id = entity.id,
                productId = entity.productId,
                warehouseId = entity.warehouseId,
                type = StockMovementType.valueOf(entity.type),
                quantityDeltaScaled = entity.quantityDeltaScaled,
                unitCost = Money.fromRials(entity.unitCostRials),
                referenceType = entity.referenceType,
                referenceId = entity.referenceId,
                note = entity.note,
                occurredAt = entity.occurredAt,
                storeId = entity.storeId
            )
        }
        AppResult.Success(movements)
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }
}

private fun WarehouseEntity.toDomain(): Warehouse = Warehouse(
    id = id, name = name, isDefault = isDefault, isActive = isActive, storeId = storeId
)