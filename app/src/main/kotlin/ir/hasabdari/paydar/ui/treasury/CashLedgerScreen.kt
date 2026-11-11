package ir.hasabdari.paydar.ui.treasury

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ir.hasabdari.paydar.domain.model.CashTransaction
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDHeroCard
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashLedgerScreen(
    onBack: () -> Unit,
    viewModel: CashLedgerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = state.account?.name ?: "کاردکس حساب", onBack = onBack)
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                PDHeroCard(
                    label = "💰 موجودی فعلی",
                    amount = (state.balanceRials / 10).toTomanString(),
                    currency = "تومان"
                )
            }
            item {
                Text(
                    "تاریخچه تراکنش‌ها (${state.transactions.size})",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (state.transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("هنوز تراکنشی ثبت نشده", fontSize = 12.sp, color = Slate500)
                    }
                }
            } else {
                items(state.transactions, key = { it.id }) { txn ->
                    LedgerRow(txn, accountId = state.account?.id ?: "")
                }
            }
        }
    }
}

// ═══ استایل ردیف کاردکس — data class اختصاصی ═══
private data class LedgerRowStyle(
    val icon: String,
    val bg1: Color,
    val bg2: Color,
    val sign: String,
    val amountColor: Color
)

@Composable
private fun LedgerRow(txn: CashTransaction, accountId: String) {
    val isIncoming = when (txn.type) {
        CashTransactionType.RECEIPT -> true
        CashTransactionType.PAYMENT -> false
        CashTransactionType.TRANSFER -> txn.toCashAccountId == accountId
    }

    val style: LedgerRowStyle = when {
        txn.type == CashTransactionType.RECEIPT -> LedgerRowStyle("📥", Green100, Green200, "+", Green600)
        txn.type == CashTransactionType.PAYMENT -> LedgerRowStyle("📤", Rose100, Rose200, "−", Rose600)
        isIncoming -> LedgerRowStyle("↔️", Sky100, Sky200, "+", Sky600)
        else -> LedgerRowStyle("↔️", Sky100, Sky200, "−", Sky600)
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(style.bg1, style.bg2))),
            contentAlignment = Alignment.Center
        ) { Text(style.icon, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                txn.description ?: (if (txn.type == CashTransactionType.RECEIPT) "دریافت" else if (txn.type == CashTransactionType.PAYMENT) "پرداخت" else "انتقال"),
                fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900
            )
            Text("${txn.method}", fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            "${style.sign}${txn.amount.rials.toTomanString()}",
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = style.amountColor
        )
    }
}