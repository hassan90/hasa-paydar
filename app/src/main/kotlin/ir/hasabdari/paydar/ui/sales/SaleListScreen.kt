package ir.hasabdari.paydar.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.Invoice
import ir.hasabdari.paydar.domain.model.InvoiceType
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.Amber100
import ir.hasabdari.paydar.ui.theme.Amber200
import ir.hasabdari.paydar.ui.theme.Amber700
import ir.hasabdari.paydar.ui.theme.Green100
import ir.hasabdari.paydar.ui.theme.Green200
import ir.hasabdari.paydar.ui.theme.Green600
import ir.hasabdari.paydar.ui.theme.Slate50
import ir.hasabdari.paydar.ui.theme.Slate500
import ir.hasabdari.paydar.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleListScreen(
    onBack: () -> Unit,
    onNewSale: () -> Unit,
    onInvoiceClick: (String) -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: SaleListViewModel = hiltViewModel()
) {
    val invoices by viewModel.invoices.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "فروش‌ها",
                    onBack = onBack,
                    trailing = {
                        Text("${invoices.size} فاکتور", color = Slate500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewSale,
                containerColor = Green600,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "فروش جدید", tint = Color.White) }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(currentRoute = "more", onNavigate = onNavigateBottom)
            }
        }
    ) { padding ->
        if (invoices.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                PDEmptyState(
                    emoji = "🧾",
                    title = "هنوز فروشی ثبت نشده",
                    message = "اولین فروش خود را از POS ثبت کنید",
                    actionText = "🛒 شروع فروش",
                    onAction = onNewSale
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(invoices, key = { it.id }) { inv ->
                SaleRow(inv, onClick = { onInvoiceClick(inv.id) })
            }
        }
    }
}

@Composable
private fun SaleRow(inv: Invoice, onClick: () -> Unit) {
    val isReturn = inv.type == InvoiceType.SALE_RETURN
    val bgColor = if (isReturn) Amber100 else Green100
    val bgColor2 = if (isReturn) Amber200 else Green200
    val icon = if (isReturn) "↩️" else "🧾"
    val titleColor = if (isReturn) Amber700 else Green600

    val statusBadge: Pair<String, PDBadgeType> = when {
        isReturn -> "برگشت" to PDBadgeType.WARNING
        inv.remaining.rials <= 0 -> "تسویه" to PDBadgeType.SUCCESS
        inv.paidAmount.rials > 0 -> "جزئی" to PDBadgeType.LOW
        else -> "پرداخت‌نشده" to PDBadgeType.DANGER
    }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(bgColor, bgColor2))),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isReturn) "برگشت ${inv.number}" else "فروش ${inv.number}",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (isReturn) Amber700 else Slate900
            )
            Text(
                JalaliDate.fromEpochMillis(inv.issueDate).formatPersian(),
                fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp)
            )
            Row(Modifier.padding(top = 4.dp)) {
                PDBadge(statusBadge.first, statusBadge.second)
                Spacer(Modifier.width(6.dp))
                PDBadge(
                    when (inv.settlementType) {
                        SettlementType.CASH -> "نقدی"
                        SettlementType.CREDIT -> "نسیه"
                        SettlementType.MIXED -> "ترکیبی"
                    },
                    PDBadgeType.NEUTRAL
                )
            }
        }
        Text(
            inv.grandTotal.rials.toTomanString() + " ت",
            fontSize = 14.sp, fontWeight = FontWeight.Black,
            color = if (isReturn) Amber700 else Green600
        )
    }
}