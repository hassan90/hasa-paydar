package ir.hasabdari.paydar.ui.accounting

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialBalanceScreen(
    onBack: () -> Unit,
    viewModel: TrialBalanceViewModel = hiltViewModel()
) {
    val data by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "تراز آزمایشی", onBack = onBack)
            }
        }
    ) { padding ->
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }
        val tb = data!!

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (tb.isBalanced) Green50 else Rose50).padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (tb.isBalanced) "✓" else "✗", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (tb.isBalanced) Green600 else Rose600)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                if (tb.isBalanced) "تراز متوازن است" else "تراز نامتوازن",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                color = if (tb.isBalanced) Green600 else Rose600
                            )
                            Text("جمع بدهکار = جمع بستانکار", fontSize = 10.sp, color = Slate500)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Slate100).padding(10.dp)) {
                    Text("حساب", modifier = Modifier.weight(2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate600)
                    Text("بدهکار", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Green600, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text("بستانکار", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Rose600, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }

            if (tb.rows.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("هنوز سندی ثبت نشده", color = Slate500, fontSize = 12.sp)
                    }
                }
            } else {
                items(tb.rows) { row ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(10.dp)) {
                        Column(Modifier.weight(2f)) {
                            Text(row.accountName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            Text(row.accountCode, fontSize = 9.sp, color = Slate400)
                        }
                        Text(
                            if (row.debit > 0) row.debit.toTomanString() else "—",
                            modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Green600,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        Text(
                            if (row.credit > 0) row.credit.toTomanString() else "—",
                            modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Rose600,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Teal50).padding(12.dp)) {
                    Text("جمع کل", modifier = Modifier.weight(2f), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Teal700)
                    Text(tb.totalDebit.toTomanString(), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Green600, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    Text(tb.totalCredit.toTomanString(), modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Rose600, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}