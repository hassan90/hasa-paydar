package ir.hasabdari.paydar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hasabdari.paydar.ui.theme.*

// ═══════════════════════════════════════════════════════════
// PDAppBar — شفاف، با دکمه‌های سفید، آماده statusBarsPadding
// ═══════════════════════════════════════════════════════════
@Composable
fun PDAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    showNotification: Boolean = false,
    notificationDot: Boolean = false,
    onNotification: (() -> Unit)? = null,
    showProfile: Boolean = false,
    onProfile: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            onBack != null -> AppBarActionButton(text = "→", onClick = onBack)
            onMenu != null -> AppBarActionButton(text = "☰", onClick = onMenu)
        }
        Text(title, modifier = Modifier.weight(1f), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Slate900)
        if (trailing != null) trailing()
        if (showNotification && onNotification != null) {
            Box {
                AppBarActionButton(text = "🔔", onClick = onNotification)
                if (notificationDot) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .padding(end = 7.dp, top = 7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(Rose500)
                    )
                }
            }
        }
        if (showProfile && onProfile != null) {
            AppBarActionButton(text = "👤", onClick = onProfile)
        }
    }
}

@Composable
private fun AppBarActionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 20.sp, color = Slate900) }
}

// ═══════════════════════════════════════════════════════════
// PDBottomNav — دقیقاً مطابق طرح: FAB با حاشیه سفید و برجستگی ۳۴dp
// ═══════════════════════════════════════════════════════════
@Composable
fun PDBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ارتفاع کل: ۷۲ nav + ۳۴ برجستگی FAB = ۱۰۶
    Box(
        modifier = modifier.fillMaxWidth().height(106.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ═══ Nav Bar (پس‌زمینه سفید) ═══
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color.White)
                .padding(horizontal = 8.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavItem("🏠", "خانه", currentRoute == "home") { onNavigate("home") }
            NavItem("📦", "کالاها", currentRoute == "products") { onNavigate("products") }
            Spacer(Modifier.width(68.dp))  // فضای FAB
            NavItem("🏭", "انبارها", currentRoute == "warehouses") { onNavigate("warehouses") }
            NavItem("⋯", "بیشتر", currentRoute == "more") { onNavigate("more") }
        }

        // ═══ FAB + Label ═══
        Column(
            modifier = Modifier.align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .shadow(12.dp, CircleShape, ambientColor = Amber500, spotColor = Amber500)
                    .border(4.dp, Color.White, CircleShape)      // ═══ حاشیه سفید ۴dp ═══
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Amber400, Amber600)))
                    .clickable(onClick = onFabClick),
                contentAlignment = Alignment.Center
            ) { Text("+", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            Spacer(Modifier.height(4.dp))
            Text("فروش", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Amber700)
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            label, fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
            color = if (active) Teal700 else Slate400
        )
    }
}

// ═══════════════════════════════════════════════════════════
// PDProgressBar + PDStockStatus
// ═══════════════════════════════════════════════════════════
enum class PDStockStatus { NORMAL, LOW, OUT }

@Composable
fun PDProgressBar(fraction: Float, status: PDStockStatus, modifier: Modifier = Modifier) {
    val (start, end) = when (status) {
        PDStockStatus.NORMAL -> Green500 to Green600
        PDStockStatus.LOW -> Amber400 to Amber500
        PDStockStatus.OUT -> Rose400 to Rose500
    }
    Box(
        modifier = modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Slate100)
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(listOf(start, end)))
        )
    }
}

// ═══════════════════════════════════════════════════════════
// PDEmptyState
// ═══════════════════════════════════════════════════════════
@Composable
fun PDEmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Teal50, Teal100))),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 60.sp) }
            Spacer(Modifier.height(20.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = Slate500, textAlign = TextAlign.Center, lineHeight = 20.sp)
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                ) { Text(actionText, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDChip — با سایه سبز روی حالت انتخاب
// ═══════════════════════════════════════════════════════════
@Composable
fun PDChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Box(
            modifier = modifier
                .shadow(4.dp, RoundedCornerShape(20.dp), ambientColor = Teal700, spotColor = Teal700)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(Teal700, Teal600)))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text(text, color = Slate500, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}