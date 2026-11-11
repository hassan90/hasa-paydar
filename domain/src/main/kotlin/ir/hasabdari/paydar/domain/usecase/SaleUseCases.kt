package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.core.result.DomainException
import ir.hasabdari.paydar.core.transaction.TransactionRunner
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.domain.model.CashTransaction
import ir.hasabdari.paydar.domain.model.CashTransactionMethod
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.model.InvoiceLine
import ir.hasabdari.paydar.domain.model.InvoiceStatus
import ir.hasabdari.paydar.domain.model.InvoiceType
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import ir.hasabdari.paydar.domain.model.NewSaleCommand
import ir.hasabdari.paydar.domain.model.SaleResult
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import ir.hasabdari.paydar.domain.repository.InvoiceRepository
import ir.hasabdari.paydar.domain.repository.PartyRepository
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first          // ← import لازم
import javax.inject.Inject

class GetSaleInvoicesUseCase @Inject constructor(
    private val repo: InvoiceRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<Invoice>> =
        repo.observeByTypes(storeId, listOf(InvoiceType.SALE, InvoiceType.SALE_RETURN))
}

class GetSaleDetailUseCase @Inject constructor(
    private val repo: InvoiceRepository
) {
    suspend operator fun invoke(id: String): AppResult<InvoiceWithLines?> = repo.getById(id)
}

