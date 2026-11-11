package ir.hasabdari.paydar.ui.accounting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.Account
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualJournalScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddManualJournalViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val accounts by viewModel.accounts.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "سند دستی", onBack = onBack)
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Box(Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { viewModel.submit(onSaved) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.submitting && state.isBalanced,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isBalanced) Teal700 else Slate200
                        )
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp
                            )
                        } else {
                            Text("ثبت سند", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PDTextField(
                label = "شرح سند *",
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                placeholder = "مثلاً: هزینه اجاره آبان ماه"
            )

            Text(
                "ردیف‌های سند",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )

            state.lines.forEachIndexed { idx, line ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "ردیف ${idx + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.lines.size > 2) {
                            Text(
                                "حذف",
                                fontSize = 11.sp,
                                color = Rose600,
                                modifier = Modifier.clickable { viewModel.removeLine(idx) }
                            )
                        }
                    }

                    AccountPicker(
                        accounts = accounts,
                        selectedCode = line.accountCode,
                        onSelected = { code -> viewModel.updateLine(idx) { it.copy(accountCode = code) } }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) {
                            PDTextField(
                                label = "بدهکار (ت)",
                                value = line.debit,
                                onValueChange = { v ->
                                    viewModel.updateLine(idx) {
                                        it.copy(debit = v.filter { c -> c.isDigit() }, credit = "")
                                    }
                                },
                                placeholder = "۰",
                                keyboardType = KeyboardType.Number
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            PDTextField(
                                label = "بستانکار (ت)",
                                value = line.credit,
                                onValueChange = { v ->
                                    viewModel.updateLine(idx) {
                                        it.copy(credit = v.filter { c -> c.isDigit() }, debit = "")
                                    }
                                },
                                placeholder = "۰",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }

                    PDTextField(
                        label = "توضیح (اختیاری)",
                        value = line.description,
                        onValueChange = { v ->
                            viewModel.updateLine(idx) { it.copy(description = v) }
                        }
                    )
                }
            }

            TextButton(onClick = viewModel::addLine) {
                Text("➕ افزودن ردیف", color = Teal700, fontWeight = FontWeight.Bold)
            }

            // چک توازن
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (state.isBalanced) Green50 else Amber50)
                    .padding(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("جمع بدهکار", fontSize = 10.sp, color = Slate500)
                        Text(
                            state.totalDebit.toTomanString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Green600
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (state.isBalanced) "✓" else "≠",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isBalanced) Green600 else Amber700
                        )
                        Text(
                            if (state.isBalanced) "متوازن" else "نامتوازن",
                            fontSize = 10.sp,
                            color = if (state.isBalanced) Green600 else Amber700
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("جمع بستانکار", fontSize = 10.sp, color = Slate500)
                        Text(
                            state.totalCredit.toTomanString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose600
                        )
                    }
                }
            }

            state.error?.let { Text(it, color = Rose600, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun AccountPicker(
    accounts: List<Account>,
    selectedCode: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.find { it.code == selectedCode }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (selected != null) "${selected.code} — ${selected.name}" else "انتخاب حساب",
                fontSize = 12.sp,
                color = if (selected != null) Slate900 else Slate400,
                modifier = Modifier.weight(1f)
            )
            Text("▼", fontSize = 10.sp, color = Slate400)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            accounts.forEach { acc ->
                DropdownMenuItem(
                    text = { Text("${acc.code} — ${acc.name}", fontSize = 12.sp) },
                    onClick = {
                        onSelected(acc.code)
                        expanded = false
                    }
                )
            }
        }
    }
}