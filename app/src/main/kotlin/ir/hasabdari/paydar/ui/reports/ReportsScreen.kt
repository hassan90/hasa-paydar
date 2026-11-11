package ir.hasabdari.paydar.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.*
import ir.hasabdari.paydar.ui.common.scaledToQuantityString
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDBottomNav
import ir.hasabdari.paydar.ui.components.PDChip
import ir.hasabdari.paydar.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("سود و زیان", "ترازنامه", "فروش", "اشخاص", "انبار")

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "گزارش‌ها",
                    onBack = onBack,
                    trailing = {
                        Text("🔄", fontSize = 20.sp,
                            modifier = Modifier.clickable { viewModel.refresh() })
                    }
                )
            }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(currentRoute = "more", onNavigate = onNavigateBottom)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Period selector
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PDChip("امروز", state.period == ReportPeriod.TODAY, onClick = { viewModel.onPeriodChange(ReportPeriod.TODAY) })
                PDChip("۷ روز", state.period == ReportPeriod.LAST_7_DAYS, onClick = { viewModel.onPeriodChange(ReportPeriod.LAST_7_DAYS) })
                PDChip("۳۰ روز", state.period == ReportPeriod.LAST_30_DAYS, onClick = { viewModel.onPeriodChange(ReportPeriod.LAST_30_DAYS) })
                PDChip("همه", state.period == ReportPeriod.ALL, onClick = { viewModel.onPeriodChange(ReportPeriod.ALL) })
            }

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Teal700,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal700)
                }
                return@Column
            }

            state.error?.let { err ->
                Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(10.dp))
                    .background(Rose50).padding(12.dp)) {
                    Text(err, color = Rose600, fontSize = 12.sp)
                }
            }

            when (selectedTab) {
                0 -> IncomeStatementTab(state.incomeStatement)
                1 -> BalanceSheetTab(state.balanceSheet)
                2 -> SalesTab(state.salesReport)
                3 -> PartiesTab(state.partyBalances)
                4 -> InventoryTab(state.inventoryReport)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Tab 1: سود و زیان
// ═══════════════════════════════════════════════════════════
@Composable
private fun IncomeStatementTab(data: IncomeStatement?) {
    if (data == null) { EmptyTab("داده‌ای موجود نیست"); return }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HeroSummary("سود خالص دوره",
                data.netProfitRials.toTomanString() + " ت",
                if (data.netProfitRials >= 0) Green600 else Rose600)
        }
        item { SectionCard("📈 درآمدها") {
            data.revenues.forEach { AccountRow(it, isDebit = false) }
            if (data.revenues.isEmpty()) Text("درآمدی ثبت نشده", color = Slate400, fontSize = 11.sp)
            SubtotalRow("جمع درآمد", data.totalRevenueRials, Green600)
        } }
        item { SectionCard("📦 بهای تمام‌شده") {
            data.cogs.forEach { AccountRow(it, isDebit = true) }
            if (data.cogs.isEmpty()) Text("بدون COGS", color = Slate400, fontSize = 11.sp)
            SubtotalRow("جمع COGS", data.totalCogsRials, Amber700)
            SubtotalRow("سود ناخالص", data.grossProfitRials, if (data.grossProfitRials >= 0) Green600 else Rose600)
        } }
        item { SectionCard("💸 هزینه‌ها") {
            data.expenses.forEach { AccountRow(it, isDebit = true) }
            if (data.expenses.isEmpty()) Text("هزینه‌ای ثبت نشده", color = Slate400, fontSize = 11.sp)
            SubtotalRow("جمع هزینه", data.totalExpensesRials, Rose600)
        } }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (data.netProfitRials >= 0) Green50 else Rose50).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سود خالص", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(data.netProfitRials.toTomanString() + " ت", fontSize = 16.sp, fontWeight = FontWeight.Black,
                        color = if (data.netProfitRials >= 0) Green600 else Rose600)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Tab 2: ترازنامه
