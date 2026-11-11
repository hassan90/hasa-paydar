package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

data class DashboardData(
    // ═══ داده‌های انبار ═══
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalWarehouses: Int = 0,
    val totalInventoryValue: Money = Money.ZERO,
    val totalStockUnitsScaled: Long = 0,
    val lowStockProducts: List<ProductWithStock> = emptyList(),

    // ═══ Hero: فروش امروز ═══
    val salesToday: Money = Money.ZERO,
    val salesTrendPercent: Int = 0,

    // ═══ KPI: سود امروز ═══
    val profitToday: Money = Money.ZERO,
    val profitTrendPercent: Int = 0,

    // ═══ KPI: فاکتور امروز ═══
    val invoicesToday: Int = 0,
    val invoicesTrend: Int = 0,

    // ═══ بدهی‌ها ═══
    val customerDebtRials: Money = Money.ZERO,
    val customerCount: Int = 0,
    val supplierDebtRials: Money = Money.ZERO,
    val supplierCount: Int = 0,

    // ═══ Bar Chart ۷ روزه ═══
    val weeklyBars: List<DailyBar> = emptyList(),
    val weeklyTotal: Money = Money.ZERO,
    val weeklyAverage: Money = Money.ZERO,

    // ═══ Donut ═══
    val donutSegments: List<DonutSegment> = emptyList(),
    val donutCenterPercent: String = "۰٪",
    val donutCenterLabel: String = "—",

    // ═══ تراکنش‌های اخیر ═══
    val recentTxns: List<TxnItem> = emptyList(),

    // ═══ Sparkline ═══
    val sparklineValues: List<Float> = emptyList()
)

data class DailyBar(
    val label: String,
    val valueRials: Long,
    val isPeak: Boolean = false
)

data class DonutSegment(
    val label: String,
    val valuePercent: Int,
    val colorArgb: Long
)

data class TxnItem(
    val id: String,
    val avatarText: String,
    val type: TxnType,
    val title: String,
    val meta: String,
    val amountRials: Long,
    val isPositive: Boolean
)

enum class TxnType { SALE, RECEIPT, PURCHASE }