package ir.hasabdari.paydar.ui.treasury

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.CashAccountKind
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDChip
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCashAccountScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddCashAccountViewModel = hiltViewModel()
) {
    val state = viewModel.state

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "افزودن حساب", onBack = onBack)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        if (state.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("ذخیره", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormSection(title = "نوع حساب") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("💵 صندوق", state.kind == CashAccountKind.CASHBOX, onClick = { viewModel.onKindChange(CashAccountKind.CASHBOX) })
                    PDChip("🏦 بانک", state.kind == CashAccountKind.BANK, onClick = { viewModel.onKindChange(CashAccountKind.BANK) })
                }
            }

            FormSection(title = "اطلاعات اصلی") {
                PDTextField(
                    label = "نام حساب *",
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = if (state.kind == CashAccountKind.CASHBOX) "مثلاً صندوق اصلی" else "مثلاً بانک ملت",
                    isError = state.nameError != null,
                    errorMessage = state.nameError
                )
                if (state.kind == CashAccountKind.BANK) {
                    PDTextField(
                        label = "نام بانک",
                        value = state.bankName,
                        onValueChange = viewModel::onBankNameChange,
                        placeholder = "مثلاً ملت، صادرات"
                    )
                    PDTextField(
                        label = "شماره حساب (اختیاری)",
                        value = state.accountNumber,
                        onValueChange = viewModel::onAccountNumberChange,
                        placeholder = "۱۲۳۴۵۶۷۸۹۰",
                        keyboardType = KeyboardType.Number
                    )
                }
                PDTextField(
                    label = "موجودی اولیه (تومان)",
                    value = state.openingBalanceToman,
                    onValueChange = viewModel::onOpeningBalanceChange,
                    placeholder = "۰",
                    keyboardType = KeyboardType.Number
                )
            }

            if (state.submitError != null) {
                Text(state.submitError, color = Rose600, fontSize = 12.sp)
            }
        }
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