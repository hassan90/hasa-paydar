package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.domain.accounting.ChartOfAccounts
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.domain.model.CashTransaction
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.model.JournalDraft
import ir.hasabdari.paydar.domain.model.JournalLineDraft
import ir.hasabdari.paydar.domain.model.JournalSourceType

/**
 * قواعد ثبت خودکار سند حسابداری از عملیات مختلف.
 *
 * فاز F4: دریافت/پرداخت/انتقال نقدی
 * فاز F5: فاکتور خرید
 * فاز F6: فاکتور فروش + COGS
 * فاز F8: برگشت از فروش + برگشت از خرید
 * فاز F9: چک (دریافتی/پرداختی/وصول/برگشت)
 */
object AutoJournalRules {

    // ═══════════════════════════════════════════════════════════
    // F4 — تراکنش‌های نقدی (دریافت/پرداخت/انتقال)
    // ═══════════════════════════════════════════════════════════
    fun forCashTransaction(
        txn: CashTransaction,
        sourceAccount: CashAccount,
        destAccount: CashAccount?,
        storeId: String
    ): JournalDraft {
        val sourceGl = if (sourceAccount.kind == CashAccountKind.CASHBOX)
            ChartOfAccounts.CASHBOX else ChartOfAccounts.BANKS

        val lines = mutableListOf<JournalLineDraft>()

        when (txn.type) {
            CashTransactionType.RECEIPT -> {
                lines.add(JournalLineDraft(accountCode = sourceGl, debit = txn.amount.rials))
                if (txn.partyId != null) {
                    lines.add(JournalLineDraft(
                        accountCode = ChartOfAccounts.ACCOUNTS_RECEIVABLE_CUSTOMERS,
                        partyId = txn.partyId,
                        credit = txn.amount.rials
                    ))
                } else {
                    lines.add(JournalLineDraft(
                        accountCode = ChartOfAccounts.MISC_REVENUE,
                        credit = txn.amount.rials
                    ))
                }
            }
            CashTransactionType.PAYMENT -> {
                if (txn.partyId != null) {
                    lines.add(JournalLineDraft(
                        accountCode = ChartOfAccounts.ACCOUNTS_PAYABLE_SUPPLIERS,
                        partyId = txn.partyId,
                        debit = txn.amount.rials
                    ))
                } else {
                    lines.add(JournalLineDraft(
                        accountCode = ChartOfAccounts.MISC_ADMIN_EXPENSE,
                        debit = txn.amount.rials
                    ))
                }
                lines.add(JournalLineDraft(accountCode = sourceGl, credit = txn.amount.rials))
            }
            CashTransactionType.TRANSFER -> {
                val destGl = when (destAccount?.kind) {
                    CashAccountKind.CASHBOX -> ChartOfAccounts.CASHBOX
                    CashAccountKind.BANK -> ChartOfAccounts.BANKS
                    null -> ChartOfAccounts.BANKS
                }
                lines.add(JournalLineDraft(accountCode = destGl, debit = txn.amount.rials))
                lines.add(JournalLineDraft(accountCode = sourceGl, credit = txn.amount.rials))
            }
        }

        return JournalDraft(
            entryDate = txn.occurredAt,
            description = txn.description ?: when (txn.type) {
                CashTransactionType.RECEIPT -> "دریافت وجه"
                CashTransactionType.PAYMENT -> "پرداخت"
                CashTransactionType.TRANSFER -> "انتقال بین حساب‌ها"
            },
            sourceType = when (txn.type) {
                CashTransactionType.RECEIPT -> JournalSourceType.CASH_RECEIPT
                CashTransactionType.PAYMENT -> JournalSourceType.CASH_PAYMENT
                CashTransactionType.TRANSFER -> JournalSourceType.CASH_TRANSFER
            },
            sourceId = txn.id,
            lines = lines,
            storeId = storeId
        )
    }

