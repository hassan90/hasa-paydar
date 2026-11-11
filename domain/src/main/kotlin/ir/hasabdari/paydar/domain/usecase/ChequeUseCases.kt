package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.core.result.DomainException
import ir.hasabdari.paydar.core.transaction.TransactionRunner
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.ChequeStatus
import ir.hasabdari.paydar.domain.repository.ChequeRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════
// Read use cases
// ═══════════════════════════════════════════════════════════
class GetChequesUseCase @Inject constructor(
    private val repo: ChequeRepository
) {
    operator fun invoke(
        storeId: String = AppConstants.DEFAULT_STORE_ID,
        kind: ChequeKind? = null
    ): Flow<List<Cheque>> = if (kind == null) repo.observeAll(storeId) else repo.observeByKind(storeId, kind)
}

class GetOpenChequesUseCase @Inject constructor(
    private val repo: ChequeRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<Cheque>> =
        repo.observeOpen(storeId)
}

class GetChequesDueSoonUseCase @Inject constructor(
    private val repo: ChequeRepository
) {
    operator fun invoke(
        days: Long = 7,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): Flow<List<Cheque>> {
        val now = System.currentTimeMillis()
        val to = now + days * 86_400_000L
        return repo.observeDueSoon(storeId, now, to)
    }
}

class GetChequeDetailUseCase @Inject constructor(
    private val repo: ChequeRepository
) {
    suspend operator fun invoke(id: String): AppResult<Cheque?> = repo.getById(id)
}

// ═══════════════════════════════════════════════════════════
// Add use case
// ═══════════════════════════════════════════════════════════
class AddChequeUseCase @Inject constructor(
    private val repo: ChequeRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(
        kind: ChequeKind,
        number: String,
        amountRials: Long,
        issueDate: Long,
        dueDate: Long,
        bankName: String? = null,
        branch: String? = null,
        accountHolder: String? = null,
        accountNumber: String? = null,
        partyId: String? = null,
        relatedInvoiceId: String? = null,
        note: String? = null,
        storeId: String = AppConstants.DEFAULT_STORE_ID
    ): AppResult<String> {
        if (number.isBlank()) return AppResult.Failure(DomainError.EmptyField("شماره چک"))
        if (amountRials <= 0) return AppResult.Failure(DomainError.NegativeAmount("مبلغ"))
        if (dueDate < issueDate) return AppResult.Failure(
            DomainError.InvalidInput("تاریخ سررسید", "باید بعد از تاریخ صدور باشد")
        )
        if (partyId.isNullOrBlank()) return AppResult.Failure(
            DomainError.EmptyField("طرف حساب")
        )

        val initialStatus = when (kind) {
            ChequeKind.RECEIVED -> ChequeStatus.IN_SAFE
            ChequeKind.ISSUED -> ChequeStatus.SPENT
        }

        val cheque = Cheque(
            id = UUID.randomUUID().toString(),
            kind = kind,
            number = number.trim(),
            amount = Money.fromRials(amountRials),
            issueDate = issueDate,
            dueDate = dueDate,
            bankName = bankName?.trim()?.takeIf { it.isNotBlank() },
            branch = branch?.trim()?.takeIf { it.isNotBlank() },
            accountHolder = accountHolder?.trim()?.takeIf { it.isNotBlank() },
            accountNumber = accountNumber?.trim()?.takeIf { it.isNotBlank() },
            partyId = partyId,
            status = initialStatus,
            relatedInvoiceId = relatedInvoiceId,
            note = note?.trim()?.takeIf { it.isNotBlank() },
            createdAt = System.currentTimeMillis(),
            storeId = storeId
        )

        return try {
            txRunner.run {
                // ذخیره چک
                when (val r = repo.save(cheque)) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }
                // سند حسابداری اولیه
                val draft = AutoJournalRules.forChequeInitial(cheque, storeId)
                when (val r = postJournal(draft)) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }
                cheque.id
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) {
            AppResult.Failure(e.error)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Status-change use cases
// ═══════════════════════════════════════════════════════════
class ClearChequeUseCase @Inject constructor(
    private val repo: ChequeRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(chequeId: String, storeId: String = AppConstants.DEFAULT_STORE_ID): AppResult<Unit> {
        val cheque = (repo.getById(chequeId) as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.NotFound("چک", chequeId))
        if (cheque.isFinal) return AppResult.Failure(DomainError.InvalidInput("وضعیت", "چک نهایی شده"))
        if (!cheque.isOpen) return AppResult.Failure(DomainError.InvalidInput("وضعیت", "چک قابل وصول نیست"))

        return try {
            txRunner.run {
                when (val r = repo.updateStatus(chequeId, ChequeStatus.CLEARED)) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }
                val cleared = cheque.copy(status = ChequeStatus.CLEARED)
                when (val r = postJournal(AutoJournalRules.forChequeCleared(cleared, storeId))) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }
                Unit
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) { AppResult.Failure(e.error) }
        catch (e: Exception) { AppResult.Failure(DomainError.Unknown(e)) }
    }
}

class BounceChequeUseCase @Inject constructor(
    private val repo: ChequeRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(chequeId: String, storeId: String = AppConstants.DEFAULT_STORE_ID): AppResult<Unit> {
        val cheque = (repo.getById(chequeId) as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.NotFound("چک", chequeId))
        if (cheque.isFinal) return AppResult.Failure(DomainError.InvalidInput("وضعیت", "چک نهایی شده"))

        return try {
            txRunner.run {
                when (val r = repo.updateStatus(chequeId, ChequeStatus.BOUNCED)) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }
                val bounced = cheque.copy(status = ChequeStatus.BOUNCED)
                when (val r = postJournal(AutoJournalRules.forChequeBounced(bounced, storeId))) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }
                Unit
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) { AppResult.Failure(e.error) }
        catch (e: Exception) { AppResult.Failure(DomainError.Unknown(e)) }
    }
}

class DepositChequeUseCase @Inject constructor(
    private val repo: ChequeRepository
) {
    suspend operator fun invoke(chequeId: String): AppResult<Unit> {
        val cheque = (repo.getById(chequeId) as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.NotFound("چک", chequeId))
        if (cheque.kind != ChequeKind.RECEIVED)
            return AppResult.Failure(DomainError.InvalidInput("نوع", "فقط چک دریافتی قابل واریز است"))
        if (cheque.status != ChequeStatus.IN_SAFE)
            return AppResult.Failure(DomainError.InvalidInput("وضعیت", "چک باید نزد صندوق باشد"))
        return repo.updateStatus(chequeId, ChequeStatus.DEPOSITED)
    }
}

class ReturnChequeUseCase @Inject constructor(
    private val repo: ChequeRepository
) {
    suspend operator fun invoke(chequeId: String): AppResult<Unit> {
        val cheque = (repo.getById(chequeId) as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.NotFound("چک", chequeId))
        if (cheque.kind != ChequeKind.RECEIVED)
            return AppResult.Failure(DomainError.InvalidInput("نوع", "فقط چک دریافتی قابل استرداد است"))
        if (cheque.status != ChequeStatus.BOUNCED)
            return AppResult.Failure(DomainError.InvalidInput("وضعیت", "چک باید برگشتی باشد"))
        return repo.updateStatus(chequeId, ChequeStatus.RETURNED)
    }
}