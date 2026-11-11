package ir.hasabdari.paydar.ui.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalDetailScreen(
    onBack: () -> Unit,
    viewModel: JournalDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "سند حسابداری", onBack = onBack)
            }
        }
    ) { padding ->
        val entry = state.entry
        if (state.isLoading || entry == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }

        val e = entry.entry
        val lines = entry.lines
        val isBalanced = entry.totalDebit == entry.totalCredit

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══ کارت هدر ═══
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Teal800, Teal600)))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        "سند شماره ${JalaliDate.toPersianDigits(e.number.toString())}",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        JalaliDate.fromEpochMillis(e.entryDate).formatPersian(),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        e.description,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp
                    )
                }
            }

            Text(
                "ردیف‌های سند",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            lines.forEach { line ->
                val desc = line.description
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
                            line.accountCode,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                        if (!desc.isNullOrBlank()) {
                            Text(
                                desc,
                                fontSize = 11.sp,
                                color = Slate600
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (line.debit > 0) {
                            Text(
                                "بدهکار: ${line.debit.toTomanString()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green600
                            )
                        }
                        if (line.credit > 0) {
                            Text(
                                "بستانکار: ${line.credit.toTomanString()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Rose600
                            )
                        }
                    }
                }
            }

            // ═══ جمع/مانده ═══
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isBalanced) Green50 else Rose50)
                    .padding(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("جمع بدهکار", fontSize = 10.sp, color = Slate500)
                        Text(
                            entry.totalDebit.toTomanString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green600
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isBalanced) "✓" else "≠",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBalanced) Green600 else Rose600
                        )
                        Text(
                            if (isBalanced) "متوازن" else "نامتوازن",
                            fontSize = 10.sp,
                            color = if (isBalanced) Green600 else Rose600
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("جمع بستانکار", fontSize = 10.sp, color = Slate500)
                        Text(
                            entry.totalCredit.toTomanString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose600
                        )
                    }
                }
            }
        }
    }
}