    // ═══════════════════════════════════════════════════════════
    // F5 — فاکتور خرید
    // ═══════════════════════════════════════════════════════════
    /**
     * Dr: موجودی کالا (subtotal − discount + shipping + other)
     * Dr: مالیات خرید (VAT قابل اعتبار)
     * Cr: صندوق/بانک (اگر نقدی)
     * Cr: حساب پرداختنی تأمین‌کننده (مانده)
     */
    fun forPurchase(invoice: Invoice, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        val inventoryCost = invoice.subtotal.rials - invoice.discountTotal.rials +
                invoice.shippingCost.rials + invoice.otherCosts.rials
        if (inventoryCost > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.INVENTORY,
                debit = inventoryCost
            ))
        }
        if (invoice.taxTotal.rials > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.VAT_PAYABLE,
                debit = invoice.taxTotal.rials
            ))
        }
        if (invoice.paidAmount.rials > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CASHBOX,
                credit = invoice.paidAmount.rials
            ))
        }
        val remaining = invoice.grandTotal.rials - invoice.paidAmount.rials
        if (remaining > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.ACCOUNTS_PAYABLE_SUPPLIERS,
                partyId = invoice.partyId,
                credit = remaining
            ))
        }

        return JournalDraft(
            entryDate = invoice.issueDate,
            description = "فاکتور خرید ${invoice.number}",
            sourceType = JournalSourceType.INVOICE_PURCHASE,
            sourceId = invoice.id,
            lines = lines,
            storeId = storeId
        )
    }

    // ═══════════════════════════════════════════════════════════
    // F6 — فاکتور فروش
    // ═══════════════════════════════════════════════════════════
    /**
     * Dr: حساب دریافتنی مشتری (کل مبلغ)
     * Cr: فروش کالا (خالص)
     * Cr: مالیات فروش
     */
    fun forSaleInvoice(invoice: Invoice, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        lines.add(JournalLineDraft(
            accountCode = ChartOfAccounts.ACCOUNTS_RECEIVABLE_CUSTOMERS,
            partyId = invoice.partyId,
            debit = invoice.grandTotal.rials
        ))

        val netSales = invoice.subtotal.rials - invoice.discountTotal.rials
        if (netSales > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.SALES_GOODS,
                credit = netSales
            ))
        }
        if (invoice.taxTotal.rials > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.VAT_PAYABLE,
                credit = invoice.taxTotal.rials
            ))
        }

        return JournalDraft(
            entryDate = invoice.issueDate,
            description = "فاکتور فروش ${invoice.number}",
            sourceType = JournalSourceType.INVOICE_SALE,
            sourceId = invoice.id,
            lines = lines,
            storeId = storeId
        )
    }

    // ═══════════════════════════════════════════════════════════
    // F6 — COGS فروش
    // ═══════════════════════════════════════════════════════════
    /**
     * Dr: بهای تمام‌شده کالای فروش‌رفته
     * Cr: موجودی کالا
     */
    fun forSaleCogs(invoice: Invoice, totalCogsRials: Long, storeId: String): JournalDraft {
        val lines = listOf(
            JournalLineDraft(
                accountCode = ChartOfAccounts.COGS,
                debit = totalCogsRials
            ),
            JournalLineDraft(
                accountCode = ChartOfAccounts.INVENTORY,
                credit = totalCogsRials
            )
        )

        return JournalDraft(
            entryDate = invoice.issueDate,
            description = "بهای تمام‌شده فاکتور ${invoice.number}",
            sourceType = JournalSourceType.INVOICE_SALE,
            sourceId = invoice.id + "_cogs",
            lines = lines,
            storeId = storeId
        )
    }

    // ═══════════════════════════════════════════════════════════
    // F8 — برگشت از فروش
    // ═══════════════════════════════════════════════════════════
    /**
     * Dr: برگشت از فروش (کاهنده درآمد)
     * Dr: مالیات فروش (کاهش بدهی VAT)
     * Cr: حساب دریافتنی مشتری
     */
    fun forSaleReturn(returnInvoice: Invoice, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        val netReturn = returnInvoice.subtotal.rials - returnInvoice.discountTotal.rials
        if (netReturn > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.SALES_RETURN,
                debit = netReturn
            ))
        }
        if (returnInvoice.taxTotal.rials > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.VAT_PAYABLE,
                debit = returnInvoice.taxTotal.rials
            ))
        }
        lines.add(JournalLineDraft(
            accountCode = ChartOfAccounts.ACCOUNTS_RECEIVABLE_CUSTOMERS,
            partyId = returnInvoice.partyId,
            credit = returnInvoice.grandTotal.rials
        ))

        return JournalDraft(
            entryDate = returnInvoice.issueDate,
            description = "برگشت از فروش ${returnInvoice.number}",
            sourceType = JournalSourceType.INVOICE_SALE,
            sourceId = returnInvoice.id,
            lines = lines,
            storeId = storeId
        )
    }

    /**
     * برگشت COGS: Dr موجودی، Cr بهای تمام‌شده
     */
    fun forSaleReturnCogs(returnInvoice: Invoice, cogsRials: Long, storeId: String): JournalDraft {
        val lines = listOf(
            JournalLineDraft(accountCode = ChartOfAccounts.INVENTORY, debit = cogsRials),
            JournalLineDraft(accountCode = ChartOfAccounts.COGS, credit = cogsRials)
        )
        return JournalDraft(
            entryDate = returnInvoice.issueDate,
            description = "برگشت COGS فاکتور ${returnInvoice.number}",
            sourceType = JournalSourceType.INVOICE_SALE,
            sourceId = returnInvoice.id + "_cogs",
            lines = lines,
            storeId = storeId
        )
    }

    // ═══════════════════════════════════════════════════════════
    // F8 — برگشت به خرید
    // ═══════════════════════════════════════════════════════════
    /**
     * Dr: حساب پرداختنی تأمین‌کننده
     * Cr: موجودی کالا
     * Cr: مالیات (کاهش VAT قابل اعتبار)
     */
    fun forPurchaseReturn(returnInvoice: Invoice, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        lines.add(JournalLineDraft(
            accountCode = ChartOfAccounts.ACCOUNTS_PAYABLE_SUPPLIERS,
            partyId = returnInvoice.partyId,
            debit = returnInvoice.grandTotal.rials
        ))

        val netReturn = returnInvoice.subtotal.rials - returnInvoice.discountTotal.rials
        if (netReturn > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.INVENTORY,
                credit = netReturn
            ))
        }
        if (returnInvoice.taxTotal.rials > 0) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.VAT_PAYABLE,
                credit = returnInvoice.taxTotal.rials
            ))
        }

        return JournalDraft(
            entryDate = returnInvoice.issueDate,
            description = "برگشت به خرید ${returnInvoice.number}",
            sourceType = JournalSourceType.INVOICE_PURCHASE,
            sourceId = returnInvoice.id,
            lines = lines,
            storeId = storeId
        )
    }

    // ═══════════════════════════════════════════════════════════
    // F9 — چک (دریافتی/پرداختی/وصول/برگشت)
    // ═══════════════════════════════════════════════════════════
    /**
     * ثبت اولیه‌ی چک:
     * - دریافتی: Dr اسناد دریافتنی، Cr حساب دریافتنی مشتری
     * - پرداختی: Dr حساب پرداختنی تأمین‌کننده، Cr اسناد پرداختنی
     */
    fun forChequeInitial(cheque: Cheque, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        if (cheque.kind == ChequeKind.RECEIVED) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CHEQUES_RECEIVABLE,
                debit = cheque.amount.rials
            ))
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.ACCOUNTS_RECEIVABLE_CUSTOMERS,
                partyId = cheque.partyId,
                credit = cheque.amount.rials
            ))
        } else {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.ACCOUNTS_PAYABLE_SUPPLIERS,
                partyId = cheque.partyId,
                debit = cheque.amount.rials
            ))
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CHEQUES_PAYABLE,
                credit = cheque.amount.rials
            ))
        }

        return JournalDraft(
            entryDate = cheque.issueDate,
            description = "ثبت چک ${if (cheque.kind == ChequeKind.RECEIVED) "دریافتی" else "پرداختی"} ${cheque.number}",
            sourceType = if (cheque.kind == ChequeKind.RECEIVED)
                JournalSourceType.CHEQUE_RECEIVED
            else JournalSourceType.CHEQUE_ISSUED,
            sourceId = "${cheque.id}_init",
            lines = lines,
            storeId = storeId
        )
    }

    /**
     * وصول چک:
     * - دریافتی: Dr بانک، Cr اسناد دریافتنی
     * - پرداختی: Dr اسناد پرداختنی، Cr بانک
     */
    fun forChequeCleared(cheque: Cheque, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        if (cheque.kind == ChequeKind.RECEIVED) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.BANKS,
                debit = cheque.amount.rials
            ))
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CHEQUES_RECEIVABLE,
                credit = cheque.amount.rials
            ))
        } else {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CHEQUES_PAYABLE,
                debit = cheque.amount.rials
            ))
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.BANKS,
                credit = cheque.amount.rials
            ))
        }

        return JournalDraft(
            entryDate = System.currentTimeMillis(),
            description = "وصول چک ${cheque.number}",
            sourceType = if (cheque.kind == ChequeKind.RECEIVED)
                JournalSourceType.CHEQUE_RECEIVED
            else JournalSourceType.CHEQUE_ISSUED,
            sourceId = "${cheque.id}_cleared",
            lines = lines,
            storeId = storeId
        )
    }

    /**
     * برگشت چک:
     * - دریافتی: Dr حساب دریافتنی مشتری، Cr اسناد دریافتنی (برگشت به بدهی)
     * - پرداختی: Dr اسناد پرداختنی، Cr حساب پرداختنی تأمین‌کننده
     */
    fun forChequeBounced(cheque: Cheque, storeId: String): JournalDraft {
        val lines = mutableListOf<JournalLineDraft>()

        if (cheque.kind == ChequeKind.RECEIVED) {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.ACCOUNTS_RECEIVABLE_CUSTOMERS,
                partyId = cheque.partyId,
                debit = cheque.amount.rials
            ))
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CHEQUES_RECEIVABLE,
                credit = cheque.amount.rials
            ))
        } else {
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.CHEQUES_PAYABLE,
                debit = cheque.amount.rials
            ))
            lines.add(JournalLineDraft(
                accountCode = ChartOfAccounts.ACCOUNTS_PAYABLE_SUPPLIERS,
                partyId = cheque.partyId,
                credit = cheque.amount.rials
            ))
        }

        return JournalDraft(
            entryDate = System.currentTimeMillis(),
            description = "برگشت چک ${cheque.number}",
            sourceType = if (cheque.kind == ChequeKind.RECEIVED)
                JournalSourceType.CHEQUE_RECEIVED
            else JournalSourceType.CHEQUE_ISSUED,
            sourceId = "${cheque.id}_bounced",
            lines = lines,
            storeId = storeId
        )
    }
}