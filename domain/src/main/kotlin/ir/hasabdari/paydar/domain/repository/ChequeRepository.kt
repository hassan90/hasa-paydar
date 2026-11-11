package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.ChequeStatus
import kotlinx.coroutines.flow.Flow

interface ChequeRepository {
    fun observeAll(storeId: String): Flow<List<Cheque>>
    fun observeByKind(storeId: String, kind: ChequeKind): Flow<List<Cheque>>
    fun observeOpen(storeId: String): Flow<List<Cheque>>
    fun observeDueSoon(storeId: String, fromMillis: Long, toMillis: Long): Flow<List<Cheque>>
    suspend fun getById(id: String): AppResult<Cheque?>
    suspend fun save(cheque: Cheque): AppResult<String>
    suspend fun updateStatus(id: String, status: ChequeStatus): AppResult<Unit>
}