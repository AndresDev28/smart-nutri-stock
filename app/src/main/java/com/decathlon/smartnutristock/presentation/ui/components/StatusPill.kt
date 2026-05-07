package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.presentation.ui.theme.StatusAmber
import com.decathlon.smartnutristock.presentation.ui.theme.StatusDeepRed
import com.decathlon.smartnutristock.presentation.ui.theme.StatusTeal

/**
 * StatusPill - Badge component displaying status with icon and text.
 *
 * Displays a rounded pill with status-colored background (12% alpha) containing
 * an appropriate icon and status label. Designed for use in HistoryCard footer.
 *
 * @param status The semaphore status to display (GREEN, YELLOW, or EXPIRED)
 * @param modifier Optional modifier for the component
 */
@Composable
fun StatusPill(
    status: SemaphoreStatus,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        SemaphoreStatus.GREEN -> StatusTeal
        SemaphoreStatus.YELLOW -> StatusAmber
        SemaphoreStatus.EXPIRED -> StatusDeepRed
    }

    val statusIcon: ImageVector = when (status) {
        SemaphoreStatus.GREEN -> Icons.Default.CheckCircle
        SemaphoreStatus.YELLOW -> Icons.Default.Warning
        SemaphoreStatus.EXPIRED -> Icons.Default.Error
    }

    val statusLabel = when (status) {
        SemaphoreStatus.GREEN -> "Seguro"
        SemaphoreStatus.YELLOW -> "Próximo"
        SemaphoreStatus.EXPIRED -> "Caducado"
    }

    Surface(
        modifier = modifier
            .semantics {
                contentDescription = "Estado: $statusLabel"
            },
        shape = RoundedCornerShape(corner = CornerSize(8.dp)),
        color = statusColor.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = statusColor
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
    }
}

/**
 * Preview for StatusPill - GREEN (Seguro) state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun StatusPillGreenPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusPill(status = SemaphoreStatus.GREEN)
    }
}

/**
 * Preview for StatusPill - YELLOW (Próximo) state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun StatusPillYellowPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusPill(status = SemaphoreStatus.YELLOW)
    }
}

/**
 * Preview for StatusPill - EXPIRED (Caducado) state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun StatusPillExpiredPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusPill(status = SemaphoreStatus.EXPIRED)
    }
}
