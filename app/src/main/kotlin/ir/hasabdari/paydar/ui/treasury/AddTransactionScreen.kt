package ir.hasabdari.paydar.ui.treasury

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.CashAccount
import ir.hasabdari.paydar.domain.model.CashTransactionMethod
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDChip
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    initialType: CashTransactionType = CashTransactionType.RECEIPT,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    LaunchedEffect(initialType) { viewModel.initType(initialType) }
    val state = viewModel.state
    val accounts by viewModel.accounts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()

    LaunchedEffect(accounts) {
        if (state.cashAccountId.isBlank() && accounts.isNotEmpty()) {
            viewModel.onCashAccountChange(accounts.first().id)
        }
    }

    val title = when (state.type) {
        CashTransactionType.RECEIPT -> "دریافت وجه"
        CashTransactionType.PAYMENT -> "پرداخت"
        CashTransactionType.TRANSFER -> "انتقال وجه"
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = title, onBack = onBack)
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Box(Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { viewModel.submit(onSaved) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.submitting && accounts.isNotEmpty(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        if (state.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("ثبت", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        if (accounts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("ابتدا یک حساب نقدی (صندوق یا بانک) بسازید", color = Slate500, fontSize = 13.sp)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ═══ انتخاب نوع (فقط اگر initialType null باشد) ═══
            FormSection(title = "نوع تراکنش") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("📥 دریافت", state.type == CashTransactionType.RECEIPT, onClick = { viewModel.onTypeChange(CashTransactionType.RECEIPT) })
                    PDChip("📤 پرداخت", state.type == CashTransactionType.PAYMENT, onClick = { viewModel.onTypeChange(CashTransactionType.PAYMENT) })
                    PDChip("↔️ انتقال", state.type == CashTransactionType.TRANSFER, onClick = { viewModel.onTypeChange(CashTransactionType.TRANSFER) })
                }
            }

            // ═══ حساب مبدأ ═══
            FormSection(title = if (state.type == CashTransactionType.TRANSFER) "از حساب" else "حساب") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    accounts.forEach { acc ->
                        AccountSelector(acc, acc.id == state.cashAccountId) { viewModel.onCashAccountChange(acc.id) }
                    }
                }
            }

            // ═══ حساب مقصد (فقط برای انتقال) ═══
            if (state.type == CashTransactionType.TRANSFER) {
                FormSection(title = "به حساب") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        accounts.filter { it.id != state.cashAccountId }.forEach { acc ->
                            AccountSelector(acc, acc.id == state.toCashAccountId) { viewModel.onToCashAccountChange(acc.id) }
                        }
                    }
                }
            }

            // ═══ طرف حساب (فقط برای دریافت/پرداخت) ═══
            if (state.type == CashTransactionType.RECEIPT || state.type == CashTransactionType.PAYMENT) {
                val parties = if (state.type == CashTransactionType.RECEIPT) customers else suppliers
                if (parties.isNotEmpty()) {
                    FormSection(title = if (state.type == CashTransactionType.RECEIPT) "از مشتری (اختیاری)" else "به تأمین‌کننده (اختیاری)") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            parties.take(5).forEach { p ->
                                PartySelector(p, p.id == state.partyId) { viewModel.onPartyChange(p.id) }
                            }
                        }
                    }
                }
            }

            // ═══ مبلغ و روش ═══
            FormSection(title = "مبلغ و روش") {
                PDTextField(
                    label = "مبلغ (تومان) *",
                    value = state.amountToman,
                    onValueChange = viewModel::onAmountChange,
                    placeholder = "۰",
                    keyboardType = KeyboardType.Number
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PDChip("💵 نقد", state.method == CashTransactionMethod.CASH, onClick = { viewModel.onMethodChange(CashTransactionMethod.CASH) })
                    PDChip("💳 کارت", state.method == CashTransactionMethod.CARD, onClick = { viewModel.onMethodChange(CashTransactionMethod.CARD) })
                    PDChip("🏦 انتقال", state.method == CashTransactionMethod.TRANSFER, onClick = { viewModel.onMethodChange(CashTransactionMethod.TRANSFER) })
                }
            }

            FormSection(title = "توضیحات") {
                PDTextField(
                    label = "توضیحات (اختیاری)",
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = "مثلاً: بابت تسویه فاکتور ۱۰۲",
                    singleLine = false,
                    minLines = 2
                )
            }

            if (state.submitError != null) {
                Text(state.submitError, color = Rose600, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun AccountSelector(acc: CashAccount, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Teal50 else Color.White)
            .border(1.dp, if (selected) Teal500 else Slate200, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(acc.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = Teal700, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PartySelector(p: Party, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Teal50 else Color.White)
            .border(1.dp, if (selected) Teal500 else Slate200, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900, modifier = Modifier.weight(1f))
        if (selected) Text("✓", color = Teal700, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Teal600))
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}