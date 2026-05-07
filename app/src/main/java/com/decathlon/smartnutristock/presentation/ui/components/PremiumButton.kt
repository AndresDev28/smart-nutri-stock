package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * PremiumButton - Epicenter action button for Quick Scan.
 *
 * Visually dominant with RoyalBluePrimary color, 56dp minimum height (exceeds 48dp requirement),
 * 12dp rounded corners, and full width for thumb-zone optimization on XCover7.
 *
 * Supports optional gradient background via gradientBrush parameter.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    gradientBrush: Brush? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Background color animation for pressed state
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 300),
        label = "buttonBackgroundColor"
    )

    if (gradientBrush != null && enabled) {
        // Gradient button version
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(gradientBrush, RoundedCornerShape(12.dp))
                .clickable(
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = rememberRipple(bounded = true)
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    } else {
        // Standard button version
        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            enabled = enabled,
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = backgroundColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = backgroundColor,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 2.dp,
                disabledElevation = 0.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Preview for PremiumButton with icon
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun PremiumButtonPreview() {
    androidx.compose.material3.MaterialTheme {
        PremiumButton(
            text = "Escanear Producto",
            onClick = {},
            icon = Icons.Default.QrCodeScanner
        )
    }
}

/**
 * Preview for PremiumButton without icon
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun PremiumButtonNoIconPreview() {
    androidx.compose.material3.MaterialTheme {
        PremiumButton(
            text = "Escanear Producto",
            onClick = {}
        )
    }
}

/**
 * Preview for PremiumButton disabled state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun PremiumButtonDisabledPreview() {
    androidx.compose.material3.MaterialTheme {
        PremiumButton(
            text = "Escanear Producto",
            onClick = {},
            icon = Icons.Default.QrCodeScanner,
            enabled = false
        )
    }
}
