package ir.hasabdari.paydar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

// فونت Vazirmatn باید در res/font باشند (فایل‌های TTF)
// موقتاً از default استفاده می‌شود تا font اضافه شود
val VazirFont = FontFamily.Default

private fun baseStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int
) = TextStyle(
    fontFamily = VazirFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    textDirection = TextDirection.Rtl
)

val PaydarTypography = Typography(
    displayLarge = baseStyle(FontWeight.Bold, 34, 44),
    displayMedium = baseStyle(FontWeight.Bold, 28, 38),
    displaySmall = baseStyle(FontWeight.Bold, 24, 34),
    headlineLarge = baseStyle(FontWeight.Bold, 24, 34),
    headlineMedium = baseStyle(FontWeight.SemiBold, 20, 30),
    headlineSmall = baseStyle(FontWeight.SemiBold, 18, 28),
    titleLarge = baseStyle(FontWeight.SemiBold, 20, 28),
    titleMedium = baseStyle(FontWeight.SemiBold, 16, 24),
    titleSmall = baseStyle(FontWeight.SemiBold, 14, 22),
    bodyLarge = baseStyle(FontWeight.Normal, 16, 26),
    bodyMedium = baseStyle(FontWeight.Normal, 14, 24),
    bodySmall = baseStyle(FontWeight.Normal, 12, 20),
    labelLarge = baseStyle(FontWeight.Medium, 14, 20),
    labelMedium = baseStyle(FontWeight.Medium, 12, 18),
    labelSmall = baseStyle(FontWeight.Medium, 11, 16)
)