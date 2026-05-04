package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.presentation.ui.theme.StatusTeal
import com.decathlon.smartnutristock.presentation.ui.theme.StatusAmber
import com.decathlon.smartnutristock.presentation.ui.theme.StatusDeepRed

/**
 * StatusCircle - Circular status indicator with subtle background.
 *
 * Uses 40dp size with 0.1f alpha background color matching the status.
 * Icons: CheckCircle for GREEN, Warning for YELLOW/EXPIRED.
 */
@Composable
fun StatusCircle(
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

    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = Color(
                    red = statusColor.red,
                    green = statusColor.green,
                    blue = statusColor.blue,
                    alpha = 0.1f
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = statusIcon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = statusColor
        )
    }
}

/**
 * Preview for StatusCircle - GREEN state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun StatusCircleGreenPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusCircle(status = SemaphoreStatus.GREEN)
    }
}

/**
 * Preview for StatusCircle - YELLOW state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun StatusCircleAmberPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusCircle(status = SemaphoreStatus.YELLOW)
    }
}

/**
 * Preview for StatusCircle - EXPIRED state
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun StatusCircleRedPreview() {
    androidx.compose.material3.MaterialTheme {
        StatusCircle(status = SemaphoreStatus.EXPIRED)
    }
}
