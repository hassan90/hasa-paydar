package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.domain.model.CashAccountWithBalance
import ir.hasabdari.paydar.domain.model.CashTransaction
import ir.hasabdari.paydar.domain.model.CashTransactionMethod
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.TreasuryOverview
import ir.hasabdari.paydar.domain.model.TreasuryTransactionRow
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import ir.hasabdari.paydar.domain.repository.PartyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

// ═══════════════════════════════════════════
// حساب‌های نقدی
// ═══════════════════════════════════════════

class GetCashAccountsUseCase @Inject constructor(
    private val repo: CashAccountRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<CashAccount>> =
        repo.observeAll(storeId)
}

class AddCashAccountUseCase @Inject constructor(
    private val repo: CashAccountRepository
) {
    suspend operator fun invoke(
        kind: CashAccountKind,
        name: String,
        bankName: String? = null,
        accountNumber: String? = null,
        openingBalanceRials: Long = 0,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (name.isBlank()) return AppResult.Failure(DomainError.EmptyField("نام حساب"))
        if (kind == CashAccountKind.BANK && bankName.isNullOrBlank())
            return AppResult.Failure(DomainError.EmptyField("نام بانک"))
        if (openingBalanceRials < 0)
            return AppResult.Failure(DomainError.NegativeAmount("موجودی اولیه"))

        return repo.save(
            CashAccount(
                id = UUID.randomUUID().toString(),
                kind = kind,
                name = name.trim(),
                bankName = bankName?.trim()?.takeIf { it.isNotBlank() },
                accountNumber = accountNumber?.trim()?.takeIf { it.isNotBlank() },
                openingBalance = Money.fromRials(openingBalanceRials),
                storeId = storeId
            )
        )
    }
}

class DeleteCashAccountUseCase @Inject constructor(
    private val repo: CashAccountRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repo.delete(id)
}

// ═══════════════════════════════════════════
// دریافت / پرداخت / انتقال
// ═══════════════════════════════════════════

class AddCashTransactionUseCase @Inject constructor(
    private val txnRepo: CashTransactionRepository,
    private val accountRepo: CashAccountRepository,
    private val postJournal: PostJournalEntryUseCase
) {
    suspend operator fun invoke(
        type: CashTransactionType,
        method: CashTransactionMethod,
        cashAccountId: String,
        toCashAccountId: String? = null,
        partyId: String? = null,
        amountRials: Long,
        description: String? = null,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        // ═══ اعتبارسنجی ═══
        if (amountRials <= 0) return AppResult.Failure(DomainError.NegativeAmount("مبلغ"))
        if (cashAccountId.isBlank()) return AppResult.Failure(DomainError.EmptyField("حساب"))
        if (type == CashTransactionType.TRANSFER) {
            if (toCashAccountId.isNullOrBlank())
                return AppResult.Failure(DomainError.EmptyField("حساب مقصد"))
            if (cashAccountId == toCashAccountId)
                return AppResult.Failure(DomainError.InvalidInput("حساب مقصد", "نباید با مبدأ یکی باشد"))
        }

        // ═══ بررسی موجودی ═══
        if (type == CashTransactionType.PAYMENT || type == CashTransactionType.TRANSFER) {
            val balanceRes = txnRepo.getBalance(cashAccountId)
            if (balanceRes is AppResult.Success) {
                val current = balanceRes.data
                if (current < amountRials) {
                    val account = (accountRepo.getById(cashAccountId) as? AppResult.Success)?.data
                    val name = account?.name ?: "حساب"
                    return AppResult.Failure(
                        DomainError.InvalidInput(
                            "موجودی",
                            "موجودی $name کافی نیست. موجودی: ${current / 10} تومان، درخواستی: ${amountRials / 10} تومان"
                        )
                    )
                }
            }
        }

        // ═══ ساخت تراکنش ═══
        val txn = CashTransaction(
            id = java.util.UUID.randomUUID().toString(),
            type = type,
            method = method,
            cashAccountId = cashAccountId,
            toCashAccountId = toCashAccountId,
            partyId = partyId,
            amount = ir.hasabdari.paydar.core.money.Money.fromRials(amountRials),
            occurredAt = System.currentTimeMillis(),
            description = description?.trim()?.takeIf { it.isNotBlank() },
            storeId = storeId
        )

        // ═══ ذخیره تراکنش ═══
        val saveResult = txnRepo.save(txn)
        if (saveResult is AppResult.Failure) return saveResult

        // ═══ ثبت سند حسابداری ═══
        val srcAccount = (accountRepo.getById(cashAccountId) as? AppResult.Success)?.data
        val dstAccount = toCashAccountId?.let { (accountRepo.getById(it) as? AppResult.Success)?.data }

        if (srcAccount != null) {
            val journalDraft = AutoJournalRules.forCashTransaction(txn, srcAccount, dstAccount, storeId)
            val journalResult = postJournal(journalDraft)

            if (journalResult is AppResult.Failure) {
                // Rollback: پاک کردن تراکنش چون سند ثبت نشد
                txnRepo.delete(txn.id)
                return journalResult
            }
        }

        return AppResult.Success(txn.id)
    }
}

