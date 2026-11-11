package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.model.ProductGroup
import ir.hasabdari.paydar.domain.model.ProductGroupNode
import kotlinx.coroutines.flow.Flow

interface PartyRepository {
    fun observeAll(storeId: String): Flow<List<Party>>
    fun observeByKind(storeId: String, kind: PartyKind): Flow<List<Party>>
    fun search(storeId: String, query: String): Flow<List<Party>>
    suspend fun getById(id: String): AppResult<Party?>
    suspend fun save(party: Party): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>
}

interface ProductGroupRepository {
    fun observeAll(storeId: String): Flow<List<ProductGroup>>
    suspend fun getById(id: String): AppResult<ProductGroup?>
    suspend fun save(group: ProductGroup): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>

    /** دریافت درخت کامل با تعداد کالاها */
    fun observeTree(storeId: String): Flow<List<ProductGroupNode>>
}