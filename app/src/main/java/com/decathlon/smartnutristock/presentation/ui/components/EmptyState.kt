package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.R

/**
 * EmptyState - Motivational empty state component with official app logo.
 *
 * Displays a centered column with the Smart Nutri-Stock logo illustration,
 * a motivational message, and optional subtitle.
 */
@Composable
fun EmptyState(
    message: String,
    subtitle: String = "",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Display official app logo OR provided icon
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            // Use Image (not Icon) to preserve original logo colors/gradients
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Preview for EmptyState component
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun EmptyStatePreview() {
    androidx.compose.material3.MaterialTheme {
        EmptyState(
            message = "Listo para la recepción de hoy",
            subtitle = "Escanea tu primer producto para comenzar"
        )
    }
}

/**
 * Preview for EmptyState with icon
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun EmptyStateWithIconPreview() {
    androidx.compose.material3.MaterialTheme {
        EmptyState(
            message = "No hay productos en esta categoría",
            subtitle = "Prueba con otro filtro o escanea nuevos productos",
            icon = Icons.Default.Inventory
        )
    }
}
