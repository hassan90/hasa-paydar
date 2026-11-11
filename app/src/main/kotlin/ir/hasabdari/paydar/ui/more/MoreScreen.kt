package ir.hasabdari.paydar.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDBottomNav
import ir.hasabdari.paydar.ui.theme.*

@Composable
fun MoreScreen(
    onNavigateToCustomers: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToTreasury: () -> Unit = {},
    onNavigateToJournal: () -> Unit = {},
    onNavigateToTrialBalance: () -> Unit = {},
    onNavigateToPurchases: () -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToPos: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToCheques: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},          // ← جدید
    onNavigateBottom: (String) -> Unit = {}
) {
    val snackbarHost = remember { SnackbarHostState() }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(msg) {
        val m = msg
        if (m != null) { snackbarHost.showSnackbar(m); msg = null }
    }
    val comingSoon: (String) -> Unit = { msg = it }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "بیشتر", onMenu = { })
            }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(currentRoute = "more", onNavigate = onNavigateBottom)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionLabel("اشخاص") }
            item {
                MenuRow(icon = "👥", title = "مشتریان", subtitle = "مدیریت مشتریان و بدهی‌ها",
                    colorStart = Rose500, colorEnd = Rose600, onClick = onNavigateToCustomers)
            }
            item {
                MenuRow(icon = "🏭", title = "تأمین‌کنندگان", subtitle = "مدیریت تأمین‌کنندگان و خریدها",
                    colorStart = Violet500, colorEnd = Violet700, onClick = onNavigateToSuppliers)
            }

            item { Spacer(Modifier.height(6.dp)) }
            item { SectionLabel("کالا") }
            item {
                MenuRow(icon = "📂", title = "گروه‌بندی کالا", subtitle = "دسته‌بندی درختی کالاها",
                    colorStart = Teal500, colorEnd = Teal700, onClick = onNavigateToGroups)
            }

            item { Spacer(Modifier.height(6.dp)) }
            item { SectionLabel("عملیات مالی") }
            item {
                MenuRow(icon = "💰", title = "خزانه", subtitle = "صندوق، بانک، دریافت و پرداخت",
                    colorStart = Sky500, colorEnd = Sky600, onClick = onNavigateToTreasury)
            }
            item {
                MenuRow(icon = "🛒", title = "خریدها", subtitle = "فاکتورهای خرید و تأمین‌کنندگان",
                    colorStart = Amber500, colorEnd = Amber600, onClick = onNavigateToPurchases)
            }

            item { Spacer(Modifier.height(6.dp)) }
            item { SectionLabel("حسابداری") }
            item {
                MenuRow(icon = "📋", title = "دفتر روزنامه", subtitle = "همه اسناد حسابداری",
                    colorStart = Teal500, colorEnd = Teal700, onClick = onNavigateToJournal)
            }
            item {
                MenuRow(icon = "⚖️", title = "تراز آزمایشی", subtitle = "بررسی توازن دوبل",
                    colorStart = Violet500, colorEnd = Violet700, onClick = onNavigateToTrialBalance)
            }

            item { Spacer(Modifier.height(6.dp)) }
            item { SectionLabel("به‌زودی") }
            item {
                MenuRow(icon = "🛒", title = "POS فروش سریع", subtitle = "صندوق فروش سریع",
                    colorStart = Green600, colorEnd = Green500, onClick = onNavigateToPos)
            }
            item {
                MenuRow(icon = "🧾", title = "فروش‌ها", subtitle = "لیست فاکتورهای فروش",
                    colorStart = Green500, colorEnd = Green600, onClick = onNavigateToSales)
            }
            item {
                MenuRow(
                    icon = "📋", title = "چک‌ها", subtitle = "دریافتی، پرداختی، وصول، برگشت",
                    colorStart = Amber500, colorEnd = Amber600,
                    onClick = onNavigateToCheques
                )
            }
            item {
                MenuRow(icon = "📊", title = "گزارش‌ها", subtitle = "سود و زیان، ترازنامه، فروش، اشخاص، انبار",
                    colorStart = Violet500, colorEnd = Violet700, onClick = onNavigateToReports)
            }
            item {
                MenuRow(
                    icon = "⚙️", title = "تنظیمات", subtitle = "فروشگاه، مالیات، ظاهر، پشتیبان",
                    colorStart = Slate500, colorEnd = Slate700,
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate500,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
}

@Composable
private fun MenuRow(
    icon: String,
    title: String,
    subtitle: String,
    colorStart: Color,
    colorEnd: Color,
    disabled: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(enabled = !disabled, onClick = onClick)
            .alpha(if (disabled) 0.55f else 1f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(colorStart, colorEnd))),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 22.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (disabled) Slate400 else Slate900)
            Text(subtitle, fontSize = 10.sp,
                color = if (disabled) Slate300 else Slate500,
                modifier = Modifier.padding(top = 2.dp))
        }
        Text("←", fontSize = 16.sp, color = if (disabled) Slate300 else Slate400)
    }
}