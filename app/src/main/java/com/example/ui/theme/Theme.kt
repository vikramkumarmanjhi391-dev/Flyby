package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val FlyByDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF04181C),
    primaryContainer = Color(0xFF083942),
    onPrimaryContainer = Color(0xFFB5FAFF),
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF341857),
    onSecondaryContainer = Color(0xFFE9D4FF),
    tertiary = PlasmaPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4F102B),
    onTertiaryContainer = Color(0xFFFFD9E4),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = DeepIndigoSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CoralAlert,
    onError = Color.White
)

val FlyByShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun FlyByTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = FlyByDarkColorScheme,
        typography = Typography,
        shapes = FlyByShapes,
        content = content
    )
}
