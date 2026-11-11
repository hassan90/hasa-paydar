package ir.hasabdari.paydar.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.ui.common.scaledToQuantityString
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    onBack: () -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: PosViewModel = hiltViewModel()
) {
    val state = viewModel.state
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }

    if (state.lastSale != null) {
        PosSuccessScreen(
            sale = state.lastSale!!,
            onNewSale = {
                viewModel.clearLastSale()
                showCartSheet = false
            },
            onBackHome = {
                viewModel.clearLastSale()
                onBack()
            }
        )
        return
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(
                    title = "فروش سریع",
                    onBack = onBack,
                    trailing = {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Teal100)
                                .clickable { showCustomerPicker = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👤", fontSize = 14.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                state.partyName.ifBlank { "متفرقه" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Teal700
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (state.cart.isNotEmpty()) {
                CartBottomBar(
                    count = state.cart.size,
                    total = state.grandTotalRials,
                    onContinue = { showCartSheet = true }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("🔍 جستجو یا اسکن بارکد...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Text("🔍") },
                trailingIcon = { Text("📷", modifier = Modifier.padding(end = 12.dp)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Teal700,
                    unfocusedBorderColor = Slate200
                )
            )

            // نمایش خطا بیرون از Sheet
            state.error?.let { err ->
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(10.dp))
                        .background(Rose50).padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(err, color = Rose600, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("✕", color = Rose600, fontSize = 14.sp,
                            modifier = Modifier.clickable { viewModel.clearError() })
                    }
                }
            }

            val filtered = if (state.query.isBlank()) products
            else products.filter {
                it.product.name.contains(state.query, ignoreCase = true) ||
                        it.product.code.contains(state.query, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("کالایی یافت نشد", color = Slate500, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.product.id }) { p ->
                        PosProductCard(product = p, onClick = { viewModel.addProduct(p) })
                    }
                }
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            containerColor = Color.White
        ) {
            CartAndPaymentContent(
                state = state,
                onIncrement = viewModel::incrementLine,
                onDecrement = viewModel::decrementLine,
                onDiscountChange = viewModel::onDiscountChange,
                onSettlementChange = viewModel::onSettlementTypeChange,
                onReceivedChange = viewModel::onReceivedChange,
                onCustomerClick = { showCustomerPicker = true },
                onSubmit = viewModel::submitSale,
                onClearError = viewModel::clearError
            )
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            onSelect = { viewModel.selectCustomer(it); showCustomerPicker = false },
            onDismiss = { showCustomerPicker = false }
        )
    }
}

@Composable
private fun CartBottomBar(count: Int, total: Long, onContinue: () -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("🛒 سبد ($count کالا)", fontSize = 11.sp, color = Slate500)
                Text("${total.toTomanString()} ت", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Teal700)
            }
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700)
            ) { Text("ادامه ←", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PosProductCard(product: ir.hasabdari.paydar.domain.model.ProductWithStock, onClick: () -> Unit) {
    val disabled = product.totalStockScaled <= 0
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !disabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (disabled) Slate100 else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(8.dp).fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Teal50, Teal100))),
                contentAlignment = Alignment.Center
            ) { Text(if (disabled) "❌" else "📦", fontSize = 26.sp) }
            Spacer(Modifier.height(6.dp))
            Text(product.product.name, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = if (disabled) Slate400 else Slate900, maxLines = 1)
            Text(
                if (disabled) "ناموجود" else "${product.product.salePrice.rials.toTomanString()} ت",
                fontSize = 10.sp,
                color = if (disabled) Rose600 else Teal700,
                fontWeight = FontWeight.Bold
            )
            if (!disabled) {
                Text("موجودی: ${product.totalStockScaled.scaledToQuantityString()}",
                    fontSize = 9.sp, color = Slate500)
            }
        }
    }
}

