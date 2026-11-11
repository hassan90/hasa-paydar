package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.ProductGroup
import ir.hasabdari.paydar.domain.model.ProductGroupNode
import ir.hasabdari.paydar.domain.repository.ProductGroupRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetProductGroupsUseCase @Inject constructor(
    private val repo: ProductGroupRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<ProductGroup>> =
        repo.observeAll(storeId)
}

class GetProductGroupTreeUseCase @Inject constructor(
    private val repo: ProductGroupRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<ProductGroupNode>> =
        repo.observeTree(storeId)
}

class AddProductGroupUseCase @Inject constructor(
    private val repo: ProductGroupRepository
) {
    suspend operator fun invoke(
        name: String,
        parentId: String? = null,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (name.isBlank()) {
            return AppResult.Failure(DomainError.EmptyField("نام گروه"))
        }
        val group = ProductGroup(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            parentId = parentId,
            storeId = storeId
        )
        return repo.save(group)
    }
}

class DeleteProductGroupUseCase @Inject constructor(
    private val repo: ProductGroupRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repo.delete(id)
}