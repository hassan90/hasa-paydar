package ir.hasabdari.paydar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hasabdari.paydar.ui.theme.*

@Composable
fun PDHeroCard(
    label: String,
    amount: String,
    currency: String = "تومان",
    trendText: String? = null,
    sparklineValues: List<Float> = emptyList(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Teal800, Teal600, Teal500),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
    ) {
        // ═══ بافت نقطه‌ای دو لایه (دقیقاً مطابق طرح) ═══
        Canvas(Modifier.matchParentSize()) {
            // لایه ۱: 30dp فاصله
            var step = 30.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    drawCircle(Color.White.copy(alpha = 0.10f), 1.dp.toPx(), Offset(x, y))
                    x += step
                }
                y += step
            }
            // لایه ۲: 40dp فاصله، افست 15dp
            step = 40.dp.toPx()
            y = 15.dp.toPx()
            while (y < size.height) {
                var x = 15.dp.toPx()
                while (x < size.width) {
                    drawCircle(Color.White.copy(alpha = 0.08f), 1.dp.toPx(), Offset(x, y))
                    x += step
                }
                y += step
            }
        }
        // ═══ لکه‌های رنگی (radial glows) ═══
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Teal400.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(size.width * 0.9f, 0f),
                    radius = size.minDimension * 0.6f
                ),
                radius = size.minDimension * 0.6f,
                center = Offset(size.width * 0.9f, 0f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Amber400.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(0f, size.height),
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(0f, size.height)
            )
        }

        Column(Modifier.padding(22.dp).fillMaxWidth()) {
            Text(label, color = Color.White.copy(alpha = 0.92f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(amount, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Spacer(Modifier.width(6.dp))
                Text(currency, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (trendText != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(trendText, color = Color(0xFFECFDF5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (sparklineValues.size >= 2) {
                Spacer(Modifier.height(16.dp))
                Sparkline(sparklineValues)
            }
        }
    }
}

@Composable
private fun Sparkline(values: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(44.dp)) {
        val maxV = values.maxOrNull() ?: 1f
        val minV = values.minOrNull() ?: 0f
        val range = (maxV - minV).coerceAtLeast(0.001f)
        val stepX = size.width / (values.size - 1)
        val linePath = Path()
        val fillPath = Path()
        var lastX = 0f
        var lastY = 0f
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height - ((v - minV) / range) * size.height * 0.85f - size.height * 0.05f
            lastX = x
            lastY = y
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(fillPath, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0f))))
        drawPath(linePath, color = Color.White, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        // ═══ نقطه سفید در انتهای خط (مطابق طرح) ═══
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(lastX, lastY))
    }
}

// ═══════════════════════════════════════════════════════════
// PDKpiCard
// ═══════════════════════════════════════════════════════════
enum class PDKpiColor { TEAL, AMBER, VIOLET, ROSE, SKY, GREEN }

@Composable
fun PDKpiCard(
    icon: String,
    label: String,
    value: String,
    color: PDKpiColor,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendPositive: Boolean = true
) {
    val (bg1, bg2) = when (color) {
        PDKpiColor.TEAL -> Teal100 to Teal200
        PDKpiColor.AMBER -> Amber100 to Amber200
        PDKpiColor.VIOLET -> Violet100 to Violet200
        PDKpiColor.ROSE -> Rose100 to Rose200
        PDKpiColor.SKY -> Sky100 to Sky200
        PDKpiColor.GREEN -> Green100 to Green200
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(bg1, bg2))),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 20.sp) }
            Spacer(Modifier.height(10.dp))
            Text(label, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Slate900, letterSpacing = (-0.3).sp)
            if (trend != null) {
                Spacer(Modifier.height(4.dp))
                Text(trend, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (trendPositive) Green600 else Rose600)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDWarnCard — با سایه رنگی روی آیکون
// ═══════════════════════════════════════════════════════════
@Composable
fun PDWarnCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: String = "⚠️",
    iconStart: Color = Amber400,
    iconEnd: Color = Amber500,
    titleColor: Color = Amber700,
    bodyColor: Color = Color(0xFF78350F),
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Amber50, Color(0xFFFEF9E7))))
            .border(1.dp, Amber200, RoundedCornerShape(14.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = iconStart, spotColor = iconStart)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(iconStart, iconEnd))),
            contentAlignment = Alignment.Center
        ) { Text(icon, fontSize = 18.sp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(3.dp))
            Text(body, color = bodyColor, fontSize = 11.sp, lineHeight = 18.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDDebtCard — با نوار گرادیانت
// ═══════════════════════════════════════════════════════════
enum class PDDebtType { CUSTOMER, SUPPLIER }

@Composable
fun PDDebtCard(
    icon: String,
    label: String,
    value: String,
    meta: String,
    type: PDDebtType,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (barStart, barEnd, iconBg1, iconBg2, valueColor) = when (type) {
        PDDebtType.CUSTOMER -> Tuple5(Rose400, Rose600, Rose100, Rose200, Rose600)
        PDDebtType.SUPPLIER -> Tuple5(Violet400, Violet600, Violet100, Violet200, Violet700)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // ═══ نوار گرادیانت سمت چپ بصری ═══
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(Brush.verticalGradient(listOf(barStart, barEnd)))
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(iconBg1, iconBg2))),
                    contentAlignment = Alignment.Center
                ) { Text(icon, fontSize = 22.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, color = Slate500, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = valueColor, letterSpacing = (-0.3).sp)
                    Spacer(Modifier.height(3.dp))
                    Text(meta, fontSize = 10.sp, color = Slate400)
                }
                Text("←", fontSize = 18.sp, color = Slate400)
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val v1: A, val v2: B, val v3: C, val v4: D, val v5: E)

