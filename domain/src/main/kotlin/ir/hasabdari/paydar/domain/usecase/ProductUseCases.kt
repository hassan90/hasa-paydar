package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.core.result.map
import ir.hasabdari.paydar.domain.model.Product
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.repository.ProductRepository
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class GetProductsWithStockUseCase @Inject constructor(
    private val productRepo: ProductRepository
) {
    operator fun invoke(storeId: String): Flow<List<ProductWithStock>> =
        productRepo.observeAllWithStock(storeId)
}

class SearchProductsUseCase @Inject constructor(
    private val productRepo: ProductRepository
) {
    operator fun invoke(storeId: String, query: String): Flow<List<ProductWithStock>> =
        productRepo.searchWithStock(storeId, query)
}

/**
 * افزودن کالای جدید + ثبت موجودی اولیه (اگر داده شده).
 *
 * TODO (F2): انتقال به تراکنش تک‌اتمی با DAO مشترک.
 */
class AddProductUseCase @Inject constructor(
    private val productRepo: ProductRepository,
    private val stockRepo: StockRepository,
    private val warehouseRepo: WarehouseRepository
) {
    suspend operator fun invoke(
        name: String,
        code: String,
        barcode: String?,
        purchasePriceRials: Long,
        salePriceRials: Long,
        minStockScaled: Long,
        initialStockScaled: Long = 0,
        notes: String? = null,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (name.isBlank()) {
            return AppResult.Failure(
                ir.hasabdari.paydar.core.error.DomainError.EmptyField("نام کالا")
            )
        }
        if (code.isBlank()) {
            return AppResult.Failure(
                ir.hasabdari.paydar.core.error.DomainError.EmptyField("کد کالا")
            )
        }
        if (purchasePriceRials < 0 || salePriceRials < 0) {
            return AppResult.Failure(
                ir.hasabdari.paydar.core.error.DomainError.NegativeAmount("قیمت")
            )
        }

        val product = Product(
            id = UUID.randomUUID().toString(),
            code = code,
            barcode = barcode,
            name = name,
            groupId = null,
            unitId = AppConstants.DEFAULT_UNIT_ID,
            purchasePrice = Money.fromRials(purchasePriceRials),
            salePrice = Money.fromRials(salePriceRials),
            minStockScaled = minStockScaled,
            maxStockScaled = null,
            isService = false,
            isActive = true,
            trackSerial = false,
            trackExpiry = false,
            notes = notes,
            storeId = storeId
        )

        val saveResult = productRepo.save(product)
        if (saveResult is AppResult.Failure) return saveResult

        if (initialStockScaled > 0) {
            val warehouseResult = warehouseRepo.getDefault(storeId)
            val warehouse = (warehouseResult as? AppResult.Success)?.data
                ?: return AppResult.Failure(
                    ir.hasabdari.paydar.core.error.DomainError.NotFound("انبار پیش‌فرض", "default")
                )

            val movementResult = stockRepo.addMovement(
                productId = product.id,
                warehouseId = warehouse.id,
                type = StockMovementType.OPENING,
                quantityDeltaScaled = initialStockScaled,
                unitCost = purchasePriceRials,
                note = "موجودی اولیه",
                storeId = storeId
            )
            if (movementResult is AppResult.Failure) return movementResult
        }

        return saveResult
    }
}