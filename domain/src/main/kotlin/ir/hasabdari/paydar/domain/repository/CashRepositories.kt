package ir.hasabdari.paydar.domain.repository

import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashTransaction
import kotlinx.coroutines.flow.Flow

interface CashAccountRepository {
    fun observeAll(storeId: String): Flow<List<CashAccount>>
    suspend fun getById(id: String): AppResult<CashAccount?>
    suspend fun save(account: CashAccount): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>
}

interface CashTransactionRepository {
    fun observeAll(storeId: String): Flow<List<CashTransaction>>
    fun observeByAccount(accountId: String, storeId: String): Flow<List<CashTransaction>>
    suspend fun getById(id: String): AppResult<CashTransaction?>
    suspend fun save(transaction: CashTransaction): AppResult<String>
    suspend fun delete(id: String): AppResult<Unit>

    /** محاسبه موجودی فعلی هر حساب */
    suspend fun getBalance(accountId: String): AppResult<Long>
}