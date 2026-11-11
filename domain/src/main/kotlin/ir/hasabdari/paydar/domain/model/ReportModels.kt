package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.date.JalaliDate

// ═══════════════════════════════════════════════════════════
// سود و زیان
// ═══════════════════════════════════════════════════════════

data class ReportAccountLine(
    val accountCode: String,
    val accountName: String,
    val amountRials: Long
)

data class IncomeStatement(
    val fromDate: Long,
    val toDate: Long,
    val revenues: List<ReportAccountLine> = emptyList(),
    val totalRevenueRials: Long = 0,
    val cogs: List<ReportAccountLine> = emptyList(),
    val totalCogsRials: Long = 0,
    val grossProfitRials: Long = 0,
    val expenses: List<ReportAccountLine> = emptyList(),
    val totalExpensesRials: Long = 0,
    val netProfitRials: Long = 0
)

// ═══════════════════════════════════════════════════════════
// ترازنامه
// ═══════════════════════════════════════════════════════════

data class BalanceSheet(
    val asOfDate: Long,
    val assets: List<ReportAccountLine> = emptyList(),
    val totalAssetsRials: Long = 0,
    val liabilities: List<ReportAccountLine> = emptyList(),
    val totalLiabilitiesRials: Long = 0,
    val equity: List<ReportAccountLine> = emptyList(),
    val totalEquityRials: Long = 0,
    val retainedEarningsRials: Long = 0,
    val totalLiabilitiesAndEquityRials: Long = 0,
    val isBalanced: Boolean = false
)

// ═══════════════════════════════════════════════════════════
// گزارش فروش
// ═══════════════════════════════════════════════════════════

data class SalesByDate(
    val jalaliDate: JalaliDate,
    val invoiceCount: Int,
    val totalRials: Long
)

data class SalesByProduct(
    val productId: String,
    val productName: String,
    val quantitySoldScaled: Long,
    val totalRevenueRials: Long,
    val totalCostRials: Long,
    val profitRials: Long
)

data class SalesReport(
    val fromDate: Long,
    val toDate: Long,
    val invoiceCount: Int = 0,
    val totalSalesRials: Long = 0,
    val totalCashSalesRials: Long = 0,
    val totalCreditSalesRials: Long = 0,
    val totalCostRials: Long = 0,
    val totalProfitRials: Long = 0,
    val byDate: List<SalesByDate> = emptyList(),
    val topProducts: List<SalesByProduct> = emptyList()
)

// ═══════════════════════════════════════════════════════════
// مانده اشخاص
// ═══════════════════════════════════════════════════════════

data class PartyBalanceItem(
    val partyId: String,
    val partyName: String,
    val mobile: String?,
    val invoiceCount: Int,
    val balanceRials: Long  // مثبت = طلب ما از او، منفی = بدهی ما به او
)

data class PartyBalanceReport(
    val totalReceivableRials: Long = 0,
    val totalPayableRials: Long = 0,
    val customers: List<PartyBalanceItem> = emptyList(),
    val suppliers: List<PartyBalanceItem> = emptyList()
)

// ═══════════════════════════════════════════════════════════
// ارزش موجودی
// ═══════════════════════════════════════════════════════════

data class InventoryValueItem(
    val productId: String,
    val productName: String,
    val productCode: String,
    val quantityScaled: Long,
    val avgCostRials: Long,
    val totalValueRials: Long,
    val isLowStock: Boolean
)

data class InventoryValueReport(
    val totalValueRials: Long = 0,
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val items: List<InventoryValueItem> = emptyList()
)

data class PartyLedgerLine(
    val entryId: String,
    val date: Long,
    val documentType: String,       // "فاکتور فروش", "دریافت", "پرداخت", ...
    val documentNumber: String,     // "SAL-0001", "سند #۱۴"
    val description: String?,
    val debitRials: Long,
    val creditRials: Long,
    val runningBalanceRials: Long   // مانده بعد از این تراکنش
)

data class PartyDetailReport(
    val partyId: String,
    val partyName: String,
    val partyKind: PartyKind,
    val mobile: String?,
    val address: String?,
    val fromDate: Long,
    val toDate: Long,
    val openingBalanceRials: Long,   // + = بدهکار / − = بستانکار
    val totalDebitRials: Long,
    val totalCreditRials: Long,
    val closingBalanceRials: Long,
    val rows: List<PartyLedgerLine>
) {
    val isDebitBalance: Boolean get() = closingBalanceRials >= 0
}