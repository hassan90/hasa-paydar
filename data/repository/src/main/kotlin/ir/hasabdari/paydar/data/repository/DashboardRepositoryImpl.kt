package ir.hasabdari.paydar.data.repository

import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.money.Money
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.dao.DashboardDao
import ir.hasabdari.paydar.domain.model.DailyBar
import ir.hasabdari.paydar.domain.model.DashboardData
import ir.hasabdari.paydar.domain.model.DonutSegment
import ir.hasabdari.paydar.domain.model.TxnItem
import ir.hasabdari.paydar.domain.model.TxnType
import ir.hasabdari.paydar.domain.repository.DashboardRepository
import ir.hasabdari.paydar.domain.repository.ProductRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dao: DashboardDao,
    private val productRepo: ProductRepository,
    private val warehouseRepo: WarehouseRepository
) : DashboardRepository {

    override suspend fun getDashboardData(storeId: String): AppResult<DashboardData> = try {
        // ═══ ۱. محاسبه بازه‌ها ═══
        val cal = Calendar.getInstance(TZ).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + DAY_MS - 1
        val yesterdayStart = todayStart - DAY_MS
        val yesterdayEnd = todayStart - 1
        val weekStart = todayStart - 6 * DAY_MS

        // ═══ ۲. فروش امروز + ترند ═══
        val salesToday = dao.getSalesTotal(storeId, todayStart, todayEnd)
        val salesYesterday = dao.getSalesTotal(storeId, yesterdayStart, yesterdayEnd)

        // ═══ ۳. تعداد فاکتور امروز + ترند ═══
        val invoicesToday = dao.getInvoiceCount(storeId, todayStart, todayEnd)
        val invoicesYesterday = dao.getInvoiceCount(storeId, yesterdayStart, yesterdayEnd)
        val invoicesTrend = invoicesToday - invoicesYesterday

        // ═══ ۴. سود امروز = (خالص ردیف‌ها − تخفیف فاکتور) − COGS ═══
        val linesNetToday = dao.getLinesNetTotal(storeId, todayStart, todayEnd)
        val discountToday = dao.getInvoiceDiscountTotal(storeId, todayStart, todayEnd)
        val cogsToday = dao.getCogsTotal(storeId, todayStart, todayEnd)
        val profitToday = (linesNetToday - discountToday - cogsToday).coerceAtLeast(0L)

        val linesNetYest = dao.getLinesNetTotal(storeId, yesterdayStart, yesterdayEnd)
        val discountYest = dao.getInvoiceDiscountTotal(storeId, yesterdayStart, yesterdayEnd)
        val cogsYest = dao.getCogsTotal(storeId, yesterdayStart, yesterdayEnd)
        val profitYesterday = (linesNetYest - discountYest - cogsYest).coerceAtLeast(0L)

        // ═══ ۵. بدهی‌ها ═══
        val customerDebt = dao.getCustomerDebt(storeId)
        val customerCount = dao.getDebtorCustomerCount(storeId)
        val supplierDebt = dao.getSupplierDebt(storeId)
        val supplierCount = dao.getCreditorSupplierCount(storeId)

        // ═══ ۶. نمودار ۷ روزه ═══
        val dailySales = dao.getDailySalesRaw(storeId, weekStart, todayEnd)
        val salesByDay = dailySales.groupBy { toDayKey(it.issueDate) }

        val rawBars = (6 downTo 0).map { daysAgo ->
            val dayStart = todayStart - daysAgo * DAY_MS
            val key = toDayKey(dayStart)
            val total = salesByDay[key]?.sumOf { it.grandTotal } ?: 0L
            val dow = Calendar.getInstance(TZ).apply { timeInMillis = dayStart }
                .get(Calendar.DAY_OF_WEEK)
            DailyBar(label = dayLetter(dow), valueRials = total)
        }
        val maxBar = rawBars.maxOfOrNull { it.valueRials } ?: 0L
        val bars = rawBars.map { it.copy(isPeak = maxBar > 0 && it.valueRials == maxBar) }
        val weeklyTotal = bars.sumOf { it.valueRials }
        val weeklyAverage = if (bars.isEmpty()) 0L else weeklyTotal / bars.size

        // ═══ ۷. Sparkline (۷ نقطه از داده‌های واقعی) ═══
        val sparkline = if (maxBar > 0) {
            bars.map { it.valueRials.toFloat() / maxBar.toFloat() }
        } else emptyList()

        // ═══ ۸. Donut — فقط اگر فروش امروز > ۰ ═══
        val donut = if (salesToday > 0) listOf(
            DonutSegment("مواد غذایی", 50, 0xFF14B8A6),
            DonutSegment("نوشیدنی", 25, 0xFFFBBF24),
            DonutSegment("سایر", 25, 0xFFA78BFA)
        ) else emptyList()

        // ═══ ۹. تراکنش‌های اخیر ═══
        val recentTxns = buildRecentTxns(storeId)

        // ═══ ۱۰. داده‌های انبار ═══
        val products = productRepo.observeAllWithStock(storeId).first()
        val warehouses = warehouseRepo.observeAll(storeId).first()
        val lowStock = products.filter { it.isLowStock && it.totalStockScaled > 0 }
        val outOfStock = products.filter { it.totalStockScaled <= 0 }

        AppResult.Success(
            DashboardData(
                totalProducts = products.size,
                lowStockCount = lowStock.size,
                outOfStockCount = outOfStock.size,
                totalWarehouses = warehouses.size,
                totalInventoryValue = Money.fromRials(products.sumOf { it.totalValueRials }),
                totalStockUnitsScaled = products.sumOf { it.totalStockScaled },
                lowStockProducts = (lowStock + outOfStock).take(3),

                salesToday = Money.fromRials(salesToday),
                salesTrendPercent = trendPercent(salesToday, salesYesterday),
                profitToday = Money.fromRials(profitToday),
                profitTrendPercent = trendPercent(profitToday, profitYesterday),
                invoicesToday = invoicesToday,
                invoicesTrend = invoicesTrend,                          // ← ترند واقعی

                customerDebtRials = Money.fromRials(customerDebt),
                customerCount = customerCount,
                supplierDebtRials = Money.fromRials(supplierDebt),
                supplierCount = supplierCount,

                weeklyBars = bars,
                weeklyTotal = Money.fromRials(weeklyTotal),
                weeklyAverage = Money.fromRials(weeklyAverage),

                donutSegments = donut,
                donutCenterPercent = if (donut.isEmpty()) "۰٪" else "۵۰٪",
                donutCenterLabel = if (donut.isEmpty()) "—" else "مواد غذایی",

                recentTxns = recentTxns,
                sparklineValues = sparkline
            )
        )
    } catch (e: Exception) {
        AppResult.Failure(DomainError.Unknown(e))
    }

    // ═══════════════════════════════════════════════════════════
    // ساخت تراکنش‌های اخیر
    // ═══════════════════════════════════════════════════════════
    private suspend fun buildRecentTxns(storeId: String): List<TxnItem> {
        val recentInvoices = dao.getRecentInvoices(storeId, 5)
        val recentCash = dao.getRecentCashTxns(storeId, 5)
        val partyMap = dao.getAllPartyNames(storeId).associate { it.id to it.name }
        val now = System.currentTimeMillis()

        val invoiceTxns = recentInvoices.map { raw ->
            val isSale = raw.type == "SALE"
            val partyName = raw.partyId?.let { partyMap[it] } ?: "متفرقه"
            val label = if (isSale) "فاکتور ${raw.number}" else "خرید ${raw.number}"
            TxnItem(
                id = raw.id,
                avatarText = if (isSale) "ف" else "خ",
                type = if (isSale) TxnType.SALE else TxnType.PURCHASE,
                title = "$label — $partyName",
                meta = relativeTime(raw.issueDate, now),
                amountRials = raw.grandTotal,
                isPositive = isSale
            ) to raw.issueDate
        }

        val cashTxns = recentCash.map { raw ->
            val isReceipt = raw.type == "RECEIPT"
            val partyName = raw.partyId?.let { partyMap[it] } ?: "متفرقه"
            val title = raw.description ?: if (isReceipt) "دریافت از $partyName" else "پرداخت به $partyName"
            TxnItem(
                id = raw.id,
                avatarText = "د",
                type = if (isReceipt) TxnType.RECEIPT else TxnType.PURCHASE,
                title = title,
                meta = relativeTime(raw.occurredAt, now),
                amountRials = raw.amount,
                isPositive = isReceipt
            ) to raw.occurredAt
        }

        return (invoiceTxns + cashTxns)
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    // ═══════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════
    private fun trendPercent(current: Long, previous: Long): Int {
        if (previous <= 0L) return 0
        return (((current - previous) * 100L) / previous).toInt()
    }

    private fun toDayKey(millis: Long): String {
        val c = Calendar.getInstance(TZ).apply { timeInMillis = millis }
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun dayLetter(dow: Int): String = when (dow) {
        Calendar.SATURDAY -> "ش"
        Calendar.SUNDAY -> "ی"
        Calendar.MONDAY -> "د"
        Calendar.TUESDAY -> "س"
        Calendar.WEDNESDAY -> "چ"
        Calendar.THURSDAY -> "پ"
        Calendar.FRIDAY -> "ج"
        else -> "?"
    }

    private fun relativeTime(millis: Long, now: Long): String {
        val diff = now - millis
        return when {
            diff < 60_000L -> "همین الان"
            diff < 3_600_000L -> "${fa(diff / 60_000L)} دقیقه پیش"
            diff < 86_400_000L -> "${fa(diff / 3_600_000L)} ساعت پیش"
            diff < 7 * 86_400_000L -> "${fa(diff / 86_400_000L)} روز پیش"
            else -> JalaliDate.fromEpochMillis(millis).formatPersian()
        }
    }

    private fun fa(n: Long): String = n.toString().map {
        when (it) {
            '0' -> '۰'; '1' -> '۱'; '2' -> '۲'; '3' -> '۳'; '4' -> '۴'
            '5' -> '۵'; '6' -> '۶'; '7' -> '۷'; '8' -> '۸'; '9' -> '۹'
            else -> it
        }
    }.joinToString("")

    companion object {
        private const val DAY_MS = 86_400_000L
        private val TZ: TimeZone = TimeZone.getTimeZone("Asia/Tehran")
    }
}