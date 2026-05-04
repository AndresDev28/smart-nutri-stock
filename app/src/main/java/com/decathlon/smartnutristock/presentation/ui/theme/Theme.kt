package com.decathlon.smartnutristock.presentation.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// SmartNutriStockTheme - "Inventario Vivo" premium theme
// Dynamic color is DISABLED for fixed brand identity across all devices
@Composable
fun SmartNutriStockTheme(
    darkTheme: Boolean = false, // Fixed: no dark theme for now
    content: @Composable () -> Unit
) {
    // Use fixed LightColorScheme with "Inventario Vivo" palette
    // Dynamic color is disabled for consistent branding across warehouse devices
    val colorScheme = androidx.compose.material3.lightColorScheme(
        primary = RoyalBluePrimary,
        onPrimary = Color.White,
        primaryContainer = RoyalBlueDark,
        onPrimaryContainer = Color.White,
        secondary = StatusTeal,
        background = CreamBackground,
        surface = CardWhite,
        onBackground = TextDark,
        onSurface = TextDark,
        error = StatusDeepRed,
        onError = Color.White,
        surfaceVariant = CreamBackground,
        onSurfaceVariant = TextGray,
        outline = TextGray.copy(alpha = 0.2f)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
