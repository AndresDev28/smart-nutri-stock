package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.presentation.ui.theme.RoyalBluePrimary
import com.decathlon.smartnutristock.presentation.ui.theme.StatusAmber
import com.decathlon.smartnutristock.presentation.ui.theme.StatusDeepRed
import com.decathlon.smartnutristock.presentation.ui.theme.StatusTeal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * HistoryCard - Text-only card for displaying batch information in History screen.
 *
 * Organizes batch information into three vertical blocks:
 * - Header Block: Product name (Plus Jakarta Sans SemiBold) + EAN (JetBrains Mono) + MoreVert menu
 * - Attributes Block: Pack size + Quantity with outline icons
 * - Footer Block: Expiry date + StatusPill
 *
 * Designed for Samsung XCover7 with tap targets >= 48dp and text overflow handling.
 *
 * @param batch The batch domain model containing all display data
 * @param onEditClick Callback triggered when Edit option is selected from menu
 * @param onDeleteClick Callback triggered when Delete option is selected from menu
 * @param modifier Optional modifier for the component
 */
@Composable
fun HistoryCard(
    batch: Batch,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Format expiry date for display
    val expiryDate = batch.expiryDate.atZone(ZoneId.systemDefault()).toLocalDate()
    val formattedDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(expiryDate)

    // Get status color for date text
    val statusColor = when (batch.status) {
        SemaphoreStatus.GREEN -> StatusTeal
        SemaphoreStatus.YELLOW -> StatusAmber
        SemaphoreStatus.EXPIRED -> StatusDeepRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Block: Product name + EAN + MoreVert
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product name
                Text(
                    text = batch.name ?: "Producto desconocido",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // MoreVert menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones para ${batch.name ?: "producto"}"
                        )
                    }

                    // Dropdown menu
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // EAN (JetBrains Mono, RoyalBluePrimary)
            Text(
                text = "EAN: ${batch.ean}",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = RoyalBluePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Attributes Block: Pack + Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pack attribute
                AttributeItem(
                    icon = Icons.Default.CalendarMonth,
                    label = "Pack:",
                    value = "${batch.packSize ?: 0}g"
                )

                // Quantity attribute
                AttributeItem(
                    icon = Icons.Default.Inventory,
                    label = "Cantidad:",
                    value = batch.quantity.toString()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Block: Expiry date + StatusPill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Expiry date
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append("Vence: ")
                        }
                        withStyle(SpanStyle(color = statusColor)) {
                            append(formattedDate)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                // StatusPill
                StatusPill(status = batch.status)
            }
        }
    }
}

/**
 * AttributeItem - Helper component for displaying attribute with icon, label, and value.
 *
 * @param icon The outline icon to display
 * @param label The small label text (e.g., "Pack:")
 * @param value The bold value text (e.g., "475g")
 */
@Composable
private fun AttributeItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Preview for HistoryCard - GREEN status
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun HistoryCardGreenPreview() {
    androidx.compose.material3.MaterialTheme {
        HistoryCard(
            batch = com.decathlon.smartnutristock.domain.model.Batch(
                id = "batch-1",
                ean = "8001234567890",
                quantity = 10,
                expiryDate = java.time.Instant.parse("2025-12-31T00:00:00Z"),
                status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.GREEN,
                name = "Proteína Whey Isolate 2kg",
                packSize = 2000,
                deletedAt = null,
                actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

/**
 * Preview for HistoryCard - YELLOW status
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun HistoryCardYellowPreview() {
    androidx.compose.material3.MaterialTheme {
        HistoryCard(
            batch = com.decathlon.smartnutristock.domain.model.Batch(
                id = "batch-2",
                ean = "8009876543210",
                quantity = 5,
                expiryDate = java.time.Instant.parse("2025-05-10T00:00:00Z"),
                status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW,
                name = "Creatine Monohydrate 500g",
                packSize = 500,
                deletedAt = null,
                actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

/**
 * Preview for HistoryCard - EXPIRED status
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun HistoryCardExpiredPreview() {
    androidx.compose.material3.MaterialTheme {
        HistoryCard(
            batch = com.decathlon.smartnutristock.domain.model.Batch(
                id = "batch-3",
                ean = "8005555555555",
                quantity = 2,
                expiryDate = java.time.Instant.parse("2025-04-20T00:00:00Z"),
                status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED,
                name = "Vitamin C 1000mg",
                packSize = 100,
                deletedAt = null,
                actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

/**
 * Preview for HistoryCard with null name
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun HistoryCardNullNamePreview() {
    androidx.compose.material3.MaterialTheme {
        HistoryCard(
            batch = com.decathlon.smartnutristock.domain.model.Batch(
                id = "batch-4",
                ean = "8003333333333",
                quantity = 15,
                expiryDate = java.time.Instant.parse("2025-12-01T00:00:00Z"),
                status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.GREEN,
                name = null,
                packSize = 750,
                deletedAt = null,
                actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
            ),
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
