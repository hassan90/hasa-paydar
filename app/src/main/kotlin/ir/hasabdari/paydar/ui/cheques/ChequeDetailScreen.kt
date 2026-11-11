package ir.hasabdari.paydar.ui.cheques

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
import androidx.compose.runtime.remember                                    // ← import جدید
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
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.ChequeStatus
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDBadge
import ir.hasabdari.paydar.ui.components.PDBadgeType
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChequeDetailScreen(
    onBack: () -> Unit,
    viewModel: ChequeDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        val m = state.actionMessage
        if (m != null) {
            snackbarHost.showSnackbar(m)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "جزئیات چک", onBack = onBack)
            }
        }
    ) { padding ->
        val cheque = state.cheque
        if (state.isLoading || cheque == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ChequeHeroCard(cheque)

            ChequeInfoCard(state, cheque)

            ChequeStatusCard(cheque)

            ActionButtons(state, cheque, viewModel)

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Hero card
// ═══════════════════════════════════════════════════════════
@Composable
private fun ChequeHeroCard(cheque: Cheque) {
    val isReceived = cheque.kind == ChequeKind.RECEIVED
    val startColor = if (isReceived) Green700 else Rose700
    val endColor = if (isReceived) Green600 else Rose600
    val icon = if (isReceived) "📥" else "📤"

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(startColor, endColor))).padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text(icon, fontSize = 28.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (isReceived) "چک دریافتی" else "چک پرداختی",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium
                    )
                    Text(
                        "شماره ${cheque.number}",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                cheque.amount.rials.toTomanString() + " تومان",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Info card — با استخراج متغیرهای nullable
// ═══════════════════════════════════════════════════════════
@Composable
private fun ChequeInfoCard(state: ChequeDetailUiState, cheque: Cheque) {
    // ⚠️ استخراج به متغیر محلی برای smart cast
    val bankName = cheque.bankName
    val branch = cheque.branch
    val accountNumber = cheque.accountNumber

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White).padding(14.dp)
    ) {
        Text("اطلاعات چک", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate500)
        Spacer(Modifier.height(8.dp))
        DetailRow("طرف حساب", state.partyName)
        DetailRow("تاریخ صدور", JalaliDate.fromEpochMillis(cheque.issueDate).formatPersian())
        DetailRow("سررسید", JalaliDate.fromEpochMillis(cheque.dueDate).formatPersian())
        val daysLeft = cheque.daysToDue(System.currentTimeMillis())
        DetailRow(
            "روزهای مانده",
            if (daysLeft >= 0) "$daysLeft روز" else "گذشته ($daysLeft)"
        )

        if (!bankName.isNullOrBlank()) {
            DetailRow("بانک", bankName)
        }
        if (!branch.isNullOrBlank()) {
            DetailRow("شعبه", branch)
        }
        if (!accountNumber.isNullOrBlank()) {
            DetailRow("شماره حساب", accountNumber)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Status card — با استخراج note
// ═══════════════════════════════════════════════════════════
@Composable
private fun ChequeStatusCard(cheque: Cheque) {
    val note = cheque.note

    val (statusText, statusType) = when (cheque.status) {
        ChequeStatus.IN_SAFE -> "نزد صندوق" to PDBadgeType.NEUTRAL
        ChequeStatus.DEPOSITED -> "واگذار به بانک" to PDBadgeType.INFO
        ChequeStatus.SPENT -> "خرج‌شده" to PDBadgeType.WARNING
        ChequeStatus.CLEARED -> "وصول‌شده" to PDBadgeType.SUCCESS
        ChequeStatus.BOUNCED -> "برگشتی" to PDBadgeType.DANGER
        ChequeStatus.RETURNED -> "مسترد به مشتری" to PDBadgeType.NEUTRAL
        ChequeStatus.CANCELLED -> "ابطال" to PDBadgeType.NEUTRAL
    }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color.White).padding(14.dp)
    ) {
        Text("وضعیت", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate500)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PDBadge(statusText, statusType)
            if (cheque.isFinal) {
                Spacer(Modifier.width(8.dp))
                Text("(نهایی)", fontSize = 10.sp, color = Slate400)
            }
        }
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(note, fontSize = 11.sp, color = Slate600)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Action buttons
// ═══════════════════════════════════════════════════════════
@Composable
private fun ActionButtons(
    state: ChequeDetailUiState,
    cheque: Cheque,
    viewModel: ChequeDetailViewModel
) {
    if (cheque.isFinal) return

    val inProgress = state.actionInProgress
    val isReceived = cheque.kind == ChequeKind.RECEIVED

    when {
        isReceived && cheque.status == ChequeStatus.IN_SAFE -> {
            ActionLabel("اقدامات")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("واریز به بانک", Sky600, !inProgress) { viewModel.deposit() }
                ActionButton("وصول شد", Green600, !inProgress) { viewModel.clear() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("برگشت خورد", Rose600, !inProgress) { viewModel.bounce() }
            }
        }
        isReceived && cheque.status == ChequeStatus.DEPOSITED -> {
            ActionLabel("اقدامات")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("وصول شد", Green600, !inProgress) { viewModel.clear() }
                ActionButton("برگشت خورد", Rose600, !inProgress) { viewModel.bounce() }
            }
        }
        isReceived && cheque.status == ChequeStatus.BOUNCED -> {
            ActionLabel("اقدامات")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("استرداد به مشتری", Amber600, !inProgress) { viewModel.returnToCustomer() }
            }
        }
        !isReceived && cheque.status == ChequeStatus.SPENT -> {
            ActionLabel("اقدامات")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionButton("پرداخت شد", Green600, !inProgress) { viewModel.clear() }
                ActionButton("برگشت خورد", Rose600, !inProgress) { viewModel.bounce() }
            }
        }
    }
}

// ⚠️ RowScope برای weight
@Composable
private fun RowScope.ActionButton(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionLabel(text: String) {
    Text(
        text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900,
        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = Slate500)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}