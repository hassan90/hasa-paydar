package ir.hasabdari.paydar.ui.purchases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.Warehouse
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDChip
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddPurchaseViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val suppliers by viewModel.suppliers.collectAsState()
    val warehouses by viewModel.warehouses.collectAsState()
    val products by viewModel.products.collectAsState()

    var showSupplierPicker by remember { mutableStateOf(false) }
    var showWarehousePicker by remember { mutableStateOf(false) }
    var productPickerIdx by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "فاکتور خرید جدید", onBack = onBack)
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
                        else Text("ثبت فاکتور خرید", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══ نوع تسویه ═══
            FormSection(title = "نوع تسویه") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("💵 نقدی", state.settlementType == SettlementType.CASH, onClick = { viewModel.onSettlementTypeChange(SettlementType.CASH) })
                    PDChip("📋 نسیه", state.settlementType == SettlementType.CREDIT, onClick = { viewModel.onSettlementTypeChange(SettlementType.CREDIT) })
                    PDChip("➕ ترکیبی", state.settlementType == SettlementType.MIXED, onClick = { viewModel.onSettlementTypeChange(SettlementType.MIXED) })
                }
            }

            // ═══ تأمین‌کننده (اگر نسیه یا ترکیبی) ═══
            if (state.settlementType != SettlementType.CASH) {
                FormSection(title = "تأمین‌کننده") {
                    PickerRow(
                        label = if (state.partyName.isBlank()) "انتخاب تأمین‌کننده..." else state.partyName,
                        isEmpty = state.partyName.isBlank()
                    ) { showSupplierPicker = true }
                }
            }

            // ═══ انبار ═══
            FormSection(title = "انبار ورودی") {
                PickerRow(
                    label = if (state.warehouseName.isBlank()) "انتخاب انبار..." else state.warehouseName,
                    isEmpty = state.warehouseName.isBlank()
                ) { showWarehousePicker = true }
            }

            // ═══ اقلام ═══
            Text("اقلام فاکتور", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
            state.lines.forEachIndexed { idx, line ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("ردیف ${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500, modifier = Modifier.weight(1f))
                        if (state.lines.size > 1) {
                            Text("حذف", fontSize = 11.sp, color = Rose600,
                                modifier = Modifier.clickable { viewModel.removeLine(idx) })
                        }
                    }
                    PickerRow(
                        label = if (line.productName.isBlank()) "انتخاب کالا..." else line.productName,
                        isEmpty = line.productName.isBlank()
                    ) { productPickerIdx = idx }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.weight(1f)) {
                            PDTextField(
                                label = "تعداد",
                                value = line.quantityText,
                                onValueChange = { v -> viewModel.updateLine(idx) { it.copy(quantityText = v.filter { c -> c.isDigit() }) } },
                                placeholder = "۰",
                                keyboardType = KeyboardType.Number
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            PDTextField(
                                label = "قیمت واحد (ت)",
                                value = line.priceText,
                                onValueChange = { v -> viewModel.updateLine(idx) { it.copy(priceText = v.filter { c -> c.isDigit() }) } },
                                placeholder = "۰",
                                keyboardType = KeyboardType.Number
                            )
                        }
                    }
                }
            }
            TextButton(onClick = viewModel::addLine) {
                Text("➕ افزودن ردیف", color = Teal700, fontWeight = FontWeight.Bold)
            }

            // ═══ هزینه‌های اضافه ═══
            FormSection(title = "تخفیف و هزینه‌ها (تومان)") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.weight(1f)) {
                        PDTextField(label = "تخفیف", value = state.discountTotalText,
                            onValueChange = viewModel::onDiscountTotalChange,
                            placeholder = "۰", keyboardType = KeyboardType.Number)
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(label = "حمل", value = state.shippingText,
                            onValueChange = viewModel::onShippingChange,
                            placeholder = "۰", keyboardType = KeyboardType.Number)
                    }
                    Box(Modifier.weight(1f)) {
                        PDTextField(label = "سایر", value = state.otherCostsText,
                            onValueChange = viewModel::onOtherCostsChange,
                            placeholder = "۰", keyboardType = KeyboardType.Number)
                    }
                }
            }

            // ═══ پرداخت (اگر ترکیبی) ═══
            if (state.settlementType == SettlementType.MIXED) {
                FormSection(title = "مبلغ پرداختی (تومان)") {
                    PDTextField(label = "پرداخت نقدی", value = state.paidAmountText,
                        onValueChange = viewModel::onPaidAmountChange,
                        placeholder = "۰", keyboardType = KeyboardType.Number)
                }
            }

            // ═══ توضیحات ═══
            FormSection(title = "توضیحات") {
                PDTextField(label = "توضیحات (اختیاری)", value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    placeholder = "اختیاری", singleLine = false, minLines = 2)
            }

            // ═══ خلاصه ═══
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Teal50).padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جمع اقلام", fontSize = 11.sp, color = Slate600)
                        Text(state.subtotalRials.toTomanString() + " ت", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
                    }
                    if (state.invoiceDiscountRials > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("تخفیف", fontSize = 11.sp, color = Slate600)
                            Text("− ${state.invoiceDiscountRials.toTomanString()} ت", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Rose600)
                        }
                    }
                    if (state.shippingRials + state.otherRials > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("هزینه‌های اضافه", fontSize = 11.sp, color = Slate600)
                            Text("+ ${(state.shippingRials + state.otherRials).toTomanString()} ت", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber700)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Teal200)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جمع کل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal700)
                        Text(state.grandTotalRials.toTomanString() + " ت", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Teal700)
                    }
                }
            }

            state.error?.let { Text(it, color = Rose600, fontSize = 12.sp) }
            Spacer(Modifier.height(20.dp))
        }
    }

    // ═══ Picker دیالوگ‌ها ═══
    if (showSupplierPicker) {
        SimplePickerDialog(
            title = "انتخاب تأمین‌کننده",
            items = suppliers.map { it.id to "${it.name}${if (!it.mobile.isNullOrBlank()) " · ${it.mobile}" else ""}" },
            onSelect = { id ->
                suppliers.find { it.id == id }?.let { viewModel.onPartySelected(it) }
                showSupplierPicker = false
            },
            onDismiss = { showSupplierPicker = false }
        )
    }

    if (showWarehousePicker) {
        SimplePickerDialog(
            title = "انتخاب انبار",
            items = warehouses.map { it.id to it.name },
            onSelect = { id ->
                warehouses.find { it.id == id }?.let { viewModel.onWarehouseSelected(it) }
                showWarehousePicker = false
            },
            onDismiss = { showWarehousePicker = false }
        )
    }

    productPickerIdx?.let { idx ->
        SimplePickerDialog(
            title = "انتخاب کالا",
            items = products.map { it.product.id to "${it.product.name} · ${it.product.purchasePrice.rials.toTomanString()} ت" },
            onSelect = { id ->
                products.find { it.product.id == id }?.let { p ->
                    viewModel.updateLine(idx) {
                        it.copy(
                            productId = p.product.id,
                            productName = p.product.name,
                            unitId = p.product.unitId,
                            priceText = (p.product.purchasePrice.rials / 10).toString()
                        )
                    }
                }
                productPickerIdx = null
            },
            onDismiss = { productPickerIdx = null }
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
private fun PickerRow(label: String, isEmpty: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, if (isEmpty) Slate200 else Teal500, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = if (isEmpty) Slate400 else Slate900,
            fontWeight = if (isEmpty) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text("▼", fontSize = 10.sp, color = Slate400)
    }
}

@Composable
private fun SimplePickerDialog(
    title: String,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) items else items.filter { it.second.contains(query, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("جستجو...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (filtered.isEmpty()) {
                    Text("موردی یافت نشد", color = Slate500, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filtered.size) { i ->
                            val (id, label) = filtered[i]
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = Slate900,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(id) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                            )
                            HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}