package ir.hasabdari.paydar.ui.returns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.ui.common.scaledToQuantityString
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReturnScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddReturnViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // پیام موفقیت
    state.success?.let { res ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("ثبت برگشت موفق", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "شماره سند برگشت: ${res.returnInvoiceNumber}\n" +
                            "مبلغ برگشت: ${res.totalRefundRials.toTomanString()} ت",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(onClick = onSaved) { Text("بازگشت") }
            }
        )
    }

    val title = if (state.isSaleReturn) "برگشت از فروش" else "برگشت از خرید"

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
                        onClick = { viewModel.submit { } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !state.submitting && state.returnLines.any { it.qtyScaled > 0 },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isSaleReturn) Green600 else Amber600
                        )
                    ) {
                        if (state.submitting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("ثبت برگشت", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    ) { padding ->
        val data = state.originalInvoice
        if (state.isLoading || data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal700)
            }
            return@Scaffold
        }

        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // هدر فاکتور اصلی
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            if (state.isSaleReturn)
                                listOf(Green700, Green600)
                            else listOf(Amber700, Amber600)
                        )
                    ).padding(18.dp)
            ) {
                Column {
                    Text("فاکتور اصلی ${data.invoice.number}",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        JalaliDate.fromEpochMillis(data.invoice.issueDate).formatPersian() +
                                " · جمع: ${data.invoice.grandTotal.rials.toTomanString()} ت",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp
                    )
                }
            }

            // اقلام
            Text("اقلام برگشتی", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
            state.returnLines.forEachIndexed { idx, line ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("کالا ${line.originalLine.productId.take(6)}…",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                        Text(
                            "تعداد اصلی: ${line.maxReturnableScaled.scaledToQuantityString()} · " +
                                    "${line.originalLine.unitPrice.rials.toTomanString()} ت",
                            fontSize = 10.sp, color = Slate500,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    OutlinedTextField(
                        value = line.quantityText,
                        onValueChange = { viewModel.onQuantityChange(idx, it) },
                        label = { Text("برگشت", fontSize = 10.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.width(90.dp).height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            }

            // دلیل
            OutlinedTextField(
                value = state.reason,
                onValueChange = viewModel::onReasonChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("دلیل برگشت (اختیاری)") },
                placeholder = { Text("مثلاً: کالای معیوب", fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // خلاصه
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (state.isSaleReturn) Green50 else Amber50).padding(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مبلغ برگشت", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text(
                        state.totalRefundRials.toTomanString() + " ت",
                        fontSize = 16.sp, fontWeight = FontWeight.Black,
                        color = if (state.isSaleReturn) Green600 else Amber700
                    )
                }
            }

            state.error?.let {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(Rose50).padding(12.dp)
                ) {
                    Row {
                        Text(it, color = Rose600, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("✕", color = Rose600, fontSize = 14.sp,
                            modifier = Modifier.clickable { viewModel.clearError() })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}