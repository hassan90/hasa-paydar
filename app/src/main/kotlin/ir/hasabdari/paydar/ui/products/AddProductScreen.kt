package ir.hasabdari.paydar.ui.products

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val state = viewModel.state

    Scaffold(
        containerColor = Slate50,
        topBar = {
            TopAppBar(
                title = { Text("افزودن کالا", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("→", fontSize = 22.sp, color = androidx.compose.ui.graphics.Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal700,
                    titleContentColor = androidx.compose.ui.graphics.Color.White
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = androidx.compose.ui.graphics.Color.White) {
                Box(Modifier.padding(16.dp).navigationBarsPadding()) {
                    Button(
                        onClick = { viewModel.submit(onSaved) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.submitting,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        if (state.submitting) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("ذخیره کالا", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Photo upload placeholder
            Box(
                modifier = Modifier.fillMaxWidth().height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Teal50, Teal100))),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White),
                        contentAlignment = Alignment.Center
                    ) { Text("📷", fontSize = 28.sp) }
                    Spacer(Modifier.height(8.dp))
                    Text("افزودن تصویر کالا", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal700)
                    Text("اختیاری · PNG یا JPG", fontSize = 10.sp, color = Slate500)
                }
            }

            FormSection(title = "اطلاعات اصلی") {
                PDTextField(
                    label = "نام کالا *",
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "مثلاً شیر پرچرب",
                    isError = state.nameError != null,
                    errorMessage = state.nameError
                )
                PDTextField(
                    label = "کد کالا *",
                    value = state.code,
                    onValueChange = viewModel::onCodeChange,
                    placeholder = "مثلاً ۱۰۰۱",
                    isError = state.codeError != null,
                    errorMessage = state.codeError
                )
                PDTextField(
                    label = "بارکد (اختیاری)",
                    value = state.barcode,
                    onValueChange = viewModel::onBarcodeChange,
                    placeholder = "با دوربین یا دستی",
                    keyboardType = KeyboardType.Number
                )
            }

            FormSection(title = "قیمت‌ها (تومان)") {
                PDTextField(
                    label = "قیمت خرید",
                    value = state.purchasePriceToman,
                    onValueChange = viewModel::onPurchasePriceChange,
                    placeholder = "۰",
                    keyboardType = KeyboardType.Number
                )
                PDTextField(
                    label = "قیمت فروش",
                    value = state.salePriceToman,
                    onValueChange = viewModel::onSalePriceChange,
                    placeholder = "۰",
                    keyboardType = KeyboardType.Number
                )
            }

            FormSection(title = "موجودی") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "موجودی اولیه",
                            value = state.initialStock,
                            onValueChange = viewModel::onInitialStockChange,
                            placeholder = "۰",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "حداقل موجودی",
                            value = state.minStock,
                            onValueChange = viewModel::onMinStockChange,
                            placeholder = "۰",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
            }

            FormSection(title = "توضیحات") {
                PDTextField(
                    label = "توضیحات",
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    placeholder = "اختیاری",
                    singleLine = false,
                    minLines = 3
                )
            }

            if (state.submitError != null) {
                Text(state.submitError, color = Rose600, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(androidx.compose.ui.graphics.Color.White)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}