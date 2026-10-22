package com.purgr.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KeepGreen = Color(0xFF3DDC84)
val DeleteRed = Color(0xFFFF5C6C)
val Bg = Color(0xFF0F1115)
val Surface = Color(0xFF1B1F27)

private val Scheme = darkColorScheme(
    primary = KeepGreen,
    onPrimary = Color.Black,
    secondary = DeleteRed,
    background = Bg,
    surface = Surface,
    onBackground = Color.White,
    onSurface = Color.White,
    error = DeleteRed,
)

@Composable
fun PurgrTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
