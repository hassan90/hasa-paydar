package ir.hasabdari.paydar.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import ir.hasabdari.paydar.ui.common.scaledToQuantityString
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.actionMessage) {
        val msg = state.actionMessage
        if (msg != null) {
            snackbarHost.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("جزئیات کالا", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("→", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal700,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (state.warehouses.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = Amber500,
                    contentColor = Color.White
                ) { Text("±", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
        }
    ) { padding ->
        val product = state.product
        if (state.isLoading || product == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Teal800, Teal600))),
            ) {
                Row(Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) { Text("📦", fontSize = 36.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text("کد: ${product.code}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        if (!product.barcode.isNullOrBlank()) {
                            Text("بارکد: ${product.barcode}", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Price KPIs
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PriceKpi("خرید", product.purchasePrice.rials.toTomanString(), Teal100, Teal700, Modifier.weight(1f))
                PriceKpi("فروش", product.salePrice.rials.toTomanString(), Green100, Green600, Modifier.weight(1f))
                PriceKpi("سود", (product.salePrice.rials - product.purchasePrice.rials).toTomanString(), Amber100, Amber700, Modifier.weight(1f))
            }

            // Stock per warehouse
            Text("موجودی در انبارها", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
            if (state.stockByWarehouse.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("موجودی ثبت نشده", Modifier.padding(16.dp), fontSize = 12.sp, color = Slate500)
                }
            } else {
                state.stockByWarehouse.forEach { ws ->
                    Card(
                        Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(Teal100, Teal200))),
                                contentAlignment = Alignment.Center
                            ) { Text("🏭", fontSize = 20.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(ws.warehouseName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                Text(
                                    "میانگین: ${ws.avgCost.rials.toTomanString()} ت",
                                    fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Text(
                                ws.quantityScaled.scaledToQuantityString(),
                                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = if (ws.quantityScaled <= 0) Rose600 else Teal700
                            )
                        }
                    }
                }
            }

            if (product.notes?.isNotBlank() == true) {
                Text("توضیحات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text(product.notes!!, Modifier.padding(14.dp), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(80.dp))
        }

        if (showDialog) {
            StockMovementDialog(
                warehouses = state.warehouses,
                onDismiss = { showDialog = false },
                onConfirm = { wh, isIn, qty, cost ->
                    viewModel.addStock(wh, isIn, qty, cost)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
private fun PriceKpi(label: String, value: String, bg: Color, fg: Color, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(bg).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = fg.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
        }
    }
}

@Composable
private fun StockMovementDialog(
    warehouses: List<ir.hasabdari.paydar.domain.model.Warehouse>,
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Long, Long) -> Unit
) {
    var whId by remember { mutableStateOf(warehouses.firstOrNull()?.id ?: "") }
    var isIn by remember { mutableStateOf(true) }
    var qty by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حرکت موجودی", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("انبار", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                warehouses.forEach { w ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = w.id == whId, onClick = { whId = w.id })
                        Text(w.name, fontSize = 13.sp)
                    }
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isIn, onClick = { isIn = true }, label = { Text("ورود ➕") })
                    FilterChip(selected = !isIn, onClick = { isIn = false }, label = { Text("خروج ➖") })
                }
                OutlinedTextField(
                    value = qty, onValueChange = { qty = it.filter { c -> c.isDigit() } },
                    label = { Text("تعداد") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isIn) {
                    OutlinedTextField(
                        value = cost, onValueChange = { cost = it.filter { c -> c.isDigit() } },
                        label = { Text("بهای واحد (تومان)") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(whId, isIn, qty.toLongOrNull() ?: 0, cost.toLongOrNull() ?: 0) },
                enabled = (qty.toLongOrNull() ?: 0) > 0 && whId.isNotBlank()
            ) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}