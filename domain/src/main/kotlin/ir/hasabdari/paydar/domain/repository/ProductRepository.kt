package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Product
import ir.hasabdari.paydar.domain.model.ProductWithStock
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeAll(storeId: String): Flow<List<Product>>
    fun observeAllWithStock(storeId: String): Flow<List<ProductWithStock>>
    fun search(storeId: String, query: String): Flow<List<Product>>
    fun searchWithStock(storeId: String, query: String): Flow<List<ProductWithStock>>
    suspend fun getById(id: String): AppResult<Product?>
    suspend fun save(product: Product): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>
}