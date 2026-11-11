package ir.hasabdari.paydar.ui.reports

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.model.PartyLedgerLine
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.Green600
import ir.hasabdari.paydar.ui.theme.Rose600
import ir.hasabdari.paydar.ui.theme.Slate50
import ir.hasabdari.paydar.ui.theme.Slate500
import ir.hasabdari.paydar.ui.theme.Slate600
import ir.hasabdari.paydar.ui.theme.Slate900
import ir.hasabdari.paydar.ui.theme.Teal200
import ir.hasabdari.paydar.ui.theme.Teal50
import ir.hasabdari.paydar.ui.theme.Teal600
import ir.hasabdari.paydar.ui.theme.Teal700
import ir.hasabdari.paydar.ui.theme.Teal800

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailReportScreen(
    onBack: () -> Unit,
    viewModel: PartyDetailReportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "گزارش تفصیلی",
                    onBack = onBack,
                    trailing = {
                        Text(
                            "🔄",
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                                .clickableCompat { viewModel.refresh() }
                        )
                    }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Date range chips
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateChip("این ماه", state.range == ReportDateRange.THIS_MONTH) {
                    viewModel.onRangeChange(ReportDateRange.THIS_MONTH)
                }
                DateChip("ماه قبل", state.range == ReportDateRange.LAST_MONTH) {
                    viewModel.onRangeChange(ReportDateRange.LAST_MONTH)
                }
                DateChip("۹۰ روز", state.range == ReportDateRange.LAST_90_DAYS) {
                    viewModel.onRangeChange(ReportDateRange.LAST_90_DAYS)
                }
                DateChip("همه", state.range == ReportDateRange.ALL_TIME) {
                    viewModel.onRangeChange(ReportDateRange.ALL_TIME)
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal700)
                }
                return@Column
            }

            val report = state.report
            if (report == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("گزارشی یافت نشد", color = Slate500, fontSize = 13.sp)
                }
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Party header
                item { PartyHeaderCard(report) }

                // Summary cards
                item { SummaryGrid(report) }

                // Export buttons
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { activity?.let { viewModel.printReport(it) } },
                            enabled = !state.isExporting && activity != null,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Teal700)
                        ) {
                            Text("🖨️ چاپ / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700)
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportExcel() },
                            enabled = !state.isExporting,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Green600)
                        ) {
                            Text("📊 Excel/CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green600)
                        }
                    }
                }

                // Section title
                item {
                    Text(
                        "تراکنش‌ها (${report.rows.size})",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Transaction rows
                if (report.rows.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "در این بازه تراکنشی وجود ندارد",
                                color = Slate500, fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(report.rows, key = { it.entryId }) { line ->
                        LedgerRow(line, report.partyKind)
                    }
                }

                // Closing summary
                item {
                    ClosingSummary(report)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Components
// ═══════════════════════════════════════════════════════════

@Composable
private fun DateChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Teal700 else Color.White)
            .clickableCompat(onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            color = if (selected) Color.White else Slate600,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PartyHeaderCard(report: PartyDetailReport) {
    val kindLabel = when (report.partyKind) {
        PartyKind.CUSTOMER -> "مشتری"
        PartyKind.SUPPLIER -> "تأمین‌کننده"
        PartyKind.BOTH -> "مشتری / تأمین‌کننده"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Teal800, Teal600)))
            .padding(18.dp)
    ) {
        Column {
            Text(
                report.partyName,
                color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PDBadge(kindLabel, PDBadgeType.NEUTRAL)
                report.mobile?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "📱 $it",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${JalaliDate.fromEpochMillis(report.fromDate).formatPersian()} تا ${JalaliDate.fromEpochMillis(report.toDate).formatPersian()}",
                color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SummaryGrid(report: PartyDetailReport) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCell(
            label = "افتتاحیه",
            value = formatBalance(report.openingBalanceRials),
            color = if (report.openingBalanceRials >= 0) Rose600 else Green600,
            modifier = Modifier.weight(1f)
        )
        SummaryCell(
            label = "بدهکار",
            value = report.totalDebitRials.toTomanString(),
            color = Rose600,
            modifier = Modifier.weight(1f)
        )
        SummaryCell(
            label = "بستانکار",
            value = report.totalCreditRials.toTomanString(),
            color = Green600,
            modifier = Modifier.weight(1f)
        )
        SummaryCell(
            label = "اختتامیه",
            value = formatBalance(report.closingBalanceRials),
            color = if (report.closingBalanceRials >= 0) Rose600 else Green600,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryCell(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 9.sp, color = Slate500, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun LedgerRow(line: PartyLedgerLine, partyKind: PartyKind) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                line.documentType,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900
            )
            Text(
                JalaliDate.fromEpochMillis(line.date).formatPersian(),
                fontSize = 10.sp, color = Slate500,
                modifier = Modifier.padding(top = 2.dp)
            )
            line.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    fontSize = 10.sp, color = Slate600,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (line.debitRials > 0) {
                Text(
                    "بد: ${line.debitRials.toTomanString()}",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Rose600
                )
            }
            if (line.creditRials > 0) {
                Text(
                    "بس: ${line.creditRials.toTomanString()}",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Green600
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                formatBalance(line.runningBalanceRials),
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (line.runningBalanceRials >= 0) Slate900 else Green600
            )
        }
    }
}

@Composable
private fun ClosingSummary(report: PartyDetailReport) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Teal50)
            .padding(14.dp)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("جمع بدهکار دوره", fontSize = 11.sp, color = Slate600)
                Text(
                    report.totalDebitRials.toTomanString() + " ت",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rose600
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("جمع بستانکار دوره", fontSize = 11.sp, color = Slate600)
                Text(
                    report.totalCreditRials.toTomanString() + " ت",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Green600
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Teal200)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("مانده اختتامیه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal700)
                Text(
                    formatBalance(report.closingBalanceRials),
                    fontSize = 15.sp, fontWeight = FontWeight.Black,
                    color = if (report.closingBalanceRials >= 0) Rose600 else Green600
                )
            }
        }
    }
}

// ═══ Helpers ═══
private fun formatBalance(rials: Long): String {
    return if (rials >= 0) {
        rials.toTomanString() + " بد"
    } else {
        (-rials).toTomanString() + " بس"
    }
}

@Composable
private fun Modifier.clickableCompat(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}