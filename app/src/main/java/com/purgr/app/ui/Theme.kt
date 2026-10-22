package com.purgr.app.ui

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.purgr.app.Accent
import com.purgr.app.Settings
import com.purgr.app.ThemeMode

/** Extra theme values Material doesn't model. */
@Immutable
data class PurgrExtras(
    val dark: Boolean,
    val hairline: Color,
    val keep: Color,
    val danger: Color,
)

val LocalPurgr = staticCompositionLocalOf { PurgrExtras(false, Color(0xFFE7E5E0), Color(0xFF2F7D5B), Color(0xFFD64545)) }

private val KeepLight = Color(0xFF2F7D5B)
private val KeepDark = Color(0xFF7FD1A8)
private val DangerLight = Color(0xFFD64545)
private val DangerDark = Color(0xFFFF8080)

private fun neutralLight(accent: Color, onAccent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accent.copy(alpha = 0.12f).compositeOver(Color.White),
    onPrimaryContainer = accent,
    secondary = Color(0xFF55555C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFEEEB),
    onSecondaryContainer = Color(0xFF17171A),
    background = Color(0xFFFAFAF8),
    onBackground = Color(0xFF17171A),
    surface = Color(0xFFFAFAF8),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFF1F0ED),
    onSurfaceVariant = Color(0xFF6E6E74),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF4F3F0),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEDECE8),
    outline = Color(0xFFD9D7D2),
    outlineVariant = Color(0xFFE7E5E0),
    error = DangerLight,
)

private fun neutralDark(accent: Color, onAccent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accent.copy(alpha = 0.16f).compositeOver(Color(0xFF151517)),
    onPrimaryContainer = accent,
    secondary = Color(0xFFB4B4BB),
    onSecondary = Color(0xFF151517),
    secondaryContainer = Color(0xFF26262A),
    onSecondaryContainer = Color(0xFFF2F2F3),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFF2F2F3),
    surface = Color(0xFF0E0E10),
    onSurface = Color(0xFFF2F2F3),
    surfaceVariant = Color(0xFF1E1E21),
    onSurfaceVariant = Color(0xFF9C9CA4),
    surfaceContainerLowest = Color(0xFF0B0B0C),
    surfaceContainerLow = Color(0xFF17171A),
    surfaceContainer = Color(0xFF19191C),
    surfaceContainerHigh = Color(0xFF1C1C1F),
    surfaceContainerHighest = Color(0xFF26262A),
    outline = Color(0xFF3A3A40),
    outlineVariant = Color(0xFF2A2A2F),
    error = DangerDark,
)

private val Sans = FontFamily.SansSerif

private val PurgrTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp),
)

private val PurgrShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PurgrTheme(content: @Composable () -> Unit) {
    val dark = when (Settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = Settings.accent
    val context = LocalContext.current

    val scheme: ColorScheme = if (accent == Accent.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dyn = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (dark) neutralDark(dyn.primary, dyn.onPrimary) else neutralLight(dyn.primary, dyn.onPrimary)
    } else {
        val a = Color(if (dark) accent.dark else accent.light)
        val onA = if (dark) Color(0xFF141416) else Color.White
        if (dark) neutralDark(a, onA) else neutralLight(a, onA)
    }

    val extras = PurgrExtras(
        dark = dark,
        hairline = scheme.outlineVariant,
        keep = if (dark) KeepDark else KeepLight,
        danger = if (dark) DangerDark else DangerLight,
    )
    CompositionLocalProvider(LocalPurgr provides extras) {
        MaterialTheme(colorScheme = scheme, typography = PurgrTypography, shapes = PurgrShapes, content = content)
    }
}

/** Keeps status/navigation bar icons readable for the in-app theme (not just the system one). */
@Composable
fun ComponentActivity.SyncSystemBars() {
    val dark = LocalPurgr.current.dark
    LaunchedEffect(dark) {
        val style = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
