package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.core.result.DomainException
import ir.hasabdari.paydar.core.transaction.TransactionRunner
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.model.InvoiceLine
import ir.hasabdari.paydar.domain.model.InvoiceStatus
import ir.hasabdari.paydar.domain.model.InvoiceType
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import ir.hasabdari.paydar.domain.model.NewPurchaseCommand
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.StockMovementType
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import ir.hasabdari.paydar.domain.repository.InvoiceRepository
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPurchaseInvoicesUseCase @Inject constructor(
    private val repo: InvoiceRepository
) {
    operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): Flow<List<Invoice>> =
        repo.observeByTypes(storeId, listOf(InvoiceType.PURCHASE, InvoiceType.PURCHASE_RETURN))
}

class GetInvoiceDetailUseCase @Inject constructor(
    private val repo: InvoiceRepository
) {
    suspend operator fun invoke(id: String): AppResult<InvoiceWithLines?> = repo.getById(id)
}

class RegisterPurchaseUseCase @Inject constructor(
    private val invoiceRepo: InvoiceRepository,
    private val stockRepo: StockRepository,
    private val warehouseRepo: WarehouseRepository,
    private val cashTxnRepo: CashTransactionRepository,
    private val postJournal: PostJournalEntryUseCase,
    private val ensureFiscalYear: EnsureFiscalYearUseCase,
    private val txRunner: TransactionRunner
) {
    suspend operator fun invoke(cmd: NewPurchaseCommand): AppResult<String> {
        // ═══ اعتبارسنجی ═══
        if (cmd.lines.isEmpty()) {
            return AppResult.Failure(DomainError.EmptyField("اقلام فاکتور"))
        }
        if (cmd.settlementType != SettlementType.CASH && cmd.partyId.isNullOrBlank()) {
            return AppResult.Failure(
                DomainError.InvalidInput("تأمین‌کننده", "برای خرید نسیه باید تأمین‌کننده انتخاب شود")
            )
        }
        if (cmd.settlementType == SettlementType.MIXED && cmd.paidAmountRials <= 0) {
            return AppResult.Failure(
                DomainError.InvalidInput("پرداخت", "برای خرید ترکیبی مبلغ پرداختی الزامی است")
            )
        }

        // ═══ محاسبه مبالغ ═══
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
            taxTotal += (lineNet * ln.taxRate) / 10_000
        }
        val grandTotal = subtotal - cmd.discountTotalRials + taxTotal +
                cmd.shippingCostRials + cmd.otherCostsRials

        if (cmd.paidAmountRials > grandTotal) {
            return AppResult.Failure(
                DomainError.InvalidInput("پرداخت", "بیشتر از جمع فاکتور است")
            )
        }
        if (cmd.settlementType == SettlementType.CASH && cmd.paidAmountRials != grandTotal) {
            return AppResult.Failure(
                DomainError.InvalidInput("پرداخت", "برای خرید نقدی باید برابر جمع باشد")
            )
        }
        if (cmd.settlementType == SettlementType.CREDIT && cmd.paidAmountRials != 0L) {
            return AppResult.Failure(
                DomainError.InvalidInput("پرداخت", "برای خرید نسیه باید صفر باشد")
            )
        }

        // ═══ سال مالی ═══
        val fyResult = ensureFiscalYear(cmd.storeId)
        if (fyResult is AppResult.Failure) return fyResult
        val fySuccess = fyResult as AppResult.Success
        val fy = fySuccess.data
        if (fy == null) return AppResult.Failure(DomainError.Unknown())

        // ═══ انبار ═══
        if (cmd.warehouseId.isBlank()) {
            return AppResult.Failure(DomainError.EmptyField("انبار"))
        }
        val whResult = warehouseRepo.getById(cmd.warehouseId)
        if (whResult is AppResult.Failure) return whResult
        val whSuccess = whResult as AppResult.Success
        val wh = whSuccess.data
        if (wh == null) return AppResult.Failure(DomainError.NotFound("انبار", cmd.warehouseId))

        // ═══ شماره فاکتور ═══
        val numberResult = invoiceRepo.getNextNumber(cmd.storeId, fy.id, InvoiceType.PURCHASE)
        if (numberResult is AppResult.Failure) return numberResult
        val numberSuccess = numberResult as AppResult.Success
        val number = numberSuccess.data

        // ═══ اجرای اتمیک ═══
        return try {
            txRunner.run {
                val invoiceId = java.util.UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                // ─── ۱. ساخت مدل فاکتور ───
                val invoice = Invoice(
                    id = invoiceId,
                    type = InvoiceType.PURCHASE,
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
                    shippingCost = Money.fromRials(cmd.shippingCostRials),
                    otherCosts = Money.fromRials(cmd.otherCostsRials),
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
                        taxRate = ln.taxRate,
                        lineTotal = Money.fromRials(lineNet),
                        costAtSale = null
                    )
                }

                // ─── ۲. ذخیره فاکتور ═──
                when (val saveResult = invoiceRepo.saveInvoiceWithLines(invoice, lines)) {
                    is AppResult.Failure -> throw DomainException(saveResult.error)
                    is AppResult.Success -> Unit
                }

                // ─── ۳. افزایش موجودی ───
                for (ln in cmd.lines) {
                    when (val stockResult = stockRepo.addMovement(
                        productId = ln.productId,
                        warehouseId = cmd.warehouseId,
                        type = StockMovementType.PURCHASE_IN,
                        quantityDeltaScaled = ln.quantityScaled,
                        unitCost = ln.unitPriceRials,
                        referenceType = "INVOICE",
                        referenceId = invoiceId,
                        note = "فاکتور خرید $number",
                        storeId = cmd.storeId
                    )) {
                        is AppResult.Failure -> throw DomainException(stockResult.error)
                        is AppResult.Success -> Unit
                    }
                }

                // ─── ۴. سند حسابداری ───
                val journalDraft = AutoJournalRules.forPurchase(invoice, cmd.storeId)
                when (val jResult = postJournal(journalDraft)) {
                    is AppResult.Failure -> throw DomainException(jResult.error)
                    is AppResult.Success -> Unit
                }

                invoiceId
            }.let { AppResult.Success(it) }
        } catch (e: DomainException) {
            AppResult.Failure(e.error)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }
}