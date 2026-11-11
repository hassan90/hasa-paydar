package ir.hasabdari.paydar.ui.cheques

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
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.ChequeStatus
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.Amber500
import ir.hasabdari.paydar.ui.theme.Green400
import ir.hasabdari.paydar.ui.theme.Green600
import ir.hasabdari.paydar.ui.theme.Rose400
import ir.hasabdari.paydar.ui.theme.Rose600
import ir.hasabdari.paydar.ui.theme.Slate50
import ir.hasabdari.paydar.ui.theme.Slate500
import ir.hasabdari.paydar.ui.theme.Slate900
import ir.hasabdari.paydar.ui.theme.Teal700

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChequeListScreen(
    onBack: () -> Unit,
    onAddClick: (ChequeKind) -> Unit,
    onChequeClick: (String) -> Unit,
    onCalendarClick: () -> Unit = {},
    onNavigateBottom: (String) -> Unit = {},
    viewModel: ChequeListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val defaultKind = state.filter ?: ChequeKind.RECEIVED

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "چک‌ها",
                    onBack = onBack,
                    trailing = {
                        Text(
                            "📅",
                            fontSize = 20.sp,
                            modifier = Modifier.clickable(onClick = onCalendarClick).padding(8.dp)
                        )
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddClick(defaultKind) },
                containerColor = Amber500,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "چک جدید", tint = Color.White) }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(currentRoute = "more", onNavigate = onNavigateBottom)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PDChip("همه", state.filter == null, onClick = { viewModel.onFilterChange(null) })
                PDChip("📥 دریافتی", state.filter == ChequeKind.RECEIVED, onClick = { viewModel.onFilterChange(ChequeKind.RECEIVED) })
                PDChip("📤 پرداختی", state.filter == ChequeKind.ISSUED, onClick = { viewModel.onFilterChange(ChequeKind.ISSUED) })
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal700)
                }
            } else if (state.items.isEmpty()) {
                PDEmptyState(
                    emoji = "🧾",
                    title = "هنوز چکی ثبت نشده",
                    message = "اولین چک دریافتی یا پرداختی خود را ثبت کنید",
                    actionText = "➕ افزودن چک",
                    onAction = { onAddClick(defaultKind) }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items, key = { it.cheque.id }) { item ->
                        ChequeRow(item, onClick = { onChequeClick(item.cheque.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ChequeRow(item: ChequeWithParty, onClick: () -> Unit) {
    val cheque = item.cheque
    val isReceived = cheque.kind == ChequeKind.RECEIVED
    val avatar1 = if (isReceived) Green400 else Rose400
    val avatar2 = if (isReceived) Green600 else Rose600
    val icon = if (isReceived) "📥" else "📤"
    val statusText: String
    val statusType: PDBadgeType
    when (cheque.status) {
        ChequeStatus.IN_SAFE -> { statusText = "نزد صندوق"; statusType = PDBadgeType.NEUTRAL }
        ChequeStatus.DEPOSITED -> { statusText = "واگذار"; statusType = PDBadgeType.INFO }
        ChequeStatus.SPENT -> { statusText = "خرج‌شده"; statusType = PDBadgeType.WARNING }
        ChequeStatus.CLEARED -> { statusText = "وصول"; statusType = PDBadgeType.SUCCESS }
        ChequeStatus.BOUNCED -> { statusText = "برگشتی"; statusType = PDBadgeType.DANGER }
        ChequeStatus.RETURNED -> { statusText = "مسترد"; statusType = PDBadgeType.NEUTRAL }
        ChequeStatus.CANCELLED -> { statusText = "ابطال"; statusType = PDBadgeType.NEUTRAL }
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(avatar1, avatar2))),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.partyName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(
                "چک ${cheque.number} · ${JalaliDate.fromEpochMillis(cheque.dueDate).formatPersian()}",
                fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(4.dp))
            PDBadge(statusText, statusType)
        }
        Text(
            cheque.amount.rials.toTomanString() + " ت",
            fontSize = 13.sp, fontWeight = FontWeight.Black,
            color = if (isReceived) Green600 else Rose600
        )
    }
}