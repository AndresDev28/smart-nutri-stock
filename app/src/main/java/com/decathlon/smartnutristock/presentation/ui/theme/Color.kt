package com.decathlon.smartnutristock.presentation.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Inventario Vivo" Palette - Base colors
val CreamBackground = Color(0xFFF8FAFC)
val RoyalBluePrimary = Color(0xFF2563EB)
val RoyalBlueDark = Color(0xFF1E40AF)
val StatusTeal = Color(0xFF14B8A6)      // Stock óptimo / GREEN
val StatusAmber = Color(0xFFF59E0B)     // Stock por caducar / YELLOW
val StatusDeepRed = Color(0xFFDC2626)   // Sin caducado / RED
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1E293B)
val TextGray = Color(0xFF64748B)

// Custom shadow color for Industrial Elegance look
val ShadowColor = Color(0xFF000000).copy(alpha = 0.08f)

// Status color accessor functions for use in Composable contexts
@Composable
fun statusTeal(): Color = StatusTeal

@Composable
fun statusAmber(): Color = StatusAmber

@Composable
fun statusDeepRed(): Color = StatusDeepRed

@Composable
fun statusGreen(): Color = StatusTeal // Alias for semantic clarity

// Material 3 Light Color Scheme mapping
private val LightColorScheme = androidx.compose.material3.lightColorScheme(
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
