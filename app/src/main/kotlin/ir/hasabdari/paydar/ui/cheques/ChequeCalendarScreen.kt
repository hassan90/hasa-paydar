package ir.hasabdari.paydar.ui.cheques

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.Cheque
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChequeCalendarScreen(
    onBack: () -> Unit,
    viewModel: ChequeCalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "تقویم سررسید", onBack = onBack)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Header ماه
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.onPrevMonth() }) {
                    Text("‹", fontSize = 24.sp, color = Teal700)
                }
                Text(
                    "${monthNames[state.month - 1]} ${JalaliDate.toPersianDigits(state.year.toString())}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Slate900
                )
                IconButton(onClick = { viewModel.onNextMonth() }) {
                    Text("›", fontSize = 24.sp, color = Teal700)
                }
            }

            // روزهای هفته
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { d ->
                    Text(
                        d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // روزهای ماه
            val daysInMonth = daysInJalaliMonth(state.year, state.month)
            val cellHeight = 56.dp

            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                var dayCounter = 1
                while (dayCounter <= daysInMonth) {
                    Row(Modifier.fillMaxWidth()) {
                        repeat(7) { _ ->
                            if (dayCounter <= daysInMonth) {
                                val d = dayCounter
                                val cheques = viewModel.chequesOnDay(d)
                                DayCell(
                                    day = d,
                                    chequesCount = cheques.size,
                                    isSelected = state.selectedDay == d,
                                    height = cellHeight
                                ) { viewModel.onDayClick(d) }
                                dayCounter++
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // لیست چک‌های روز انتخاب‌شده
            state.selectedDay?.let { selectedDay ->
                val cheques = viewModel.chequesOnDay(selectedDay)
                Spacer(Modifier.height(12.dp))
                Text(
                    "چک‌های روز ${JalaliDate.toPersianDigits(selectedDay.toString())}",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (cheques.isEmpty()) {
                    Text(
                        "—", fontSize = 12.sp, color = Slate500,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(cheques, key = { it.id }) { c ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .background(Color.White).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(if (c.kind == ChequeKind.RECEIVED) Green100 else Rose100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (c.kind == ChequeKind.RECEIVED) "📥" else "📤",
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "چک ${c.number}",
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900
                                    )
                                    Text(
                                        when (c.kind) {
                                            ChequeKind.RECEIVED -> "دریافتی"
                                            ChequeKind.ISSUED -> "پرداختی"
                                        },
                                        fontSize = 10.sp, color = Slate500
                                    )
                                }
                                Text(
                                    c.amount.rials.toTomanString() + " ت",
                                    fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (c.kind == ChequeKind.RECEIVED) Green600 else Rose600
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══ ⚠️ تغییر اصلی: RowScope receiver ═══
@Composable
private fun RowScope.DayCell(
    day: Int,
    chequesCount: Int,
    isSelected: Boolean,
    height: Dp,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .weight(1f)                 // ← الان مجاز است (RowScope)
            .height(height)
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Teal700 else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                JalaliDate.toPersianDigits(day.toString()),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Slate900
            )
            if (chequesCount > 0) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(chequesCount.coerceAtMost(3)) {
                        Box(
                            Modifier.size(5.dp).clip(CircleShape)
                                .background(if (isSelected) Color.White else Amber500)
                        )
                    }
                }
            }
        }
    }
}

private fun daysInJalaliMonth(year: Int, month: Int): Int = when {
    month in 1..6 -> 31
    month in 7..11 -> 30
    month == 12 -> if ((year % 33) in setOf(1, 5, 9, 13, 17, 22, 26, 30)) 30 else 29
    else -> 30
}