// Destructuring helper
private operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component1() = v1
private operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component2() = v2
private operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component3() = v3
private operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component4() = v4
private operator fun <A, B, C, D, E> Tuple5<A, B, C, D, E>.component5() = v5

// ═══════════════════════════════════════════════════════════
// PDQuickAction — با سایه رنگی روی آیکون
// ═══════════════════════════════════════════════════════════
@Composable
fun PDQuickAction(
    icon: String,
    label: String,
    color: PDKpiColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (start, end) = when (color) {
        PDKpiColor.TEAL -> Teal500 to Teal700
        PDKpiColor.AMBER -> Amber400 to Amber600
        PDKpiColor.VIOLET -> Violet500 to Violet700
        PDKpiColor.ROSE -> Rose500 to Rose600
        PDKpiColor.SKY -> Sky500 to Sky600
        PDKpiColor.GREEN -> Green500 to Green600
    }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = start, spotColor = start)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(start, end))),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 22.sp) }
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900, textAlign = TextAlign.Center)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDBarChartCard — با سایه peak
// ═══════════════════════════════════════════════════════════
data class PDBarChartItem(
    val label: String,
    val valueText: String,
    val valueFraction: Float,
    val isPeak: Boolean = false
)

@Composable
fun PDBarChartCard(
    title: String,
    subtitle: String,
    badge: String,
    items: List<PDBarChartItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 11.sp, color = Slate500)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Teal50)
                        .border(1.dp, Teal100, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text(badge, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Teal700) }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                items.forEach { item ->
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(item.valueText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(item.valueFraction.coerceIn(0.05f, 1f) * 0.85f)
                                .let {
                                    if (item.isPeak)
                                        it.shadow(4.dp, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp), ambientColor = Amber500, spotColor = Amber500)
                                    else it
                                }
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                                .background(
                                    if (item.isPeak) Brush.linearGradient(listOf(Amber400, Amber500))
                                    else Brush.linearGradient(listOf(Teal400, Teal600))
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(item.label, fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDDonutCard
// ═══════════════════════════════════════════════════════════
data class PDDonutSegment(
    val label: String,
    val valueText: String,
    val valueFraction: Float,
    val color: Color
)

@Composable
fun PDDonutCard(
    segments: List<PDDonutSegment>,
    centerPercent: String,
    centerLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(color = Slate100, radius = radius, center = center, style = Stroke(width = strokeWidth))
                    var startAngle = -90f
                    segments.forEach { seg ->
                        val sweep = 360f * seg.valueFraction
                        drawArc(
                            color = seg.color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 6f).coerceAtLeast(2f),
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        startAngle += sweep
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(centerPercent, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Slate900)
                    Text(centerLabel, fontSize = 10.sp, color = Slate500)
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                segments.forEach { seg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(seg.color))
                        Spacer(Modifier.width(8.dp))
                        Text(seg.label, fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text(seg.valueText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDTxnRow
// ═══════════════════════════════════════════════════════════
enum class PDTxnType { SALE, RECEIPT, PURCHASE }

@Composable
fun PDTxnRow(
    avatarText: String,
    type: PDTxnType,
    title: String,
    meta: String,
    amount: String,
    amountPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val (bg1, bg2) = when (type) {
        PDTxnType.SALE -> Green500 to Green600
        PDTxnType.RECEIPT -> Sky400 to Sky600
        PDTxnType.PURCHASE -> Amber400 to Amber600
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Slate100, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(bg1, bg2))),
            contentAlignment = Alignment.Center
        ) { Text(avatarText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(meta, fontSize = 11.sp, color = Slate500)
        }
        Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (amountPositive) Green600 else Rose600)
    }
}

// ═══════════════════════════════════════════════════════════
// PDBadge
// ═══════════════════════════════════════════════════════════
enum class PDBadgeType { LOW, OUT, SUCCESS, WARNING, INFO, NEUTRAL, DANGER }

@Composable
fun PDBadge(text: String, type: PDBadgeType, modifier: Modifier = Modifier) {
    val (bg, fg) = when (type) {
        PDBadgeType.LOW -> Amber100 to Amber700
        PDBadgeType.OUT -> Rose100 to Rose600
        PDBadgeType.SUCCESS -> Green100 to Green600
        PDBadgeType.WARNING -> Amber100 to Amber700
        PDBadgeType.INFO -> Sky100 to Sky600
        PDBadgeType.NEUTRAL -> Slate100 to Slate600
        PDBadgeType.DANGER -> Rose100 to Rose600
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) { Text(text, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

@Composable
fun PDSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate900)
        if (actionText != null && onAction != null) {
            Text(
                actionText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Teal700,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDProductGridCard
// ═══════════════════════════════════════════════════════════
@Composable
fun PDProductGridCard(
    emoji: String,
    name: String,
    priceText: String,
    stockQuantityText: String,
    stockStatus: PDStockStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stockFraction: Float = 0.5f
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Teal50, Teal100))),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 40.sp)
                if (stockStatus != PDStockStatus.NORMAL) {
                    Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                        PDBadge(
                            text = if (stockStatus == PDStockStatus.OUT) "تمام" else "کم",
                            type = if (stockStatus == PDStockStatus.OUT) PDBadgeType.OUT else PDBadgeType.LOW
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(priceText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Teal700, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("موجودی", fontSize = 10.sp, color = Slate500)
                Text(
                    stockQuantityText, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    color = when (stockStatus) {
                        PDStockStatus.OUT -> Rose600
                        PDStockStatus.LOW -> Amber700
                        PDStockStatus.NORMAL -> Green600
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            PDProgressBar(fraction = stockFraction, status = stockStatus)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// PDProductListRow
// ═══════════════════════════════════════════════════════════
@Composable
fun PDProductListRow(
    emoji: String,
    name: String,
    code: String,
    priceText: String,
    stockQuantityText: String,
    stockStatus: PDStockStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(Teal50, Teal100))),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 24.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("کد: $code", fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(priceText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Teal700)
                Spacer(Modifier.height(4.dp))
                PDBadge(
                    text = stockQuantityText,
                    type = when (stockStatus) {
                        PDStockStatus.OUT -> PDBadgeType.OUT
                        PDStockStatus.LOW -> PDBadgeType.LOW
                        PDStockStatus.NORMAL -> PDBadgeType.SUCCESS
                    }
                )
            }
        }
    }
}