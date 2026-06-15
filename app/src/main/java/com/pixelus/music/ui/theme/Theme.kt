package com.pixelus.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onPrimary = OnPrimary,
    onSecondary = OnSecondary,
    onBackground = OnBackground,
    onSurface = OnSurface,
    outline = DividerColor
)

@Composable
fun PixelusMusicTheme(
    dominantColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dominantColor != null) {
        darkColorScheme(
            primary = dominantColor,
            secondary = dominantColor.copy(alpha = 0.8f),
            background = Background,
            surface = Surface,
            surfaceVariant = SurfaceVariant,
            onPrimary = OnPrimary,
            onSecondary = OnSecondary,
            onBackground = OnBackground,
            onSurface = OnSurface,
            outline = DividerColor
        )
    } else if (Build.VERSION.SDK_INT >= 31) {
        try {
            dynamicDarkColorScheme(LocalContext.current)
        } catch (_: Exception) {
            DarkColorScheme
        }
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun extractDominantColor(bitmap: android.graphics.Bitmap): Color? {
    val palette = Palette.from(bitmap).generate()
    val swatch = palette?.vibrantSwatch
        ?: palette?.darkVibrantSwatch
        ?: palette?.dominantSwatch
        ?: return null
    return Color(swatch.rgb)
}
