package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.presentation.ui.theme.ShadowColor
import com.decathlon.smartnutristock.presentation.ui.theme.StatusTeal
import com.decathlon.smartnutristock.presentation.ui.theme.StatusAmber
import com.decathlon.smartnutristock.presentation.ui.theme.StatusDeepRed

/**
 * NutriCard - Signature card component for displaying product information.
 *
 * Features:
 * - 6dp status line on left (vertical) matching semaphore status color
 * - 24dp rounded corners (large shape)
 * - Custom shadow using ShadowColor with 4dp elevation
 * - Product name in titleMedium Bold
 * - EAN code in labelMedium (JetBrains Mono)
 * - Quantity and expiry date as InfoChips
 * - StatusCircle indicator on the right
 * - Optional onClick for interactivity
 *
 * All status logic is calculated by CalculateStatusUseCase and passed as parameter.
 * This composable is a pure renderer - it never calculates status.
 */
@Composable
fun NutriCard(
    productName: String,
    ean: String,
    quantity: Int,
    expiryDate: String,
    status: SemaphoreStatus,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        SemaphoreStatus.GREEN -> StatusTeal
        SemaphoreStatus.YELLOW -> StatusAmber
        SemaphoreStatus.EXPIRED -> StatusDeepRed
    }

    val cardColors = CardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val cardModifier = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 4.dp,
            shape = MaterialTheme.shapes.large,
            ambientColor = ShadowColor,
            spotColor = ShadowColor
        )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Card(
        modifier = cardModifier.then(clickableModifier),
        shape = MaterialTheme.shapes.large,
        colors = cardColors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 6dp status line on left (vertical)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(color = statusColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Content column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
            ) {
                // Product name
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // EAN code (JetBrains Mono)
                Text(
                    text = "EAN: $ean",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // InfoChips for quantity and expiry date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(
                        icon = Icons.Default.Inventory,
                        text = "$quantity unidades"
                    )

                    InfoChip(
                        icon = Icons.Default.CalendarMonth,
                        text = expiryDate
                    )
                }
            }

            // StatusCircle
            StatusCircle(status = status)

            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

/**
 * Preview for NutriCard - GREEN status
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun NutriCardGreenPreview() {
    androidx.compose.material3.MaterialTheme {
        NutriCard(
            productName = "Proteína Whey Isolate 2kg",
            ean = "8001234567890",
            quantity = 24,
            expiryDate = "2025-06-15",
            status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.GREEN
        )
    }
}

/**
 * Preview for NutriCard - YELLOW status
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun NutriCardYellowPreview() {
    androidx.compose.material3.MaterialTheme {
        NutriCard(
            productName = "Creatine Monohydrate 500g",
            ean = "8009876543210",
            quantity = 12,
            expiryDate = "2025-05-10",
            status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW
        )
    }
}

/**
 * Preview for NutriCard - EXPIRED status
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun NutriCardExpiredPreview() {
    androidx.compose.material3.MaterialTheme {
        NutriCard(
            productName = "Vitamin C 1000mg",
            ean = "8005555555555",
            quantity = 5,
            expiryDate = "2025-04-20",
            status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED
        )
    }
}

/**
 * Preview for NutriCard with onClick
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun NutriCardClickablePreview() {
    androidx.compose.material3.MaterialTheme {
        NutriCard(
            productName = "Omega-3 Fish Oil 1000mg",
            ean = "8003333333333",
            quantity = 30,
            expiryDate = "2025-12-01",
            status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.GREEN,
            onClick = {}
        )
    }
}
