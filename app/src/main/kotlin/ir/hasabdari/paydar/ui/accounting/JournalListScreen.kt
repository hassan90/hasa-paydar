package ir.hasabdari.paydar.ui.accounting

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
import ir.hasabdari.paydar.domain.model.JournalEntry
import ir.hasabdari.paydar.domain.model.JournalSourceType
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalListScreen(
    onBack: () -> Unit,
    onAddManual: () -> Unit,
    onEntryClick: (String) -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: JournalListViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "دفتر روزنامه", onBack = onBack)
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddManual,
                containerColor = Amber500
            ) { Icon(Icons.Default.Add, contentDescription = "سند دستی", tint = Color.White) }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(currentRoute = "more", onNavigate = onNavigateBottom)
            }
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                PDEmptyState(
                    emoji = "📋",
                    title = "هنوز سندی ثبت نشده",
                    message = "با ثبت اولین دریافت/پرداخت یا خرید/فروش، اسناد اینجا نمایش داده می‌شوند",
                    actionText = "➕ سند دستی",
                    onAction = onAddManual
                )
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "${entries.size} سند حسابداری",
                    fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.SemiBold
                )
            }
            items(entries, key = { it.id }) { entry ->
                JournalCard(entry, onClick = { onEntryClick(entry.id) })
            }
        }
    }
}

@Composable
private fun JournalCard(entry: JournalEntry, onClick: () -> Unit) {
    val (icon, bg1, bg2) = when (entry.sourceType) {
        JournalSourceType.CASH_RECEIPT -> Triple("📥", Green100, Green200)
        JournalSourceType.CASH_PAYMENT -> Triple("📤", Rose100, Rose200)
        JournalSourceType.CASH_TRANSFER -> Triple("↔️", Sky100, Sky200)
        JournalSourceType.MANUAL -> Triple("✏️", Amber100, Amber200)
        JournalSourceType.INVOICE_SALE -> Triple("🧾", Green100, Green200)
        JournalSourceType.INVOICE_PURCHASE -> Triple("🛒", Amber100, Amber200)
        else -> Triple("📋", Slate100, Slate200)
    }
    val date = JalaliDate.fromEpochMillis(entry.entryDate).formatPersian()

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(bg1, bg2))),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "سند #${JalaliDate.toPersianDigits(entry.number.toString())}",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900
            )
            Text(entry.description, fontSize = 11.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
            Text(date, fontSize = 10.sp, color = Slate400, modifier = Modifier.padding(top = 2.dp))
        }
        Text("←", fontSize = 16.sp, color = Slate400)
    }
}