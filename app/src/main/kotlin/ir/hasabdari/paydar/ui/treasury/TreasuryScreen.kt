package ir.hasabdari.paydar.ui.treasury

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.domain.model.CashAccountWithBalance
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.TreasuryTransactionRow
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasuryScreen(
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onAddReceipt: () -> Unit,
    onAddPayment: () -> Unit,
    onAddTransfer: () -> Unit,
    onAccountClick: (String) -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: TreasuryViewModel = hiltViewModel()
) {
    val overview by viewModel.overview.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "خزانه",
                    onBack = onBack,
                    trailing = {
                        IconButton(onClick = onAddAccount) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن حساب", tint = Teal700)
                        }
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
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PDHeroCard(
                    label = "💰 موجودی کل خزانه",
                    amount = overview.totalBalance.rials.toTomanString(),
                    currency = "تومان"
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PDQuickAction(
                        icon = "📥", label = "دریافت", color = PDKpiColor.GREEN,
                        onClick = onAddReceipt, modifier = Modifier.weight(1f)
                    )
                    PDQuickAction(
                        icon = "📤", label = "پرداخت", color = PDKpiColor.ROSE,
                        onClick = onAddPayment, modifier = Modifier.weight(1f)
                    )
                    PDQuickAction(
                        icon = "↔️", label = "انتقال", color = PDKpiColor.SKY,
                        onClick = onAddTransfer, modifier = Modifier.weight(1f)
                    )
                }
            }

            if (overview.cashboxes.isNotEmpty()) {
                item { PDSectionHeader(title = "💵 صندوق‌ها") }
                items(overview.cashboxes, key = { it.account.id }) { acc ->
                    CashAccountCard(acc, onClick = { onAccountClick(acc.account.id) })
                }
            }

            if (overview.banks.isNotEmpty()) {
                item { PDSectionHeader(title = "🏦 بانک‌ها") }
                items(overview.banks, key = { it.account.id }) { acc ->
                    CashAccountCard(acc, onClick = { onAccountClick(acc.account.id) })
                }
            }

            if (overview.cashboxes.isEmpty() && overview.banks.isEmpty()) {
                item {
                    PDEmptyState(
                        emoji = "💰",
                        title = "هنوز حسابی نساخته‌اید",
                        message = "اولین صندوق یا حساب بانکی خود را اضافه کنید",
                        actionText = "➕ افزودن حساب",
                        onAction = onAddAccount
                    )
                }
            }

            if (overview.recentTransactions.isNotEmpty()) {
                item {
                    PDSectionHeader(title = "🕐 آخرین تراکنش‌ها", actionText = "همه ←", onAction = { })
                }
                items(overview.recentTransactions, key = { it.id }) { txn ->
                    TreasuryTxnRow(txn)
                }
            }
        }
    }
}

@Composable
private fun CashAccountCard(acc: CashAccountWithBalance, onClick: () -> Unit) {
    val (icon, bg1, bg2) = when (acc.account.kind) {
        CashAccountKind.CASHBOX -> Triple("💵", Green100, Green200)
        CashAccountKind.BANK -> Triple("🏦", Sky100, Sky200)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(bg1, bg2))),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 24.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(acc.account.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
            if (acc.account.kind == CashAccountKind.BANK && !acc.account.accountNumber.isNullOrBlank()) {
                Text("شماره: ${acc.account.accountNumber}", fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(
            acc.currentBalance.rials.toTomanString() + " ت",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (acc.currentBalance.isNegative) Rose600 else Teal700
        )
    }
}

// ═══ استایل ردیف تراکنش — data class اختصاصی، بدون Tuple5 ═══
private data class TxnRowStyle(
    val icon: String,
    val bg1: Color,
    val bg2: Color,
    val sign: String,
    val amountColor: Color
)

@Composable
private fun TreasuryTxnRow(txn: TreasuryTransactionRow) {
    val style: TxnRowStyle = when (txn.type) {
        CashTransactionType.RECEIPT -> TxnRowStyle("📥", Green100, Green200, "+", Green600)
        CashTransactionType.PAYMENT -> TxnRowStyle("📤", Rose100, Rose200, "−", Rose600)
        CashTransactionType.TRANSFER -> TxnRowStyle("↔️", Sky100, Sky200, "", Sky600)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(style.bg1, style.bg2))),
            contentAlignment = Alignment.Center
        ) { Text(style.icon, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                txn.partyName ?: txn.description ?: txn.accountName,
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900
            )
            Text(txn.accountName, fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            "${style.sign}${txn.amount.rials.toTomanString()}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = style.amountColor
        )
    }
}