package ir.hasabdari.paydar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.model.TxnType
import ir.hasabdari.paydar.ui.common.scaledToQuantityString
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToProducts: () -> Unit,
    onNavigateToWarehouses: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToSuppliers: () -> Unit = {},
    onNavigateToTreasury: () -> Unit = {},
    onNavigateToAddReceipt: () -> Unit = {},
    onNavigateToAddPayment: () -> Unit = {},
    onNavigateBottom: (String) -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToPos: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()

) {
    val state by viewModel.state.collectAsState()
    val d = state.data
    var selectedPeriod by remember { mutableStateOf(0) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun comingSoon(msg: String) {
        scope.launch { snackbarHost.showSnackbar(msg) }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "سلام، رضایی 👋",
                    onMenu = { },
                    showNotification = true,
                    notificationDot = true,
                    onNotification = { },
                    showProfile = true,
                    onProfile = { }
                )
            }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(
                    currentRoute = "home",
                    onNavigate = onNavigateBottom,
                    onFabClick = onNavigateToPos         // ← FAB به POS
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══ Chips ═══
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("امروز", selectedPeriod == 0, onClick = { selectedPeriod = 0 })
                    PDChip("هفته", selectedPeriod == 1, onClick = { selectedPeriod = 1 })
                    PDChip("ماه", selectedPeriod == 2, onClick = { selectedPeriod = 2 })
                }
            }

            // ═══ Hero: فروش امروز ═══
            item {
                PDHeroCard(
                    label = "💰 فروش امروز",
                    amount = d.salesToday.rials.toTomanString(),
                    currency = "تومان",
                    trendText = if (d.salesTrendPercent > 0)
                        "▲ ${d.salesTrendPercent}٪ بیشتر از دیروز"
                    else "امروز هنوز فروشی ثبت نشده",
                    sparklineValues = d.sparklineValues
                )
            }

            // ═══ 2 KPI Cards (سود + فاکتور) — هم‌اندازه با IntrinsicSize.Min ═══
            item {
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PDKpiCard(
                        icon = "📈",
                        label = "سود امروز",
                        value = d.profitToday.rials.toTomanString(),
                        color = PDKpiColor.AMBER,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        trend = if (d.profitTrendPercent > 0) "▲ ${d.profitTrendPercent}٪" else null,
                        trendPositive = true
                    )
                    PDKpiCard(
                        icon = "🧾",
                        label = "فاکتور امروز",
                        value = "${d.invoicesToday} عدد",
                        color = PDKpiColor.SKY,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        trend = if (d.invoicesTrend > 0) "▲ ${d.invoicesTrend}" else null,
                        trendPositive = true
                    )
                }
            }

            // ═══ Warn: کم‌موجودی ═══
            if (d.lowStockCount + d.outOfStockCount > 0) {
                item {
                    val title = when {
                        d.outOfStockCount > 0 -> "${d.outOfStockCount} کالا ناموجود · ${d.lowStockCount} کالا کم‌موجود"
                        else -> "${d.lowStockCount} کالا رو به اتمام"
                    }
                    val body = d.lowStockProducts.joinToString(" · ") { p ->
                        "${p.product.name} (${p.totalStockScaled.scaledToQuantityString()})"
                    }.ifBlank { "بررسی موجودی انبارها" }
                    PDWarnCard(
                        title = title,
                        body = body,
                        onClick = onNavigateToProducts
                    )
                }
            }

            // ═══ Debt: مشتری + تأمین‌کننده ═══
            item {
                PDDebtCard(
                    icon = "👥",
                    label = "بدهی مشتریان",
                    value = "۰ تومان",
                    meta = "مشاهده لیست مشتریان",
                    type = PDDebtType.CUSTOMER,
                    onClick = onNavigateToCustomers
                )
            }
            item {
                PDDebtCard(
                    icon = "🏭",
                    label = "بدهی به تأمین‌کنندگان",
                    value = "۰ تومان",
                    meta = "مشاهده لیست تأمین‌کنندگان",
                    type = PDDebtType.SUPPLIER,
                    onClick = onNavigateToSuppliers
                )
            }

            // ═══ اقدامات سریع — دقیقاً مطابق طرح ═══
            item { PDSectionHeader(title = "⚡ اقدامات سریع") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PDQuickAction(
                        icon = "🛒", label = "خرید", color = PDKpiColor.TEAL,
                        onClick = { comingSoon("ثبت خرید در فاز F5 فعال می‌شود") },
                        modifier = Modifier.weight(1f)
                    )
                    PDQuickAction(
                        icon = "📥", label = "دریافت", color = PDKpiColor.SKY,
                        onClick = onNavigateToAddReceipt,
                        modifier = Modifier.weight(1f)
                    )
                    PDQuickAction(
                        icon = "📤", label = "پرداخت", color = PDKpiColor.VIOLET,
                        onClick = onNavigateToAddPayment,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ═══ Bar Chart ═══
            item {
                PDSectionHeader(title = "📊 آخرین ۷ روز", actionText = "همه ←", onAction = {
                    comingSoon("گزارش‌های فروش در فاز F8 فعال می‌شوند")
                })
            }
            item {
                val bars = d.weeklyBars
                val maxVal = bars.maxOfOrNull { it.valueRials } ?: 1L
                PDBarChartCard(
                    title = "فروش روزانه",
                    subtitle = "مجموع: ${d.weeklyTotal.rials.toTomanString()} ت",
                    badge = "میانگین ${d.weeklyAverage.rials.toTomanString()}",
                    items = bars.map { bar ->
                        PDBarChartItem(
                            label = bar.label,
                            valueText = "${bar.valueRials / 1_000_000}M",
                            valueFraction = bar.valueRials.toFloat() / maxVal.toFloat(),
                            isPeak = bar.isPeak
                        )
                    }
                )
            }

            // ═══ Donut ═══
            if (d.donutSegments.isNotEmpty()) {
                item { PDSectionHeader(title = "🍩 ترکیب فروش") }
                item {
                    PDDonutCard(
                        segments = d.donutSegments.map { seg ->
                            PDDonutSegment(
                                label = seg.label,
                                valueText = "${seg.valuePercent}٪",
                                valueFraction = seg.valuePercent / 100f,
                                color = Color(seg.colorArgb)
                            )
                        },
                        centerPercent = d.donutCenterPercent,
                        centerLabel = d.donutCenterLabel
                    )
                }
            }

            // ═══ کالاهای کم‌موجود ═══
            if (d.lowStockProducts.isNotEmpty()) {
                item {
                    PDSectionHeader(
                        title = "⚠️ کالاهای نیازمند رسیدگی",
                        actionText = "همه ←",
                        onAction = onNavigateToProducts
                    )
                }
                items(d.lowStockProducts.take(3)) { p ->
                    LowStockRow(p, onClick = onNavigateToProducts)
                }
            }

            // ═══ تراکنش‌های اخیر ═══
            if (d.recentTxns.isNotEmpty()) {
                item {
                    PDSectionHeader(title = "🕐 آخرین تراکنش‌ها", actionText = "همه ←", onAction = { })
                }
                items(d.recentTxns.take(3), key = { it.id }) { txn ->
                    PDTxnRow(
                        avatarText = txn.avatarText,
                        type = when (txn.type) {
                            TxnType.SALE -> PDTxnType.SALE
                            TxnType.RECEIPT -> PDTxnType.RECEIPT
                            TxnType.PURCHASE -> PDTxnType.PURCHASE
                        },
                        title = txn.title,
                        meta = txn.meta,
                        amount = (if (txn.isPositive) "+" else "−") + txn.amountRials.toTomanString(),
                        amountPositive = txn.isPositive
                    )
                }
            } else {
                item {
                    PDWarnCard(
                        title = "هنوز تراکنشی ثبت نشده",
                        body = "با ثبت اولین فروش یا خرید، تاریخچه تراکنش‌ها اینجا نمایش داده می‌شود",
                        icon = "🕐",
                        iconStart = Sky400,
                        iconEnd = Sky600,
                        titleColor = Sky700,
                        bodyColor = Sky700
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LowStockRow(p: ProductWithStock, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(if (p.totalStockScaled <= 0) Rose100 else Amber100),
            contentAlignment = Alignment.Center
        ) { Text(if (p.totalStockScaled <= 0) "❌" else "⚠️", fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.product.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(
                "موجودی: ${p.totalStockScaled.scaledToQuantityString()} · حداقل: ${p.product.minStockScaled.scaledToQuantityString()}",
                fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp)
            )
        }
        PDBadge(
            text = if (p.totalStockScaled <= 0) "ناموجود" else "کم",
            type = if (p.totalStockScaled <= 0) PDBadgeType.OUT else PDBadgeType.LOW
        )
    }
}