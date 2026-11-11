package ir.hasabdari.paydar.ui.cheques

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDChip
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChequeScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddChequeViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val parties by viewModel.parties.collectAsState()
    var showPartyPicker by remember { mutableStateOf(false) }

    val filteredParties = parties.filter {
        if (state.kind == ChequeKind.RECEIVED) it.isCustomer else it.isSupplier
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = if (state.kind == ChequeKind.RECEIVED) "چک دریافتی جدید" else "چک پرداختی جدید",
                    onBack = onBack
                )
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Box(Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { viewModel.submit(onSaved) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.submitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.kind == ChequeKind.RECEIVED) Green600 else Rose600
                        )
                    ) {
                        if (state.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("ثبت چک", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // نوع چک
            FormSection("نوع چک") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("📥 دریافتی", state.kind == ChequeKind.RECEIVED, onClick = { viewModel.onKindChange(ChequeKind.RECEIVED) })
                    PDChip("📤 پرداختی", state.kind == ChequeKind.ISSUED, onClick = { viewModel.onKindChange(ChequeKind.ISSUED) })
                }
            }

            // طرف حساب
            FormSection("طرف حساب") {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, if (state.partyName.isBlank()) Slate200 else Teal500, RoundedCornerShape(10.dp))
                        .clickable { showPartyPicker = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (state.partyName.isBlank()) "انتخاب طرف حساب..." else state.partyName,
                        fontSize = 12.sp,
                        color = if (state.partyName.isBlank()) Slate400 else Slate900,
                        fontWeight = if (state.partyName.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("▼", fontSize = 10.sp, color = Slate400)
                }
            }

            // اطلاعات چک
            FormSection("اطلاعات چک") {
                PDTextField(
                    label = "شماره چک *",
                    value = state.number,
                    onValueChange = viewModel::onNumberChange,
                    placeholder = "مثلاً ۱۲۳۴۵۶"
                )
                PDTextField(
                    label = "مبلغ (تومان) *",
                    value = state.amountText,
                    onValueChange = viewModel::onAmountChange,
                    placeholder = "۰",
                    keyboardType = KeyboardType.Number
                )
            }

            // تاریخ سررسید
            FormSection("سررسید") {
                Text("چه مدت بعد؟", fontSize = 11.sp, color = Slate500)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(7, 14, 30, 60, 90).forEach { d ->
                        PDChip(
                            "${d} روز",
                            state.dueInDays == d,
                            onClick = { viewModel.onDueInDaysChange(d) }
                        )
                    }
                }
                OutlinedTextField(
                    value = state.dueInDays.toString(),
                    onValueChange = { v -> v.filter { it.isDigit() }.toIntOrNull()?.let { viewModel.onDueInDaysChange(it) } },
                    label = { Text("یا روز دلخواه", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // بانک
            FormSection("اطلاعات بانکی (اختیاری)") {
                PDTextField(
                    label = "نام بانک",
                    value = state.bankName,
                    onValueChange = viewModel::onBankNameChange,
                    placeholder = "مثلاً ملت، صادرات"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "شعبه",
                            value = state.branch,
                            onValueChange = viewModel::onBranchChange,
                            placeholder = "اختیاری"
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "شماره حساب",
                            value = state.accountNumber,
                            onValueChange = viewModel::onAccountNumberChange,
                            placeholder = "اختیاری",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
            }

            FormSection("توضیحات") {
                PDTextField(
                    label = "توضیحات",
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    placeholder = "اختیاری",
                    singleLine = false,
                    minLines = 2
                )
            }

            state.error?.let {
                Text(it, color = Rose600, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showPartyPicker) {
        PartyPickerDialog(
            parties = filteredParties,
            onSelect = { p -> viewModel.onPartyChange(p); showPartyPicker = false },
            onDismiss = { showPartyPicker = false }
        )
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
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun PartyPickerDialog(
    parties: List<Party>,
    onSelect: (Party) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب طرف حساب", fontWeight = FontWeight.Bold) },
        text = {
            if (parties.isEmpty()) {
                Text("طرف حسابی ثبت نشده — ابتدا از بخش اشخاص اضافه کنید", fontSize = 12.sp, color = Slate500)
            } else {
                LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(parties.size) { i ->
                        val p = parties[i]
                        Text(
                            "${p.name}${if (!p.mobile.isNullOrBlank()) " · ${p.mobile}" else ""}",
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { onSelect(p) }
                                .padding(vertical = 10.dp)
                        )
                        HorizontalDivider(color = Slate100)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}