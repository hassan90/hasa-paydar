package ir.hasabdari.paydar.ui.parties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.ui.components.PDChip
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartyScreen(
    initialKind: PartyKind = PartyKind.CUSTOMER,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddPartyViewModel = hiltViewModel()
) {
    LaunchedEffect(initialKind) { viewModel.initKind(initialKind) }
    val state = viewModel.state

    Scaffold(
        containerColor = Slate50,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.kind) {
                            PartyKind.CUSTOMER -> "افزودن مشتری"
                            PartyKind.SUPPLIER -> "افزودن تأمین‌کننده"
                            PartyKind.BOTH -> "افزودن شخص"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("→", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal700, titleContentColor = Color.White
                )
            )
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
            // ═══ انتخاب نوع ═══
            FormSection(title = "نوع") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("👥 مشتری", state.kind == PartyKind.CUSTOMER, onClick = { viewModel.onKindChange(PartyKind.CUSTOMER) })
                    PDChip("🏭 تأمین‌کننده", state.kind == PartyKind.SUPPLIER, onClick = { viewModel.onKindChange(PartyKind.SUPPLIER) })
                    PDChip("همه", state.kind == PartyKind.BOTH, onClick = { viewModel.onKindChange(PartyKind.BOTH) })
                }
            }

            FormSection(title = "اطلاعات اصلی") {
                PDTextField(
                    label = "نام کامل *",
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "مثلاً آقای احمدی",
                    isError = state.nameError != null,
                    errorMessage = state.nameError
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "موبایل",
                            value = state.mobile,
                            onValueChange = viewModel::onMobileChange,
                            placeholder = "۰۹۱۲۳۴۵۶۷۸۹",
                            keyboardType = KeyboardType.Phone,
                            isError = state.mobileError != null,
                            errorMessage = state.mobileError
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "تلفن ثابت",
                            value = state.phone,
                            onValueChange = viewModel::onPhoneChange,
                            placeholder = "۰۲۱۸۸۷۷۶۶۵۵",
                            keyboardType = KeyboardType.Phone
                        )
                    }
                }
            }

            FormSection(title = "اطلاعات مالیاتی") {
                PDTextField(
                    label = "کد ملی / شناسه ملی",
                    value = state.nationalId,
                    onValueChange = viewModel::onNationalIdChange,
                    placeholder = "۰۰۱۲۳۴۵۶۷۸",
                    keyboardType = KeyboardType.Number
                )
                PDTextField(
                    label = "کد اقتصادی",
                    value = state.economicCode,
                    onValueChange = viewModel::onEconomicCodeChange,
                    placeholder = "اختیاری",
                    keyboardType = KeyboardType.Number
                )
            }

            FormSection(title = "اطلاعات مالی") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "سقف اعتبار (تومان)",
                            value = state.creditLimitToman,
                            onValueChange = viewModel::onCreditLimitChange,
                            placeholder = "۰",
                            keyboardType = KeyboardType.Number
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "درصد تخفیف",
                            value = state.discountPercent,
                            onValueChange = viewModel::onDiscountChange,
                            placeholder = "۰",
                            keyboardType = KeyboardType.Number
                        )
                    }
                }
                PDTextField(
                    label = "گروه (اختیاری)",
                    value = state.groupName,
                    onValueChange = viewModel::onGroupNameChange,
                    placeholder = "مثلاً عادی، عمده، ویژه"
                )
            }

            FormSection(title = "آدرس") {
                PDTextField(
                    label = "نشانی",
                    value = state.address,
                    onValueChange = viewModel::onAddressChange,
                    placeholder = "خیابان، کوچه، پلاک...",
                    singleLine = false,
                    minLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "استان",
                            value = state.province,
                            onValueChange = viewModel::onProvinceChange,
                            placeholder = "تهران"
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(
                            label = "شهر",
                            value = state.city,
                            onValueChange = viewModel::onCityChange,
                            placeholder = "تهران"
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
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}