// ═══════════════════════════════════════════════════════════
@Composable
private fun BalanceSheetTab(data: BalanceSheet?) {
    if (data == null) { EmptyTab("داده‌ای موجود نیست"); return }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (data.isBalanced) Green50 else Rose50).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (data.isBalanced) "✓" else "✗", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        color = if (data.isBalanced) Green600 else Rose600)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(if (data.isBalanced) "ترازنامه متوازن است" else "ترازنامه نامتوازن",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (data.isBalanced) Green600 else Rose600)
                        Text("دارایی = بدهی + حقوق مالکانه", fontSize = 10.sp, color = Slate500)
                    }
                }
            }
        }
        item { SectionCard("💚 دارایی‌ها") {
            data.assets.forEach { AccountRow(it, isDebit = true) }
            if (data.assets.isEmpty()) Text("—", color = Slate400, fontSize = 11.sp)
            SubtotalRow("جمع دارایی", data.totalAssetsRials, Green600)
        } }
        item { SectionCard("🔴 بدهی‌ها") {
            data.liabilities.forEach { AccountRow(it, isDebit = false) }
            if (data.liabilities.isEmpty()) Text("—", color = Slate400, fontSize = 11.sp)
            SubtotalRow("جمع بدهی", data.totalLiabilitiesRials, Rose600)
        } }
        item { SectionCard("🟣 حقوق مالکانه") {
            data.equity.forEach { AccountRow(it, isDebit = false) }
            SubtotalRow("سرمایه", data.totalEquityRials, Violet700)
            SubtotalRow("سود انباشته", data.retainedEarningsRials,
                if (data.retainedEarningsRials >= 0) Green600 else Rose600)
            SubtotalRow("جمع حقوق مالکانه", data.totalEquityRials + data.retainedEarningsRials, Violet700, bold = true)
        } }
        item {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(Teal50).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("جمع دارایی", fontSize = 11.sp, color = Slate600)
                        Text(data.totalAssetsRials.toTomanString() + " ت", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal700)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("جمع بدهی + حقوق", fontSize = 11.sp, color = Slate600)
                        Text(data.totalLiabilitiesAndEquityRials.toTomanString() + " ت", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal700)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Tab 3: فروش
// ═══════════════════════════════════════════════════════════
@Composable
private fun SalesTab(data: SalesReport?) {
    if (data == null) { EmptyTab("داده‌ای موجود نیست"); return }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeroSummary("کل فروش", data.totalSalesRials.toTomanString() + " ت", Green600) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallKpi("فاکتورها", data.invoiceCount.toString(), Sky600, Modifier.weight(1f))
                SmallKpi("نقدی", data.totalCashSalesRials.toTomanString(), Green600, Modifier.weight(1f))
                SmallKpi("نسیه", data.totalCreditSalesRials.toTomanString(), Rose600, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallKpi("بهای تمام‌شده", data.totalCostRials.toTomanString(), Amber700, Modifier.weight(1f))
                SmallKpi("سود", data.totalProfitRials.toTomanString(), Green600, Modifier.weight(1f))
            }
        }
        if (data.byDate.isNotEmpty()) {
            item {
                SectionCard("📅 فروش روزانه") {
                    data.byDate.take(10).forEach { d ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(d.jalaliDate.formatPersian(), fontSize = 11.sp, color = Slate600)
                            Text("${d.invoiceCount} فاکتور", fontSize = 10.sp, color = Slate400)
                            Text(d.totalRials.toTomanString() + " ت", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Green600)
                        }
                    }
                }
            }
        }
        if (data.topProducts.isNotEmpty()) {
            item { SectionCard("🏆 پرفروش‌ترین کالاها") {
                data.topProducts.take(10).forEach { p ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(p.productName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900, modifier = Modifier.weight(1f))
                            Text(p.totalRevenueRials.toTomanString() + " ت", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Green600)
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${p.quantitySoldScaled.scaledToQuantityString()} فروش", fontSize = 10.sp, color = Slate500)
                            Text("سود: ${p.profitRials.toTomanString()} ت", fontSize = 10.sp, color = Green600, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Tab 4: اشخاص
// ═══════════════════════════════════════════════════════════
@Composable
private fun PartiesTab(data: PartyBalanceReport?) {
    if (data == null) { EmptyTab("داده‌ای موجود نیست"); return }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallKpi("طلب از مشتریان", data.totalReceivableRials.toTomanString(), Rose600, Modifier.weight(1f))
                SmallKpi("بدهی به تأمین‌کنندگان", data.totalPayableRials.toTomanString(), Violet700, Modifier.weight(1f))
            }
        }
        if (data.customers.isNotEmpty()) {
            item { SectionLabel("👥 مشتریان بدهکار (${data.customers.size})") }
            items(data.customers) { c ->
                PartyRow(
                    name = c.partyName,
                    mobile = c.mobile,
                    balanceText = c.balanceRials.toTomanString() + " ت",
                    balanceColor = Rose600
                )
            }
        }
        if (data.suppliers.isNotEmpty()) {
            item { SectionLabel("🏭 تأمین‌کنندگان بستانکار (${data.suppliers.size})") }
            items(data.suppliers) { s ->
                PartyRow(
                    name = s.partyName,
                    mobile = s.mobile,
                    balanceText = s.balanceRials.toTomanString() + " ت",
                    balanceColor = Violet700
                )
            }
        }
        if (data.customers.isEmpty() && data.suppliers.isEmpty()) {
            item { EmptyTab("همه‌ی حساب‌ها تسویه هستند") }
        }
    }
}

@Composable
private fun PartyRow(
    name: String,
    mobile: String?,
    balanceText: String,
    balanceColor: Color
) {
    val mobileText = mobile?.takeIf { it.isNotBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
            if (mobileText != null) {
                Text(mobileText, fontSize = 10.sp, color = Slate500)
            }
        }
        Text(
            balanceText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = balanceColor
        )
    }
}


// ═══════════════════════════════════════════════════════════
// Tab 5: انبار
// ═══════════════════════════════════════════════════════════
@Composable
private fun InventoryTab(data: InventoryValueReport?) {
    if (data == null) { EmptyTab("داده‌ای موجود نیست"); return }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeroSummary("ارزش موجودی انبار", data.totalValueRials.toTomanString() + " ت", Teal700) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallKpi("کالاها", data.totalProducts.toString(), Sky600, Modifier.weight(1f))
                SmallKpi("کم‌موجود", data.lowStockCount.toString(), Amber700, Modifier.weight(1f))
            }
        }
        item { SectionLabel("📦 ارزش هر کالا") }
        items(data.items) { item ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("${item.quantityScaled.scaledToQuantityString()} × ${item.avgCostRials.toTomanString()} ت",
                        fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(item.totalValueRials.toTomanString() + " ت",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (item.isLowStock) Amber700 else Teal700)
                    if (item.isLowStock) Text("⚠️ کم‌موجود", fontSize = 9.sp, color = Amber700)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════

@Composable
private fun HeroSummary(label: String, value: String, color: Color) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(Brush.linearGradient(listOf(Teal800, Teal600))).padding(20.dp)) {
        Column {
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White,
                modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun SmallKpi(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp)
    ) {
        Text(label, fontSize = 10.sp, color = Slate500)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor,
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900,
        modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(14.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun AccountRow(line: ReportAccountLine, isDebit: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${line.accountCode} — ${line.accountName}", fontSize = 11.sp, color = Slate600, modifier = Modifier.weight(1f))
        Text(line.amountRials.toTomanString() + " ت", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = if (isDebit) Amber700 else Green600)
    }
}

@Composable
private fun SubtotalRow(label: String, amount: Long, color: Color, bold: Boolean = false) {
    Spacer(Modifier.height(6.dp))
    HorizontalDivider(color = Slate100)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = if (bold) 13.sp else 12.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold, color = Slate900)
        Text(amount.toTomanString() + " ت", fontSize = if (bold) 14.sp else 12.sp,
            fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EmptyTab(message: String) {
    Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
        Text(message, color = Slate500, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}