@Composable
private fun CartAndPaymentContent(
    state: PosState,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
    onSettlementChange: (SettlementType) -> Unit,
    onReceivedChange: (String) -> Unit,
    onCustomerClick: () -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("🛒 سبد خرید (${state.cart.size} کالا)",
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }

        items(state.cart.size) { i ->
            val line = state.cart[i]
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Slate50).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("${line.unitPriceRials.toTomanString()} × ${line.quantity}",
                        fontSize = 10.sp, color = Slate500)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(Rose100)
                            .clickable { onDecrement(line.productId) },
                        contentAlignment = Alignment.Center
                    ) { Text("−", color = Rose600, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    Text("${line.quantity}", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = Slate900, modifier = Modifier.padding(horizontal = 10.dp))
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(Teal600)
                            .clickable { onIncrement(line.productId) },
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("جمع اقلام", fontSize = 11.sp, color = Slate600)
                Text("${state.subtotalRials.toTomanString()} ت", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("🎁 تخفیف (تومان)", fontSize = 11.sp, color = Slate600, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    value = state.discountText,
                    onValueChange = onDiscountChange,
                    placeholder = { Text("۰", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("جمع کل", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Teal700)
                Text("${state.grandTotalRials.toTomanString()} ت", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Teal700)
            }
        }

        item {
            Text("روش پرداخت", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = Slate900, modifier = Modifier.padding(top = 8.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PDChip("💵 نقد", state.settlementType == SettlementType.CASH, onClick = { onSettlementChange(SettlementType.CASH) })
                PDChip("📋 نسیه", state.settlementType == SettlementType.CREDIT, onClick = { onSettlementChange(SettlementType.CREDIT) })
                PDChip("➕ ترکیبی", state.settlementType == SettlementType.MIXED, onClick = { onSettlementChange(SettlementType.MIXED) })
            }
        }

        if (state.settlementType != SettlementType.CASH) {
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Slate50)
                        .clickable(onClick = onCustomerClick)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👤", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.partyName.ifBlank { "انتخاب مشتری..." },
                            fontSize = 12.sp,
                            color = if (state.partyName.isBlank()) Slate400 else Slate900,
                            fontWeight = if (state.partyName.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text("▼", fontSize = 10.sp, color = Slate400)
                    }
                }
            }
        }

        if (state.settlementType == SettlementType.CASH || state.settlementType == SettlementType.MIXED) {
            item {
                OutlinedTextField(
                    value = state.receivedText,
                    onValueChange = onReceivedChange,
                    label = { Text("مبلغ دریافتی (تومان)", fontSize = 11.sp) },
                    placeholder = { Text("۰", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Teal700
                    )
                )
            }
            if (state.changeRials > 0) {
                item {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Amber50).padding(12.dp)) {
                        Row {
                            Text("💵 باقی‌مانده به مشتری: ", fontSize = 11.sp, color = Amber700, fontWeight = FontWeight.SemiBold)
                            Text("${state.changeRials.toTomanString()} ت", fontSize = 12.sp, color = Amber700, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ⚠️ نمایش خطا داخل Sheet — بسیار مهم
        if (state.error != null) {
            item {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(Rose50).padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(state.error, color = Rose600, fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("✕", color = Rose600, fontSize = 14.sp,
                            modifier = Modifier.clickable { onClearError() })
                    }
                }
            }
        }

        item {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp),
                enabled = !state.submitting && state.cart.isNotEmpty(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700)
            ) {
                if (state.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("✅ ثبت فاکتور", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PosSuccessScreen(
    sale: ir.hasabdari.paydar.domain.model.SaleResult,
    onNewSale: () -> Unit,
    onBackHome: () -> Unit
) {
    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "فروش موفق", onBack = onBackHome)
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Green500, Green600))),
                contentAlignment = Alignment.Center
            ) { Text("✓", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold) }

            Text("فاکتور با موفقیت ثبت شد", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text("شماره ${sale.invoiceNumber}", fontSize = 12.sp, color = Slate500)

            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("جمع کل", fontSize = 12.sp, color = Slate600)
                        Text("${sale.grandTotalRials.toTomanString()} ت", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("بهای تمام‌شده", fontSize = 12.sp, color = Slate600)
                        Text("${sale.cogsTotalRials.toTomanString()} ت", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Amber700)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("سود ناخالص", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Text("${sale.profitRials.toTomanString()} ت", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Green600)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNewSale,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700)
            ) { Text("🛒 فروش جدید", fontWeight = FontWeight.Bold, fontSize = 15.sp) }

            TextButton(onClick = onBackHome) {
                Text("بازگشت به داشبورد", color = Teal700, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<Party>,
    onSelect: (Party?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب مشتری", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    Text(
                        "مشتری متفرقه",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Teal700,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(null) }
                            .padding(vertical = 12.dp)
                    )
                    HorizontalDivider(color = Slate100)
                }
                items(customers.size) { i ->
                    val c = customers[i]
                    Text(
                        "${c.name}${if (!c.mobile.isNullOrBlank()) " · ${c.mobile}" else ""}",
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { onSelect(c) }
                            .padding(vertical = 10.dp)
                    )
                    HorizontalDivider(color = Slate100)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}