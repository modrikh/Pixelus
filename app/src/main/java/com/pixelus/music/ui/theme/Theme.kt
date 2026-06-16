package com.pixelus.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.pixelus.music.data.Appearance

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

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = PrimaryVariant,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFFE0E0E0),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    outline = Color(0xFFCCCCCC)
)

@Composable
fun PixelusMusicTheme(
    dominantColor: Color? = null,
    appearance: Appearance = Appearance.System,
    useDynamicColor: Boolean = true,
    amoledDarkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (appearance) {
        Appearance.Light -> false
        Appearance.Dark -> true
        Appearance.System -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dominantColor != null -> {
            if (isDark) {
                darkColorScheme(
                    primary = dominantColor,
                    secondary = dominantColor.copy(alpha = 0.8f),
                    background = if (amoledDarkMode) Color.Black else Background,
                    surface = if (amoledDarkMode) Color.Black else Surface,
                    surfaceVariant = SurfaceVariant,
                    onPrimary = OnPrimary,
                    onSecondary = OnSecondary,
                    onBackground = OnBackground,
                    onSurface = OnSurface,
                    outline = DividerColor
                )
            } else {
                lightColorScheme(
                    primary = dominantColor,
                    secondary = dominantColor.copy(alpha = 0.8f),
                    background = Color.White,
                    surface = Color(0xFFF5F5F5),
                    surfaceVariant = Color(0xFFE0E0E0),
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onBackground = Color(0xFF1A1A1A),
                    onSurface = Color(0xFF1A1A1A),
                    outline = Color(0xFFCCCCCC)
                )
            }
        }
        useDynamicColor && Build.VERSION.SDK_INT >= 31 -> {
            if (isDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        isDark -> {
            darkColorScheme(
                primary = Primary,
                secondary = Secondary,
                background = if (amoledDarkMode) Color.Black else Background,
                surface = if (amoledDarkMode) Color.Black else Surface,
                surfaceVariant = SurfaceVariant,
                onPrimary = OnPrimary,
                onSecondary = OnSecondary,
                onBackground = OnBackground,
                onSurface = OnSurface,
                outline = DividerColor
            )
        }
        else -> LightColorScheme
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
