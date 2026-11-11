package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.StockItem
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.model.WarehouseStock
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetWarehousesUseCase @Inject constructor(
    private val repo: WarehouseRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<Warehouse>> =
        repo.observeAll(storeId)
}

class AddWarehouseUseCase @Inject constructor(
    private val repo: WarehouseRepository
) {
    suspend operator fun invoke(
        name: String,
        isDefault: Boolean = false,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (name.isBlank()) {
            return AppResult.Failure(DomainError.EmptyField("نام انبار"))
        }
        return repo.save(
            Warehouse(
                id = UUID.randomUUID().toString(),
                name = name,
                isDefault = isDefault,
                storeId = storeId
            )
        )
    }
}

/**
 * اطمینان از وجود انبار پیش‌فرض — در اولین اجرا "انبار اصلی" را می‌سازد.
 */
class EnsureDefaultWarehouseUseCase @Inject constructor(
    private val repo: WarehouseRepository
) {
    suspend operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): AppResult<Warehouse> {
        val existing = repo.getDefault(storeId)
        val existingData = (existing as? AppResult.Success)?.data
        if (existingData != null) return AppResult.Success(existingData)

        val newWh = Warehouse(
            id = UUID.randomUUID().toString(),
            name = AppConstants.DEFAULT_WAREHOUSE_NAME,
            isDefault = true,
            storeId = storeId
        )
        return when (val result = repo.save(newWh)) {
            is AppResult.Success -> AppResult.Success(newWh)
            is AppResult.Failure -> result
        }
    }
}

class GetStockByProductUseCase @Inject constructor(
    private val repo: StockRepository
) {
    operator fun invoke(
        productId: String,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): Flow<List<WarehouseStock>> = repo.observeByProductAndStore(productId, storeId)
}

class AddStockMovementUseCase @Inject constructor(
    private val repo: StockRepository
) {
    suspend operator fun invoke(
        productId: String,
        warehouseId: String,
        type: StockMovementType,
        quantityDeltaScaled: Long,
        unitCostRials: Long,
        note: String? = null,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (quantityDeltaScaled == 0L) {
            return AppResult.Failure(DomainError.InvalidInput("مقدار", "باید غیر صفر باشد"))
        }
        if (unitCostRials < 0) {
            return AppResult.Failure(DomainError.NegativeAmount("بهای واحد"))
        }
        return repo.addMovement(
            productId = productId,
            warehouseId = warehouseId,
            type = type,
            quantityDeltaScaled = quantityDeltaScaled,
            unitCost = unitCostRials,
            note = note,
            storeId = storeId
        )
    }
}