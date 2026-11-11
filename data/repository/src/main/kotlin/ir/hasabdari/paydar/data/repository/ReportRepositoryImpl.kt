package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.ReportDao
import ir.hasabdari.paydar.domain.model.BalanceSheet
import ir.hasabdari.paydar.domain.model.IncomeStatement
import ir.hasabdari.paydar.domain.model.InventoryValueItem
import ir.hasabdari.paydar.domain.model.InventoryValueReport
import ir.hasabdari.paydar.domain.model.PartyBalanceItem
import ir.hasabdari.paydar.domain.model.PartyBalanceReport
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import ir.hasabdari.paydar.domain.model.PartyLedgerLine
import ir.hasabdari.paydar.domain.model.ReportAccountLine
import ir.hasabdari.paydar.domain.model.SalesByDate
import ir.hasabdari.paydar.domain.model.SalesByProduct
import ir.hasabdari.paydar.domain.model.SalesReport
import ir.hasabdari.paydar.domain.repository.ReportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val dao: ReportDao
) : ReportRepository {

    // ═══════════════════════════════════════════════════════════
    // سود و زیان
    // ═══════════════════════════════════════════════════════════
    override suspend fun incomeStatement(
        storeId: String, fromDate: Long, toDate: Long
    ): AppResult<IncomeStatement> = try {
        val balances = dao.getAccountBalances(storeId, fromDate, toDate)

        // درآمد: SUM(credit) - SUM(debit) برای category=REVENUE
        val revenueAccounts = balances.filter { it.category == "REVENUE" }
        val revenues = revenueAccounts.map {
            ReportAccountLine(
                accountCode = it.accountCode,
                accountName = it.accountName,
                amountRials = it.totalCredit - it.totalDebit
            )
        }.filter { it.amountRials != 0L }
        val totalRevenue = revenues.sumOf { it.amountRials }

        // COGS: SUM(debit) - SUM(credit) برای category=COGS
        val cogsAccounts = balances.filter { it.category == "COGS" }
        val cogs = cogsAccounts.map {
            ReportAccountLine(
                accountCode = it.accountCode,
                accountName = it.accountName,
                amountRials = it.totalDebit - it.totalCredit
            )
        }.filter { it.amountRials != 0L }
        val totalCogs = cogs.sumOf { it.amountRials }

        // هزینه: SUM(debit) - SUM(credit) برای category=EXPENSE
        val expenseAccounts = balances.filter { it.category == "EXPENSE" }
        val expenses = expenseAccounts.map {
            ReportAccountLine(
                accountCode = it.accountCode,
                accountName = it.accountName,
                amountRials = it.totalDebit - it.totalCredit
            )
        }.filter { it.amountRials != 0L }
        val totalExpenses = expenses.sumOf { it.amountRials }

        AppResult.Success(
            IncomeStatement(
                fromDate = fromDate,
                toDate = toDate,
                revenues = revenues,
                totalRevenueRials = totalRevenue,
                cogs = cogs,
                totalCogsRials = totalCogs,
                grossProfitRials = totalRevenue - totalCogs,
                expenses = expenses,
                totalExpensesRials = totalExpenses,
                netProfitRials = totalRevenue - totalCogs - totalExpenses
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══════════════════════════════════════════════════════════
    // ترازنامه
    // ═══════════════════════════════════════════════════════════
    override suspend fun balanceSheet(storeId: String, asOfDate: Long): AppResult<BalanceSheet> = try {
        // از ابتدای زمان تا asOfDate
        val fromBeginning = 0L
        val balances = dao.getAccountBalances(storeId, fromBeginning, asOfDate)

        val assets = balances.filter { it.category == "ASSET" }.map {
            ReportAccountLine(it.accountCode, it.accountName, it.totalDebit - it.totalCredit)
        }.filter { it.amountRials != 0L }
        val totalAssets = assets.sumOf { it.amountRials }

        val liabilities = balances.filter { it.category == "LIABILITY" }.map {
            ReportAccountLine(it.accountCode, it.accountName, it.totalCredit - it.totalDebit)
        }.filter { it.amountRials != 0L }
        val totalLiabilities = liabilities.sumOf { it.amountRials }

        val equity = balances.filter { it.category == "EQUITY" }.map {
            ReportAccountLine(it.accountCode, it.accountName, it.totalCredit - it.totalDebit)
        }.filter { it.amountRials != 0L }
        val totalEquity = equity.sumOf { it.amountRials }

        // سود انباشته = Revenue - COGS - Expenses
        val totalRevenue = balances.filter { it.category == "REVENUE" }
            .sumOf { it.totalCredit - it.totalDebit }
        val totalCogs = balances.filter { it.category == "COGS" }
            .sumOf { it.totalDebit - it.totalCredit }
        val totalExpenses = balances.filter { it.category == "EXPENSE" }
            .sumOf { it.totalDebit - it.totalCredit }
        val retainedEarnings = totalRevenue - totalCogs - totalExpenses

        val totalLiabEq = totalLiabilities + totalEquity + retainedEarnings

        AppResult.Success(
            BalanceSheet(
                asOfDate = asOfDate,
                assets = assets,
                totalAssetsRials = totalAssets,
                liabilities = liabilities,
                totalLiabilitiesRials = totalLiabilities,
                equity = equity,
                totalEquityRials = totalEquity,
                retainedEarningsRials = retainedEarnings,
                totalLiabilitiesAndEquityRials = totalLiabEq,
                isBalanced = totalAssets == totalLiabEq
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══════════════════════════════════════════════════════════
    // گزارش فروش
    // ═══════════════════════════════════════════════════════════
    override suspend fun salesReport(
        storeId: String, fromDate: Long, toDate: Long
    ): AppResult<SalesReport> = try {
        val invoices = dao.getSaleInvoices(storeId, fromDate, toDate)
        val byProductRaw = dao.getSalesByProduct(storeId, fromDate, toDate)

        val invoiceCount = invoices.size
        val totalSales = invoices.sumOf { it.grandTotal }
        val totalCash = invoices.sumOf { it.paidAmount }
        val totalCredit = totalSales - totalCash

        val byProduct = byProductRaw.map {
            val revenue = it.totalRevenue
            val cost = it.totalCost
            SalesByProduct(
                productId = it.productId,
                productName = it.productName,
                quantitySoldScaled = it.totalQuantity,
                totalRevenueRials = revenue,
                totalCostRials = cost,
                profitRials = revenue - cost
            )
        }
        val totalCost = byProduct.sumOf { it.totalCostRials }

        // گروه‌بندی per روز
        data class DayKey(val y: Int, val m: Int, val d: Int)
        val grouped = invoices.groupBy {
            val j = JalaliDate.fromEpochMillis(it.issueDate)
            DayKey(j.year, j.month, j.day)
        }
        val byDate = grouped.map { (key, list) ->
            SalesByDate(
                jalaliDate = JalaliDate(key.y, key.m, key.d),
                invoiceCount = list.size,
                totalRials = list.sumOf { it.grandTotal }
            )
        }.sortedByDescending { "${it.jalaliDate.year}-${it.jalaliDate.month.toString().padStart(2, '0')}-${it.jalaliDate.day.toString().padStart(2, '0')}" }

        AppResult.Success(
            SalesReport(
                fromDate = fromDate,
                toDate = toDate,
                invoiceCount = invoiceCount,
                totalSalesRials = totalSales,
                totalCashSalesRials = totalCash,
                totalCreditSalesRials = totalCredit,
                totalCostRials = totalCost,
                totalProfitRials = totalSales - totalCost,
                byDate = byDate,
                topProducts = byProduct
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══════════════════════════════════════════════════════════
    // مانده اشخاص
    // ═══════════════════════════════════════════════════════════
    override suspend fun partyBalances(storeId: String): AppResult<PartyBalanceReport> = try {
        val customers = dao.getCustomerBalances(storeId).map {
            PartyBalanceItem(
                partyId = it.partyId,
                partyName = it.partyName,
                mobile = it.mobile,
                invoiceCount = it.invoiceCount,
                balanceRials = it.receivable
            )
        }
        val suppliers = dao.getSupplierBalances(storeId).map {
            PartyBalanceItem(
                partyId = it.partyId,
                partyName = it.partyName,
                mobile = it.mobile,
                invoiceCount = it.invoiceCount,
                balanceRials = it.payable
            )
        }

        AppResult.Success(
            PartyBalanceReport(
                totalReceivableRials = customers.sumOf { it.balanceRials },
                totalPayableRials = suppliers.sumOf { it.balanceRials },
                customers = customers,
                suppliers = suppliers
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══════════════════════════════════════════════════════════
    // ارزش موجودی
    // ═══════════════════════════════════════════════════════════
    override suspend fun inventoryValues(storeId: String): AppResult<InventoryValueReport> = try {
        val raw = dao.getInventoryValues(storeId)
        val items = raw.map {
            val avgCost = if (it.quantity > 0) (it.totalValue * 1000) / it.quantity else 0L
            InventoryValueItem(
                productId = it.productId,
                productName = it.productName,
                productCode = it.productCode,
                quantityScaled = it.quantity,
                avgCostRials = avgCost,
                totalValueRials = it.totalValue,
                isLowStock = it.quantity > 0 && it.quantity <= it.minStock
            )
        }
        AppResult.Success(
            InventoryValueReport(
                totalValueRials = items.sumOf { it.totalValueRials },
                totalProducts = items.size,
                lowStockCount = items.count { it.isLowStock },
                items = items
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))

    }
    override suspend fun partyDetailReport(
        storeId: String, partyId: String, fromDate: Long, toDate: Long
    ): AppResult<PartyDetailReport> = try {
        // ─── ۱. اطلاعات شخص ───
        val partyEntity = dao.getPartyById(partyId)  // ← نیاز به کوئری جدید در DAO
        val partyKind = if (partyEntity != null) {
            runCatching { ir.hasabdari.paydar.domain.model.PartyKind.valueOf(partyEntity.kind) }
                .getOrDefault(ir.hasabdari.paydar.domain.model.PartyKind.CUSTOMER)
        } else ir.hasabdari.paydar.domain.model.PartyKind.CUSTOMER

        // ─── ۲. مانده ابتدای دوره ───
        val opening = dao.getPartyOpeningBalance(partyId, storeId, fromDate)
        val openingBalance = opening.totalDebit - opening.totalCredit

        // ─── ۳. تراکنش‌ها ───
        val rawTxns = dao.getPartyTransactions(partyId, storeId, fromDate, toDate)

        // ─── ۴. ساخت ردیف‌ها با مانده جاری ───
        var running = openingBalance
        var totalDebit = 0L
        var totalCredit = 0L
        val rows = rawTxns.map { raw ->
            val debit = raw.totalDebit
            val credit = raw.totalCredit
            running += (debit - credit)
            totalDebit += debit
            totalCredit += credit

            PartyLedgerLine(
                entryId = raw.entryId,
                date = raw.entryDate,
                documentType = mapSourceType(raw.sourceType),
                documentNumber = buildDocumentNumber(raw.sourceType, raw.entryNumber, raw.sourceId),
                description = raw.description,
                debitRials = debit,
                creditRials = credit,
                runningBalanceRials = running
            )
        }

        val closingBalance = openingBalance + totalDebit - totalCredit

        AppResult.Success(
            PartyDetailReport(
                partyId = partyId,
                partyName = partyEntity?.name ?: "—",
                partyKind = partyKind,
                mobile = partyEntity?.mobile,
                address = partyEntity?.address,
                fromDate = fromDate,
                toDate = toDate,
                openingBalanceRials = openingBalance,
                totalDebitRials = totalDebit,
                totalCreditRials = totalCredit,
                closingBalanceRials = closingBalance,
                rows = rows
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══ Helpers ═══
    private fun mapSourceType(sourceType: String): String = when (sourceType) {
        "INVOICE_SALE" -> "فاکتور فروش"
        "INVOICE_PURCHASE" -> "فاکتور خرید"
        "CASH_RECEIPT" -> "دریافت"
        "CASH_PAYMENT" -> "پرداخت"
        "CASH_TRANSFER" -> "انتقال"
        "CHEQUE_RECEIVED" -> "چک دریافتی"
        "CHEQUE_ISSUED" -> "چک پرداختی"
        "STOCK_ADJUSTMENT" -> "تعدیل انبار"
        "OPENING" -> "مانده اولیه"
        "CLOSING" -> "بستن دوره"
        "MANUAL" -> "سند دستی"
        "REVERSAL" -> "برگشت سند"
        else -> sourceType
    }

    private fun buildDocumentNumber(
        sourceType: String, entryNumber: Long, sourceId: String?
    ): String = when (sourceType) {
        "INVOICE_SALE", "INVOICE_PURCHASE",
        "CASH_RECEIPT", "CASH_PAYMENT" -> "سند #${entryNumber}"
        else -> "سند #${entryNumber}"
    }

}