package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.StockItem
import ir.hasabdari.paydar.domain.model.StockMovement
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.domain.model.WarehouseStock
import kotlinx.coroutines.flow.Flow

interface WarehouseRepository {
    fun observeAll(storeId: String): Flow<List<Warehouse>>
    suspend fun getById(id: String): AppResult<Warehouse?>
    suspend fun getDefault(storeId: String): AppResult<Warehouse?>
    suspend fun save(warehouse: Warehouse): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>
}

interface StockRepository {
    fun observeByProductAndStore(productId: String, storeId: String): Flow<List<WarehouseStock>>
    suspend fun getItem(productId: String, warehouseId: String): AppResult<StockItem?>

    /**
     * ثبت یک حرکت انبار به همراه به‌روزرسانی موجودی، هر دو در یک تراکنش اتمیک.
     * @return شناسه حرکت ثبت‌شده
     */
    suspend fun addMovement(
        productId: String,
        warehouseId: String,
        type: StockMovementType,
        quantityDeltaScaled: Long,
        unitCost: Long,
        referenceType: String? = null,
        referenceId: String? = null,
        note: String? = null,
        storeId: String
    ): AppResult<String>

    suspend fun getMovements(productId: String, storeId: String): AppResult<List<StockMovement>>
}