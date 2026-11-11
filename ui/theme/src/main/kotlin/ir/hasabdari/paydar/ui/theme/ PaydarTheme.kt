package ir.hasabdari.paydar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColors = lightColorScheme(
    primary = Teal700,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    secondary = Amber500,
    onSecondary = Slate900,
    secondaryContainer = Amber100,
    onSecondaryContainer = Amber700,
    tertiary = Violet600,
    onTertiary = Color.White,
    tertiaryContainer = Violet100,
    onTertiaryContainer = Violet700,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose100,
    onErrorContainer = Rose600,
    outline = Slate200
)

private val DarkColors = darkColorScheme(
    primary = Teal400,
    onPrimary = Slate900,
    primaryContainer = Teal900,
    onPrimaryContainer = Teal100,
    secondary = Amber400,
    onSecondary = Slate900,
    secondaryContainer = Amber700,
    onSecondaryContainer = Amber100,
    tertiary = Violet500,
    onTertiary = Color.White,
    tertiaryContainer = Violet700,
    onTertiaryContainer = Violet100,
    background = Color(0xFF0B1220),
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    error = Rose500,
    onError = Color.White,
    errorContainer = Rose600,
    onErrorContainer = Rose100,
    outline = Slate700
)

@Composable
fun PaydarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // کل اپ راست‌چین است — یک‌بار و برای همیشه
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PaydarTypography,
            shapes = PaydarShapes,
            content = content
        )
    }
}