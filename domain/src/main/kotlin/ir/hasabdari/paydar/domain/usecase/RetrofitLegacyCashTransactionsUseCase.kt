package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * برای تراکنش‌های F3 که قبل از F4 ثبت شده‌اند، سند حسابداری می‌سازد.
 * به لطف PostJournalEntryUseCase (idempotent)، این متد اجرای مکرر مشکلی ندارد.
 */
class RetrofitLegacyCashTransactionsUseCase @Inject constructor(
    private val txnRepo: CashTransactionRepository,
    private val accountRepo: CashAccountRepository,
    private val postJournal: PostJournalEntryUseCase
) {
    suspend operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): AppResult<Int> {
        val accounts = accountRepo.observeAll(storeId).first().associateBy { it.id }
        val txns = txnRepo.observeAll(storeId).first()
        var posted = 0

        for (txn in txns) {
            val src = accounts[txn.cashAccountId] ?: continue
            val dst = txn.toCashAccountId?.let { accounts[it] }

            val draft = AutoJournalRules.forCashTransaction(txn, src, dst, storeId)
            val result = postJournal(draft)
            if (result is AppResult.Success) posted++
        }

        return AppResult.Success(posted)
    }
}