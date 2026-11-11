package ir.hasabdari.paydar.domain.usecase

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
import ir.hasabdari.paydar.domain.model.NewReturnCommand
import ir.hasabdari.paydar.domain.model.ReturnResult
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import ir.hasabdari.paydar.domain.repository.InvoiceRepository
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// ═══════════════════════════════════════════════════════════
// برگشت از فروش
// ═══════════════════════════════════════════════════════════
class RegisterSaleReturnUseCase @Inject constructor(
    private val invoiceRepo: InvoiceRepository,
    private val stockRepo: StockRepository,
    private val warehouseRepo: WarehouseRepository,
    private val cashAccountRepo: CashAccountRepository,
    private val cashTxnRepo: CashTransactionRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val ensureFiscalYear: EnsureFiscalYearUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(cmd: NewReturnCommand): AppResult<ReturnResult> {
        val original = (invoiceRepo.getById(cmd.originalInvoiceId) as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.NotFound("فاکتور", cmd.originalInvoiceId))

        if (original.invoice.type != InvoiceType.SALE)
            return AppResult.Failure(DomainError.InvalidInput("نوع فاکتور", "باید فروش باشد"))
        if (original.invoice.status != InvoiceStatus.CONFIRMED)
            return AppResult.Failure(DomainError.InvalidInput("وضعیت", "فاکتور فعال نیست"))
        if (cmd.lines.isEmpty())
            return AppResult.Failure(DomainError.EmptyField("اقلام برگشتی"))

        // ─── اعتبارسنجی مقدارها ───
        for (rl in cmd.lines) {
            if (rl.quantityScaled <= 0) continue
            val originalLine = original.lines.find { it.id == rl.originalLineId }
                ?: return AppResult.Failure(DomainError.InvalidInput("ردیف", "یافت نشد"))
            val alreadyReturned = (invoiceRepo.getReturnedQuantity(original.invoice.id, rl.productId) as? AppResult.Success)?.data ?: 0L
            val availableToReturn = originalLine.quantityScaled - alreadyReturned
            if (rl.quantityScaled > availableToReturn) {
                return AppResult.Failure(
                    DomainError.InvalidInput(
                        "مقدار «${rl.productName}»",
                        "حداکثر قابل برگشت: ${availableToReturn / 1000}، درخواستی: ${rl.quantityScaled / 1000}"
                    )
                )
            }
        }

        val validLines = cmd.lines.filter { it.quantityScaled > 0 }
        if (validLines.isEmpty())
            return AppResult.Failure(DomainError.InvalidInput("اقلام", "هیچ مقداری برای برگشت وارد نشد"))

        // ─── محاسبه ───
        var subtotal = 0L
        var taxTotal = 0L
        var cogsReversal = 0L
        val lineAmounts = mutableListOf<Pair<InvoiceLine, Long>>() // (originalLine, returnQty)

        for (rl in validLines) {
            val originalLine = original.lines.find { it.id == rl.originalLineId }!!
            val ratio = rl.quantityScaled.toDouble() / originalLine.quantityScaled.toDouble()
            val lineNet = (originalLine.lineTotal.rials * ratio).toLong()
            val lineTax = (lineNet * originalLine.taxRate) / 10_000
            val lineCogs = ((originalLine.costAtSale?.rials ?: 0L) * ratio).toLong()
            subtotal += lineNet
            taxTotal += lineTax
            cogsReversal += lineCogs
            lineAmounts.add(originalLine to rl.quantityScaled)
        }

        val grandTotal = subtotal + taxTotal
        val isCash = original.invoice.settlementType == SettlementType.CASH

        // ─── سال مالی ───
        val fyResult = ensureFiscalYear(cmd.storeId)
        if (fyResult is AppResult.Failure) return fyResult
        val fy = (fyResult as AppResult.Success).data ?: return AppResult.Failure(DomainError.Unknown())

        // ─── صندوق پیش‌فرض برای بازگشت نقدی ───
        val defaultCashbox = if (isCash && grandTotal > 0) {
            cashAccountRepo.observeAll(cmd.storeId).first()
                .firstOrNull { it.kind == CashAccountKind.CASHBOX }
        } else null

        // ─── شماره ───
        val numberResult = invoiceRepo.getNextNumber(cmd.storeId, fy.id, InvoiceType.SALE_RETURN)
        if (numberResult is AppResult.Failure) return numberResult
        val number = (numberResult as AppResult.Success).data

        return try {
            txRunner.run {
                val returnId = java.util.UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val returnInvoice = Invoice(
                    id = returnId,
                    type = InvoiceType.SALE_RETURN,
                    number = number,
                    issueDate = cmd.returnDate,
                    dueDate = null,
                    partyId = original.invoice.partyId,
                    warehouseId = original.invoice.warehouseId,
                    settlementType = original.invoice.settlementType,
                    paidAmount = if (isCash) Money.fromRials(grandTotal) else Money.ZERO,
                    subtotal = Money.fromRials(subtotal),
                    discountTotal = Money.ZERO,
                    taxTotal = Money.fromRials(taxTotal),
                    shippingCost = Money.ZERO,
                    otherCosts = Money.ZERO,
                    grandTotal = Money.fromRials(grandTotal),
                    status = InvoiceStatus.CONFIRMED,
                    parentId = original.invoice.id,
                    notes = cmd.reason ?: "برگشت از فاکتور ${original.invoice.number}",
                    createdBy = "system",
                    createdAt = now,
                    storeId = cmd.storeId,
                    fiscalYearId = fy.id
                )

                val returnLines = validLines.mapIndexed { idx, rl ->
                    val originalLine = original.lines.find { it.id == rl.originalLineId }!!
                    val ratio = rl.quantityScaled.toDouble() / originalLine.quantityScaled.toDouble()
                    val lineNet = (originalLine.lineTotal.rials * ratio).toLong()
                    val lineCogs = ((originalLine.costAtSale?.rials ?: 0L) * ratio).toLong()
                    InvoiceLine(
                        id = java.util.UUID.randomUUID().toString(),
                        invoiceId = returnId,
                        lineNo = idx + 1,
                        productId = rl.productId,
                        description = null,
                        unitId = rl.unitId,
                        quantityScaled = rl.quantityScaled,
                        unitPrice = Money.fromRials(rl.unitPriceRials),
                        discount = Money.ZERO,
                        taxRate = originalLine.taxRate,
                        lineTotal = Money.fromRials(lineNet),
                        costAtSale = Money.fromRials(lineCogs)
                    )
                }

                when (val s = invoiceRepo.saveInvoiceWithLines(returnInvoice, returnLines)) {
                    is AppResult.Failure -> throw DomainException(s.error)
                    is AppResult.Success -> Unit
                }

                // افزایش موجودی (برگشت به انبار)
                for (rl in validLines) {
                    val originalLine = original.lines.find { it.id == rl.originalLineId }!!
                    val ratio = rl.quantityScaled.toDouble() / originalLine.quantityScaled.toDouble()
                    val unitCost = ((originalLine.costAtSale?.rials ?: 0L) * 1000L /
                            originalLine.quantityScaled).toLong()
                    when (val r = stockRepo.addMovement(
                        productId = rl.productId,
                        warehouseId = original.invoice.warehouseId,
                        type = StockMovementType.RETURN_IN,
                        quantityDeltaScaled = rl.quantityScaled,
                        unitCost = unitCost,
                        referenceType = "INVOICE",
                        referenceId = returnId,
                        note = "برگشت از فروش $number",
                        storeId = cmd.storeId
                    )) {
                        is AppResult.Failure -> throw DomainException(r.error)
                        is AppResult.Success -> Unit
                    }
                }

                // سند برگشت فروش
                when (val r = postJournal(
                    AutoJournalRules.forSaleReturn(returnInvoice, cmd.storeId)
                )) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }

                // سند برگشت COGS
                if (cogsReversal > 0) {
                    when (val r = postJournal(
                        AutoJournalRules.forSaleReturnCogs(returnInvoice, cogsReversal, cmd.storeId)
                    )) {
                        is AppResult.Failure -> throw DomainException(r.error)
                        is AppResult.Success -> Unit
                    }
                }

                // پرداخت نقدی به مشتری
                if (isCash && grandTotal > 0 && defaultCashbox != null) {
                    val txn = CashTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        type = CashTransactionType.PAYMENT,
                        method = CashTransactionMethod.CASH,
                        cashAccountId = defaultCashbox.id,
                        toCashAccountId = null,
                        partyId = original.invoice.partyId,
                        amount = Money.fromRials(grandTotal),
                        occurredAt = now,
                        description = "برگشت وجه فاکتور $number",
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

                ReturnResult(
                    returnInvoiceId = returnId,
                    returnInvoiceNumber = number,
                    totalRefundRials = grandTotal
                )
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) {
            AppResult.Failure(e.error)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// برگشت از خرید
// ═══════════════════════════════════════════════════════════
class RegisterPurchaseReturnUseCase @Inject constructor(
    private val invoiceRepo: InvoiceRepository,
    private val stockRepo: StockRepository,
    private val cashAccountRepo: CashAccountRepository,
    private val cashTxnRepo: CashTransactionRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val ensureFiscalYear: EnsureFiscalYearUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(cmd: NewReturnCommand): AppResult<ReturnResult> {
        val original = (invoiceRepo.getById(cmd.originalInvoiceId) as? AppResult.Success)?.data
            ?: return AppResult.Failure(DomainError.NotFound("فاکتور", cmd.originalInvoiceId))

        if (original.invoice.type != InvoiceType.PURCHASE)
            return AppResult.Failure(DomainError.InvalidInput("نوع فاکتور", "باید خرید باشد"))
        if (original.invoice.status != InvoiceStatus.CONFIRMED)
            return AppResult.Failure(DomainError.InvalidInput("وضعیت", "فاکتور فعال نیست"))
        if (cmd.lines.isEmpty())
            return AppResult.Failure(DomainError.EmptyField("اقلام برگشتی"))

        for (rl in cmd.lines) {
            if (rl.quantityScaled <= 0) continue
            val originalLine = original.lines.find { it.id == rl.originalLineId }
                ?: return AppResult.Failure(DomainError.InvalidInput("ردیف", "یافت نشد"))
            val alreadyReturned = (invoiceRepo.getReturnedQuantity(original.invoice.id, rl.productId) as? AppResult.Success)?.data ?: 0L
            val availableToReturn = originalLine.quantityScaled - alreadyReturned
            if (rl.quantityScaled > availableToReturn) {
                return AppResult.Failure(
                    DomainError.InvalidInput(
                        "مقدار «${rl.productName}»",
                        "حداکثر قابل برگشت: ${availableToReturn / 1000}"
                    )
                )
            }
        }

        val validLines = cmd.lines.filter { it.quantityScaled > 0 }
        if (validLines.isEmpty())
            return AppResult.Failure(DomainError.InvalidInput("اقلام", "هیچ مقداری وارد نشد"))

        var subtotal = 0L
        var taxTotal = 0L

        for (rl in validLines) {
            val originalLine = original.lines.find { it.id == rl.originalLineId }!!
            val ratio = rl.quantityScaled.toDouble() / originalLine.quantityScaled.toDouble()
            val lineNet = (originalLine.lineTotal.rials * ratio).toLong()
            val lineTax = (lineNet * originalLine.taxRate) / 10_000
            subtotal += lineNet
            taxTotal += lineTax
        }

        val grandTotal = subtotal + taxTotal
        val isCash = original.invoice.settlementType == SettlementType.CASH

        val fyResult = ensureFiscalYear(cmd.storeId)
        if (fyResult is AppResult.Failure) return fyResult
        val fy = (fyResult as AppResult.Success).data ?: return AppResult.Failure(DomainError.Unknown())

        val defaultCashbox = if (isCash && grandTotal > 0) {
            cashAccountRepo.observeAll(cmd.storeId).first()
                .firstOrNull { it.kind == CashAccountKind.CASHBOX }
        } else null

        val numberResult = invoiceRepo.getNextNumber(cmd.storeId, fy.id, InvoiceType.PURCHASE_RETURN)
        if (numberResult is AppResult.Failure) return numberResult
        val number = (numberResult as AppResult.Success).data

        return try {
            txRunner.run {
                val returnId = java.util.UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val returnInvoice = Invoice(
                    id = returnId,
                    type = InvoiceType.PURCHASE_RETURN,
                    number = number,
                    issueDate = cmd.returnDate,
                    dueDate = null,
                    partyId = original.invoice.partyId,
                    warehouseId = original.invoice.warehouseId,
                    settlementType = original.invoice.settlementType,
                    paidAmount = if (isCash) Money.fromRials(grandTotal) else Money.ZERO,
                    subtotal = Money.fromRials(subtotal),
                    discountTotal = Money.ZERO,
                    taxTotal = Money.fromRials(taxTotal),
                    shippingCost = Money.ZERO,
                    otherCosts = Money.ZERO,
                    grandTotal = Money.fromRials(grandTotal),
                    status = InvoiceStatus.CONFIRMED,
                    parentId = original.invoice.id,
                    notes = cmd.reason ?: "برگشت به فاکتور ${original.invoice.number}",
                    createdBy = "system",
                    createdAt = now,
                    storeId = cmd.storeId,
                    fiscalYearId = fy.id
                )

                val returnLines = validLines.mapIndexed { idx, rl ->
                    val originalLine = original.lines.find { it.id == rl.originalLineId }!!
                    val ratio = rl.quantityScaled.toDouble() / originalLine.quantityScaled.toDouble()
                    val lineNet = (originalLine.lineTotal.rials * ratio).toLong()
                    InvoiceLine(
                        id = java.util.UUID.randomUUID().toString(),
                        invoiceId = returnId,
                        lineNo = idx + 1,
                        productId = rl.productId,
                        description = null,
                        unitId = rl.unitId,
                        quantityScaled = rl.quantityScaled,
                        unitPrice = Money.fromRials(rl.unitPriceRials),
                        discount = Money.ZERO,
                        taxRate = originalLine.taxRate,
                        lineTotal = Money.fromRials(lineNet),
                        costAtSale = null
                    )
                }

                when (val s = invoiceRepo.saveInvoiceWithLines(returnInvoice, returnLines)) {
                    is AppResult.Failure -> throw DomainException(s.error)
                    is AppResult.Success -> Unit
                }

                // کاهش موجودی (خروج به تأمین‌کننده)
                for (rl in validLines) {
                    val originalLine = original.lines.find { it.id == rl.originalLineId }!!
                    val ratio = rl.quantityScaled.toDouble() / originalLine.quantityScaled.toDouble()
                    val unitCost = ((originalLine.lineTotal.rials / originalLine.quantityScaled) * 1000)
                    when (val r = stockRepo.addMovement(
                        productId = rl.productId,
                        warehouseId = original.invoice.warehouseId,
                        type = StockMovementType.RETURN_OUT,
                        quantityDeltaScaled = -rl.quantityScaled,
                        unitCost = unitCost,
                        referenceType = "INVOICE",
                        referenceId = returnId,
                        note = "برگشت به خرید $number",
                        storeId = cmd.storeId
                    )) {
                        is AppResult.Failure -> throw DomainException(r.error)
                        is AppResult.Success -> Unit
                    }
                }

                when (val r = postJournal(
                    AutoJournalRules.forPurchaseReturn(returnInvoice, cmd.storeId)
                )) {
                    is AppResult.Failure -> throw DomainException(r.error)
                    is AppResult.Success -> Unit
                }

                // دریافت نقدی از تأمین‌کننده
                if (isCash && grandTotal > 0 && defaultCashbox != null) {
                    val txn = CashTransaction(
                        id = java.util.UUID.randomUUID().toString(),
                        type = CashTransactionType.RECEIPT,
                        method = CashTransactionMethod.CASH,
                        cashAccountId = defaultCashbox.id,
                        toCashAccountId = null,
                        partyId = original.invoice.partyId,
                        amount = Money.fromRials(grandTotal),
                        occurredAt = now,
                        description = "بازگشت وجه فاکتور $number",
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

                ReturnResult(
                    returnInvoiceId = returnId,
                    returnInvoiceNumber = number,
                    totalRefundRials = grandTotal
                )
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) {
            AppResult.Failure(e.error)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }
}