class DeleteCashTransactionUseCase @Inject constructor(
    private val repo: CashTransactionRepository
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repo.delete(id)
}

// ═══════════════════════════════════════════
// نمای کلی خزانه
// ═══════════════════════════════════════════

class GetTreasuryOverviewUseCase @Inject constructor(
    private val accountRepo: CashAccountRepository,
    private val txnRepo: CashTransactionRepository,
    private val partyRepo: PartyRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<TreasuryOverview> =
        combine(
            accountRepo.observeAll(storeId),
            txnRepo.observeAll(storeId)
        ) { accounts, txns ->
            val partyMap = partyRepo.observeAll(storeId).first().associateBy { it.id }

            // محاسبه موجودی هر حساب
            val withBalance = accounts.map { acc ->
                var bal = acc.openingBalance.rials
                txns.forEach { t ->
                    when {
                        t.type == CashTransactionType.RECEIPT && t.cashAccountId == acc.id -> bal += t.amount.rials
                        t.type == CashTransactionType.PAYMENT && t.cashAccountId == acc.id -> bal -= t.amount.rials
                        t.type == CashTransactionType.TRANSFER && t.cashAccountId == acc.id -> bal -= t.amount.rials
                        t.type == CashTransactionType.TRANSFER && t.toCashAccountId == acc.id -> bal += t.amount.rials
                    }
                }
                CashAccountWithBalance(acc, Money.fromRials(bal))
            }

            val cashboxes = withBalance.filter { it.account.kind == CashAccountKind.CASHBOX }
            val banks = withBalance.filter { it.account.kind == CashAccountKind.BANK }
            val total = withBalance.sumOf { it.currentBalance.rials }

            val recent = txns.sortedByDescending { it.occurredAt }.take(5).map { t ->
                val acc = accounts.find { it.id == t.cashAccountId }
                TreasuryTransactionRow(
                    id = t.id,
                    type = t.type,
                    accountName = acc?.name ?: "—",
                    partyName = t.partyId?.let { partyMap[it]?.name },
                    amount = t.amount,
                    occurredAt = t.occurredAt,
                    description = t.description
                )
            }

            TreasuryOverview(
                totalBalance = Money.fromRials(total),
                cashboxes = cashboxes,
                banks = banks,
                recentTransactions = recent
            )
        }

    // For empty-txn fallback, we need an alternative overload that doesn't suspend
    private fun cashBalanceFor(account: CashAccount, txns: List<CashTransaction>): Long {
        var bal = account.openingBalance.rials
        txns.forEach { t ->
            when {
                t.type == CashTransactionType.RECEIPT && t.cashAccountId == account.id -> bal += t.amount.rials
                t.type == CashTransactionType.PAYMENT && t.cashAccountId == account.id -> bal -= t.amount.rials
                t.type == CashTransactionType.TRANSFER && t.cashAccountId == account.id -> bal -= t.amount.rials
                t.type == CashTransactionType.TRANSFER && t.toCashAccountId == account.id -> bal += t.amount.rials
            }
        }
        return bal
    }
}