class RegisterSaleUseCase @Inject constructor(
    private val invoiceRepo: InvoiceRepository,
    private val stockRepo: StockRepository,
    private val warehouseRepo: WarehouseRepository,
    private val partyRepo: PartyRepository,
    private val cashAccountRepo: CashAccountRepository,
    private val cashTxnRepo: CashTransactionRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val ensureFiscalYear: EnsureFiscalYearUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(cmd: NewSaleCommand): AppResult<SaleResult> {
        // ═══ اعتبارسنجی ═══
        if (cmd.lines.isEmpty()) {
            return AppResult.Failure(DomainError.EmptyField("اقلام فاکتور"))
        }
        if (cmd.settlementType != SettlementType.CASH && cmd.partyId.isNullOrBlank()) {
            return AppResult.Failure(
                DomainError.InvalidInput("مشتری", "برای فروش نسیه باید مشتری انتخاب شود")
            )
        }

        var subtotal = 0L
        var taxTotal = 0L
        for (ln in cmd.lines) {
            if (ln.quantityScaled <= 0) {
                return AppResult.Failure(
                    DomainError.InvalidInput("تعداد «${ln.productName}»", "باید مثبت باشد")
                )
            }
            if (ln.unitPriceRials < 0) {
                return AppResult.Failure(DomainError.NegativeAmount("قیمت «${ln.productName}»"))
            }
            val lineGross = (ln.unitPriceRials * ln.quantityScaled) / 1000
            val lineNet = lineGross - ln.discountRials
            subtotal += lineGross
            taxTotal += (lineNet * cmd.taxRate) / 10_000
        }
        val grandTotal = subtotal - cmd.discountTotalRials + taxTotal

        if (cmd.paidAmountRials > grandTotal) {
            return AppResult.Failure(DomainError.InvalidInput("پرداخت", "بیشتر از جمع فاکتور است"))
        }
        if (cmd.settlementType == SettlementType.CASH && cmd.paidAmountRials != grandTotal) {
            return AppResult.Failure(DomainError.InvalidInput("پرداخت", "برای فروش نقدی باید برابر جمع باشد"))
        }
        if (cmd.settlementType == SettlementType.CREDIT && cmd.paidAmountRials != 0L) {
            return AppResult.Failure(DomainError.InvalidInput("پرداخت", "برای فروش نسیه باید صفر باشد"))
        }

        // ═══ سال مالی ═══
        val fyResult = ensureFiscalYear(cmd.storeId)
        if (fyResult is AppResult.Failure) return fyResult
        val fy = (fyResult as AppResult.Success).data
            ?: return AppResult.Failure(DomainError.Unknown())

        // ═══ انبار ═══
        if (cmd.warehouseId.isBlank()) return AppResult.Failure(DomainError.EmptyField("انبار"))
        val whResult = warehouseRepo.getById(cmd.warehouseId)
        if (whResult is AppResult.Failure) return whResult
        val wh = (whResult as AppResult.Success).data
            ?: return AppResult.Failure(DomainError.NotFound("انبار", cmd.warehouseId))

        // ═══ بررسی موجودی + محاسبه COGS ═══
        val cogsPerLine = mutableListOf<Long>()
        var cogsTotal = 0L
        for (ln in cmd.lines) {
            val stockResult = stockRepo.getItem(ln.productId, cmd.warehouseId)
            if (stockResult is AppResult.Failure) return stockResult
            val stockItem = (stockResult as AppResult.Success).data
            val available = stockItem?.quantityScaled ?: 0L
            if (available < ln.quantityScaled) {
                return AppResult.Failure(
                    DomainError.InsufficientStock(
                        productName = ln.productName,
                        requested = ln.quantityScaled,
                        available = available
                    )
                )
            }
            val avgCost = stockItem?.avgCost?.rials ?: 0L
            val lineCogs = (avgCost * ln.quantityScaled) / 1000
            cogsPerLine.add(lineCogs)
            cogsTotal += lineCogs
        }

        // ═══ بررسی سقف اعتبار ═══
        if (cmd.settlementType != SettlementType.CASH && cmd.partyId != null) {
            val partyResult = partyRepo.getById(cmd.partyId)
            if (partyResult is AppResult.Failure) return partyResult
            val party = (partyResult as AppResult.Success).data
            if (party != null && party.creditLimit.rials > 0) {
                val outstandingResult = invoiceRepo.getPartyOutstandingBalance(cmd.partyId, cmd.storeId)
                if (outstandingResult is AppResult.Failure) return outstandingResult
                val outstanding = (outstandingResult as AppResult.Success).data
                val newBalance = outstanding + (grandTotal - cmd.paidAmountRials)
                if (newBalance > party.creditLimit.rials) {
                    return AppResult.Failure(
                        DomainError.CreditLimitExceeded(
                            partyName = party.name,
                            currentDebt = outstanding,
                            creditLimit = party.creditLimit.rials
                        )
                    )
                }
            }
        }

        // ═══ شماره فاکتور ═══
        val numberResult = invoiceRepo.getNextNumber(cmd.storeId, fy.id, InvoiceType.SALE)
        if (numberResult is AppResult.Failure) return numberResult
        val number = (numberResult as AppResult.Success).data

        // ═══ صندوق پیش‌فرض (اگر نقدی) — قبل از تراکنش بگیریم ═══
        val defaultCashbox = if (cmd.paidAmountRials > 0) {
            cashAccountRepo.observeAll(cmd.storeId).first()
                .firstOrNull { it.kind == CashAccountKind.CASHBOX }
        } else null

        // ═══ اجرای اتمیک ═══
        return try {
            txRunner.run {
                val invoiceId = java.util.UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val invoice = Invoice(
                    id = invoiceId,
                    type = InvoiceType.SALE,
                    number = number,
                    issueDate = cmd.issueDate,
                    dueDate = null,
                    partyId = cmd.partyId,
                    warehouseId = cmd.warehouseId,
                    settlementType = cmd.settlementType,
                    paidAmount = Money.fromRials(cmd.paidAmountRials),
                    subtotal = Money.fromRials(subtotal),
                    discountTotal = Money.fromRials(cmd.discountTotalRials),
                    taxTotal = Money.fromRials(taxTotal),
                    shippingCost = Money.ZERO,
                    otherCosts = Money.ZERO,
                    grandTotal = Money.fromRials(grandTotal),
                    status = InvoiceStatus.CONFIRMED,
                    parentId = null,
                    notes = cmd.notes,
                    createdBy = "system",
                    createdAt = now,
                    storeId = cmd.storeId,
                    fiscalYearId = fy.id
                )

                val lines = cmd.lines.mapIndexed { idx, ln ->
                    val lineGross = (ln.unitPriceRials * ln.quantityScaled) / 1000
                    val lineNet = lineGross - ln.discountRials
                    InvoiceLine(
                        id = java.util.UUID.randomUUID().toString(),
                        invoiceId = invoiceId,
                        lineNo = idx + 1,
                        productId = ln.productId,
                        description = null,
                        unitId = ln.unitId,
                        quantityScaled = ln.quantityScaled,
                        unitPrice = Money.fromRials(ln.unitPriceRials),
                        discount = Money.fromRials(ln.discountRials),
                        taxRate = cmd.taxRate,
                        lineTotal = Money.fromRials(lineNet),
                        costAtSale = Money.fromRials(cogsPerLine[idx])
                    )
                }

                // ۱. ذخیره فاکتور
                when (val saveResult = invoiceRepo.saveInvoiceWithLines(invoice, lines)) {
                    is AppResult.Failure -> throw DomainException(saveResult.error)
                    is AppResult.Success -> Unit
                }

                // ۲. کاهش موجودی
                for (ln in cmd.lines) {
                    when (val r = stockRepo.addMovement(
                        productId = ln.productId,
                        warehouseId = cmd.warehouseId,
                        type = StockMovementType.SALE_OUT,
                        quantityDeltaScaled = -ln.quantityScaled,
                        unitCost = ln.unitPriceRials,
                        referenceType = "INVOICE",
                        referenceId = invoiceId,
                        note = "فاکتور فروش $number",
                        storeId = cmd.storeId
                    )) {
                        is AppResult.Failure -> throw DomainException(r.error)
                        is AppResult.Success -> Unit
                    }
                }

                // ۳. سند فروش
                when (val jr = postJournal(AutoJournalRules.forSaleInvoice(invoice, cmd.storeId))) {
                    is AppResult.Failure -> throw DomainException(jr.error)
                    is AppResult.Success -> Unit
                }

                // ۴. سند COGS
                if (cogsTotal > 0) {
                    when (val jr = postJournal(AutoJournalRules.forSaleCogs(invoice, cogsTotal, cmd.storeId))) {
                        is AppResult.Failure -> throw DomainException(jr.error)
                        is AppResult.Success -> Unit
                    }
                }

                // ۵. CashTransaction برای دریافت نقدی
                if (cmd.paidAmountRials > 0 && defaultCashbox != null) {
                    val txn = CashTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        type = CashTransactionType.RECEIPT,
                        method = CashTransactionMethod.CASH,
                        cashAccountId = defaultCashbox.id,
                        toCashAccountId = null,
                        partyId = cmd.partyId,
                        amount = Money.fromRials(cmd.paidAmountRials),
                        occurredAt = now,
                        description = "دریافت فروش فاکتور $number",
                        storeId = cmd.storeId
                    )
                    when (val r = cashTxnRepo.save(txn)) {
                        is AppResult.Failure -> throw DomainException(r.error)
                        is AppResult.Success -> Unit
                    }
                    when (val r = postJournal(
                        AutoJournalRules.forCashTransaction(txn, defaultCashbox, null, cmd.storeId)
                    )) {
                        is AppResult.Failure -> throw DomainException(r.error)
                        is AppResult.Success -> Unit
                    }
                }

                SaleResult(
                    invoiceId = invoiceId,
                    invoiceNumber = number,
                    grandTotalRials = grandTotal,
                    cogsTotalRials = cogsTotal,
                    profitRials = (subtotal - cmd.discountTotalRials) - cogsTotal
                )
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) {
            AppResult.Failure(e.error)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }
}