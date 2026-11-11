package ir.hasabdari.paydar.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hasabdari.paydar.ui.theme.*

@Composable
fun PinDots(
    filled: Int,
    total: Int = 4,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (i < filled)
                            Brush.linearGradient(listOf(Teal600, Teal700))
                        else
                            Brush.linearGradient(listOf(Slate100, Slate100))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (i < filled) "●" else "○",
                    fontSize = 22.sp,
                    color = if (i < filled) Color.White else Slate400,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PinKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("۱", "۲", "۳"),
        listOf("۴", "۵", "۶"),
        listOf("۷", "۸", "۹"),
        listOf("", "۰", "⌫")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { label ->
                    if (label.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        PinKey(
                            label = label,
                            isBackspace = label == "⌫",
                            enabled = enabled,
                            onClick = {
                                if (label == "⌫") onBackspace()
                                else onDigit(label.toLatinDigit())
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PinKey(
    label: String,
    isBackspace: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isBackspace)
                    Brush.linearGradient(listOf(Slate100, Slate200))
                else
                    Brush.linearGradient(listOf(Color.White, Color(0xFFFAFAFA)))
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isBackspace) Slate600 else Slate900
        )
    }
}

private fun String.toLatinDigit(): String = when (this) {
    "۰" -> "0"
    "۱" -> "1"
    "۲" -> "2"
    "۳" -> "3"
    "۴" -> "4"
    "۵" -> "5"
    "۶" -> "6"
    "۷" -> "7"
    "۸" -> "8"
    "۹" -> "9"
    else -> this
}