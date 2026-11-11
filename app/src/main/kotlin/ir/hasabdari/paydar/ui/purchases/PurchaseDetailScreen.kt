package ir.hasabdari.paydar.ui.purchases

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDBadge
import ir.hasabdari.paydar.ui.components.PDBadgeType
import ir.hasabdari.paydar.ui.print.PrintViewModel
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    onBack: () -> Unit,
    onReturn: (String) -> Unit = {},
    viewModel: PurchaseDetailViewModel = hiltViewModel(),
    printViewModel: PrintViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val printState by printViewModel.ui.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    LaunchedEffect(printState.error) {
        printState.error?.let {
            snackbarHost.showSnackbar(it)
            printViewModel.clearError()
        }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "جزئیات فاکتور خرید", onBack = onBack)
            }
        }
    ) { padding ->
        val data = state.data
        if (state.isLoading || data == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }

        val inv = data.invoice
        val lines = data.lines

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══ هدر ═══
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Amber700, Amber500)))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        "فاکتور خرید ${inv.number}",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        JalaliDate.fromEpochMillis(inv.issueDate).formatPersian(),
                        color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PDBadge(
                            when (inv.settlementType.name) {
                                "CASH" -> "نقدی"
                                "CREDIT" -> "نسیه"
                                else -> "ترکیبی"
                            },
                            PDBadgeType.NEUTRAL
                        )
                        if (inv.remaining.rials <= 0) {
                            PDBadge("تسویه", PDBadgeType.SUCCESS)
                        } else if (inv.paidAmount.rials > 0) {
                            PDBadge("پرداخت جزئی", PDBadgeType.LOW)
                        } else {
                            PDBadge("پرداخت‌نشده", PDBadgeType.DANGER)
                        }
                    }
                }
            }

            // ═══ اقلام ═══
            Text("اقلام", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
            lines.forEach { line ->
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
                            "کالا ${line.productId.take(6)}…",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900
                        )
                        Text(
                            "${line.quantityScaled / 1000} × ${line.unitPrice.rials.toTomanString()} ت",
                            fontSize = 10.sp, color = Slate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Text(
                        line.lineTotal.rials.toTomanString() + " ت",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Amber700
                    )
                }
            }

            // ═══ جمع‌ها ═══
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Amber50)
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryRow("جمع اقلام", inv.subtotal.rials.toTomanString() + " ت")
                    if (inv.discountTotal.rials > 0) {
                        SummaryRow(
                            "تخفیف",
                            "− ${inv.discountTotal.rials.toTomanString()} ت",
                            Rose600
                        )
                    }
                    if (inv.taxTotal.rials > 0) {
                        SummaryRow(
                            "مالیات (VAT)",
                            "+ ${inv.taxTotal.rials.toTomanString()} ت",
                            Amber700
                        )
                    }
                    if (inv.shippingCost.rials > 0) {
                        SummaryRow(
                            "حمل",
                            "+ ${inv.shippingCost.rials.toTomanString()} ت",
                            Amber700
                        )
                    }
                    if (inv.otherCosts.rials > 0) {
                        SummaryRow(
                            "سایر هزینه‌ها",
                            "+ ${inv.otherCosts.rials.toTomanString()} ت",
                            Amber700
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Amber200)
                    SummaryRow(
                        "جمع کل",
                        inv.grandTotal.rials.toTomanString() + " ت",
                        Amber700, bold = true, big = true
                    )
                    SummaryRow(
                        "پرداخت‌شده",
                        inv.paidAmount.rials.toTomanString() + " ت",
                        Green600
                    )
                    if (inv.remaining.rials > 0) {
                        SummaryRow(
                            "مانده",
                            inv.remaining.rials.toTomanString() + " ت",
                            Rose600, bold = true
                        )
                    }
                }
            }

            // ═══ توضیحات ═══
            if (!inv.notes.isNullOrBlank()) {
                Text("توضیحات", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Text(inv.notes ?: "", fontSize = 12.sp, color = Slate700)
                }
            }

            // ═══ دکمه چاپ / PDF ═══
            OutlinedButton(
                onClick = {
                    activity?.let { printViewModel.printInvoice(inv.id, it) }
                },
                enabled = !printState.isLoading && activity != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Teal700)
            ) {
                if (printState.isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = Teal700,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "🖨️ چاپ فاکتور / ذخیره PDF",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal700
                    )
                }
            }

            // ═══ دکمه برگشت ═══
            Button(
                onClick = { onReturn(inv.id) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rose600)
            ) {
                Text(
                    "↩️ برگشت به خرید",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════════════════════
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = Slate900,
    bold: Boolean = false,
    big: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = if (big) 14.sp else 11.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) Slate900 else Slate600
        )
        Text(
            value,
            fontSize = if (big) 16.sp else 12.sp,
            fontWeight = if (bold || big) FontWeight.Black else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/**
 * پیدا کردن Activity از Context — لازم برای WebView و PrintManager
 */
private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}