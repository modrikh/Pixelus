package com.pixelus.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle as MaterialkolorPaletteStyle
import com.materialkolor.ktx.toHct
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
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    animate: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (appearance) {
        Appearance.Light -> false
        Appearance.Dark -> true
        Appearance.System -> isSystemInDarkTheme()
    }

    val seedColor = dominantColor ?: Primary
    val useMaterialKolor = Build.VERSION.SDK_INT >= 31 && useDynamicColor

    if (useMaterialKolor) {
        DynamicMaterialTheme(
            seedColor = if (dominantColor != null && dominantColor.toHct().chroma <= 20f) seedColor else seedColor,
            useDarkTheme = isDark,
            withAmoled = amoledDarkMode,
            style = when (paletteStyle) {
                PaletteStyle.TonalSpot -> MaterialkolorPaletteStyle.TonalSpot
                PaletteStyle.Neutral -> MaterialkolorPaletteStyle.Neutral
                PaletteStyle.Vibrant -> MaterialkolorPaletteStyle.Vibrant
                PaletteStyle.Expressive -> MaterialkolorPaletteStyle.Expressive
                PaletteStyle.Rainbow -> MaterialkolorPaletteStyle.Rainbow
                PaletteStyle.FruitSalad -> MaterialkolorPaletteStyle.FruitSalad
                PaletteStyle.Monochrome -> MaterialkolorPaletteStyle.Monochrome
                PaletteStyle.Fidelity -> MaterialkolorPaletteStyle.Fidelity
                PaletteStyle.Content -> MaterialkolorPaletteStyle.Content
            },
            animationSpec = androidx.compose.animation.core.tween(300, 200),
            animate = animate
        ) {
            content()
        }
    } else {
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
}

fun extractDominantColor(bitmap: android.graphics.Bitmap): Color? {
    val palette = Palette.from(bitmap).generate()
    val swatch = palette?.vibrantSwatch
        ?: palette?.darkVibrantSwatch
        ?: palette?.dominantSwatch
        ?: return null
    return Color(swatch.rgb)
}
