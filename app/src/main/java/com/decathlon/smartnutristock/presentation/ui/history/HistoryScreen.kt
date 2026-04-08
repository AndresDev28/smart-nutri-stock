package com.decathlon.smartnutristock.presentation.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.presentation.ui.scanner.BottomSheetMode
import com.decathlon.smartnutristock.presentation.ui.scanner.ProductRegistrationBottomSheet
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * History screen for viewing all product batches.
 *
 * Features:
 * - Displays all batches from active_stocks table
 * - Shows product name, pack size, expiry date, quantity, and status
 * - Status is calculated dynamically based on TODAY's date (matching Dashboard)
 * - Loading state with spinner
 * - Error state with message
 * - Empty state when no batches
 * - Pull-to-refresh support (manual refresh)
 * - Edit batch via three-dot menu on each card
 * - Soft delete with undo functionality via Snackbar
 *
 * Performance:
 * - LazyColumn for performance
 * - Optimized for thumb zone (56dp minimum item height)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Collect undo state from ViewModel
    val undoState by viewModel.undoState.collectAsState()

    // Collect edit bottom sheet state from ViewModel
    val editBottomSheetState by viewModel.editBottomSheetState.collectAsState()

    // Collect action filter state from ViewModel
    val actionFilter by viewModel.actionFilter.collectAsState()

    // Snackbar host state for undo functionality
    val snackbarHostState = remember { SnackbarHostState() }

    // Show undo snackbar when undo state is PendingDelete
    LaunchedEffect(undoState) {
        when (val state = undoState) {
            is UndoState.PendingDelete -> {
                val result = snackbarHostState.showSnackbar(
                    message = "Lote eliminado. ¿Deshacer? (${state.secondsRemaining}s)",
                    actionLabel = "Deshacer",
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    viewModel.undoDelete()
                }
            }
            UndoState.Idle -> {
                // Snackbar is dismissed automatically
            }
        }
    }

    // Edit bottom sheet state
    val editBottomSheetStateInternal = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Auto-show/hide edit bottom sheet based on state
    LaunchedEffect(editBottomSheetState) {
        if (editBottomSheetState is EditBottomSheetState.Open) {
            editBottomSheetStateInternal.show()
        } else {
            editBottomSheetStateInternal.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de Productos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Filter chips row
                FilterChipsRow(
                    currentFilter = actionFilter,
                    onFilterSelected = { viewModel.setActionFilter(it) }
                )

                when (uiState) {
                    is HistoryUiState.Loading -> {
                        // Show loading spinner
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is HistoryUiState.Success -> {
                        val batches = (uiState as HistoryUiState.Success).batches

                        if (batches.isEmpty()) {
                            // Show empty state
                            EmptyState()
                        } else {
                            // Show batch list
                            BatchList(
                                batches = batches,
                                onEditClick = { batch -> viewModel.openEditBottomSheet(batch) },
                                onDeleteClick = { batch -> viewModel.softDeleteBatch(batch) },
                                onToggleActionClick = { batch -> viewModel.toggleBatchAction(batch) }
                            )
                        }
                    }

                    is HistoryUiState.Error -> {
                        // Show error message
                        ErrorScreen(message = (uiState as HistoryUiState.Error).message)
                    }
                }
            }
        }

        // Edit BottomSheet
        if (editBottomSheetState is EditBottomSheetState.Open) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { viewModel.closeEditBottomSheet() },
                sheetState = editBottomSheetStateInternal
            ) {
                val editBatch = (editBottomSheetState as EditBottomSheetState.Open).batch
                ProductRegistrationBottomSheet(
                    mode = BottomSheetMode.EDIT,
                    existingBatch = editBatch,
                    ean = editBatch.ean,
                    onDismiss = { viewModel.closeEditBottomSheet() },
                    onSave = { _, quantity, expiryDate, _, _, productName ->
                        viewModel.saveBatchUpdate(
                            barcode = editBatch.ean,
                            quantity = quantity,
                            expiryDate = expiryDate,
                            batchId = editBatch.id,
                            productName = productName
                        )
                    }
                )
            }
        }
    }
}

/**
 * Batch list with LazyColumn.
 * Optimized for thumb zone on Samsung XCover7.
 */
@Composable
private fun BatchList(
    batches: List<Batch>,
    onEditClick: (Batch) -> Unit,
    onDeleteClick: (Batch) -> Unit,
    onToggleActionClick: (Batch) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = batches,
            key = { it.id }
        ) { batch ->
            BatchCard(
                batch = batch,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onToggleActionClick = onToggleActionClick
            )
        }

        // Add bottom padding for scrolling
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Individual batch card component.
 * Thumb zone optimized (minimum 56dp height).
 * Uses pre-calculated status from Batch model to prevent recomposition loops.
 * Includes three-dot menu for edit and delete actions.
 * Shows action buttons for YELLOW and RED batches only.
 */
@Composable
private fun BatchCard(
    batch: Batch,
    onEditClick: (Batch) -> Unit,
    onDeleteClick: (Batch) -> Unit,
    onToggleActionClick: (Batch) -> Unit
) {
    val status = batch.status

    val (statusColor, statusText) = when (status) {
        com.decathlon.smartnutristock.domain.model.SemaphoreStatus.GREEN -> {
            Pair(Color(0xFF4CAF50), "Seguro")
        }
        com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW -> {
            Pair(Color(0xFFFFC107), "Por vencer")
        }
        com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED -> {
            Pair(Color(0xFFFF4444), "Expirado")
        }
    }

    // Determine if action buttons should be shown (YELLOW or RED only)
    val showActionButtons = status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW ||
            status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED

    // Format expiry date for display
    val expiryDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        .withZone(java.time.ZoneId.of("UTC"))
    val expiryDateText = expiryDateFormatter.format(batch.expiryDate)

    // Dropdown menu state
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = batch.name ?: "Producto desconocido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EAN: ${batch.ean}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, shape = CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )

                // Three-dot menu (48dp tap target for XCover7)
                Box(modifier = Modifier.size(48.dp)) {
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Dropdown menu
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // Edit option
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                    Text("Editar")
                                }
                            },
                            onClick = {
                                onEditClick(batch)
                                expanded = false
                            }
                        )

                        // Delete option
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                    Text("Eliminar")
                                }
                            },
                            onClick = {
                                onDeleteClick(batch)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                batch.packSize?.let {
                    Text(
                        text = "Pack: ${it}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "Cantidad: ${batch.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Vence: $expiryDateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Action buttons (only for YELLOW and RED batches)
            if (showActionButtons) {
                Spacer(modifier = Modifier.height(12.dp))

                when {
                    // CASE 1: RED/EXPIRED + PENDING → Only "Retirar" button (NO discount)
                    status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED &&
                    batch.actionTaken == com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING -> {
                        FilledTonalButton(
                            onClick = { onToggleActionClick(batch) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Retirar del Público")
                        }
                    }

                    // CASE 2: RED/EXPIRED + DISCOUNTED → Info indicator + Delete button
                    status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED &&
                    batch.actionTaken == com.decathlon.smartnutristock.domain.model.WorkflowAction.DISCOUNTED -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Non-interactive indicator
                            AssistChip(
                                label = { Text("Descuento Aplicado") },
                                onClick = {}, // Empty - not interactive
                                modifier = Modifier.weight(1f)
                            )
                            // Delete button
                            FilledTonalButton(
                                onClick = { onDeleteClick(batch) },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eliminar")
                            }
                        }
                    }

                    // CASE 3: RED/EXPIRED + REMOVED → Delete button (already correct)
                    status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED &&
                    batch.actionTaken == com.decathlon.smartnutristock.domain.model.WorkflowAction.REMOVED -> {
                        FilledTonalButton(
                            onClick = { onDeleteClick(batch) },
                            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    }

                    // CASE 4: YELLOW + PENDING → Both buttons (keep existing behavior)
                    status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW &&
                    batch.actionTaken == com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "Desc -20%" button
                            FilledTonalButton(
                                onClick = { onToggleActionClick(batch) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Desc -20%")
                            }
                            // "Retirar" button
                            FilledTonalButton(
                                onClick = { onToggleActionClick(batch) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Text("Retirar")
                            }
                        }
                    }

                    // CASE 5: YELLOW + DISCOUNTED → Undo button (keep existing behavior)
                    status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW &&
                    batch.actionTaken == com.decathlon.smartnutristock.domain.model.WorkflowAction.DISCOUNTED -> {
                        FilledTonalButton(
                            onClick = { onToggleActionClick(batch) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Deshacer Dto")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter chips row for filtering batches by action state.
 */
@Composable
private fun FilterChipsRow(
    currentFilter: com.decathlon.smartnutristock.presentation.ui.history.ActionFilter,
    onFilterSelected: (com.decathlon.smartnutristock.presentation.ui.history.ActionFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { onFilterSelected(com.decathlon.smartnutristock.presentation.ui.history.ActionFilter.ALL) },
            label = { Text("Todos") },
            colors = if (currentFilter == com.decathlon.smartnutristock.presentation.ui.history.ActionFilter.ALL) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                AssistChipDefaults.assistChipColors()
            },
            modifier = Modifier.weight(1f)
        )

        AssistChip(
            onClick = { onFilterSelected(com.decathlon.smartnutristock.presentation.ui.history.ActionFilter.PENDING) },
            label = { Text("Pendientes") },
            colors = if (currentFilter == com.decathlon.smartnutristock.presentation.ui.history.ActionFilter.PENDING) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                AssistChipDefaults.assistChipColors()
            },
            modifier = Modifier.weight(1f)
        )

        AssistChip(
            onClick = { onFilterSelected(com.decathlon.smartnutristock.presentation.ui.history.ActionFilter.WITH_ACTION) },
            label = { Text("Con acción") },
            colors = if (currentFilter == com.decathlon.smartnutristock.presentation.ui.history.ActionFilter.WITH_ACTION) {
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                AssistChipDefaults.assistChipColors()
            },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Empty state component.
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📦",
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No hay lotes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escanea un producto para agregar un lote",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Error screen component.
 */
@Composable
private fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌ Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Preview for BatchCard with PENDING action (YELLOW batch).
 */
@Preview(showBackground = true)
@Composable
private fun BatchCardPendingPreview() {
    val batch = Batch(
        id = "preview-1",
        ean = "8435408475366",
        quantity = 10,
        expiryDate = java.time.Instant.now().plusSeconds(15 * 24 * 60 * 60), // 15 days from now
        status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW,
        name = "Protein Whey 500g",
        packSize = 500,
        actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
    )

    androidx.compose.material3.MaterialTheme {
        BatchCard(
            batch = batch,
            onEditClick = {},
            onDeleteClick = {},
            onToggleActionClick = {}
        )
    }
}

/**
 * Preview for BatchCard with DISCOUNTED action (YELLOW batch).
 */
@Preview(showBackground = true)
@Composable
private fun BatchCardDiscountedPreview() {
    val batch = Batch(
        id = "preview-2",
        ean = "8435408475366",
        quantity = 10,
        expiryDate = java.time.Instant.now().plusSeconds(15 * 24 * 60 * 60), // 15 days from now
        status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW,
        name = "Protein Whey 500g",
        packSize = 500,
        actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.DISCOUNTED
    )

    androidx.compose.material3.MaterialTheme {
        BatchCard(
            batch = batch,
            onEditClick = {},
            onDeleteClick = {},
            onToggleActionClick = {}
        )
    }
}

/**
 * Preview for BatchCard with REMOVED action (RED/EXPIRED batch).
 * Shows the DELETE button (unidirectional flow).
 */
@Preview(showBackground = true)
@Composable
private fun BatchCardRemovedPreview() {
    val batch = Batch(
        id = "preview-3",
        ean = "8435408475366",
        quantity = 10,
        expiryDate = java.time.Instant.now().minusSeconds(1 * 24 * 60 * 60), // 1 day ago
        status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED,
        name = "Protein Whey 500g",
        packSize = 500,
        actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.REMOVED
    )

    androidx.compose.material3.MaterialTheme {
        BatchCard(
            batch = batch,
            onEditClick = {},
            onDeleteClick = {},
            onToggleActionClick = {}
        )
    }
}

/**
 * Preview for BatchCard with PENDING action (RED/EXPIRED batch).
 * Shows the RETIRAR button (which applies REMOVED action).
 */
@Preview(showBackground = true)
@Composable
private fun BatchCardRedPendingPreview() {
    val batch = Batch(
        id = "preview-4",
        ean = "8435408475366",
        quantity = 10,
        expiryDate = java.time.Instant.now().minusSeconds(1 * 24 * 60 * 60), // 1 day ago
        status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED,
        name = "Protein Whey 500g",
        packSize = 500,
        actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
    )

    androidx.compose.material3.MaterialTheme {
        BatchCard(
            batch = batch,
            onEditClick = {},
            onDeleteClick = {},
            onToggleActionClick = {}
        )
    }
}

/**
 * Preview for BatchCard with DISCOUNTED action (RED/EXPIRED batch).
 * Shows the hybrid state: non-interactive "Descuento Aplicado" chip + RED "Eliminar" button.
 */
@Preview(name = "BatchCard - RED/EXPIRED + DISCOUNTED (Hybrid)", showBackground = true)
@Composable
private fun BatchCardRedDiscountedPreview() {
    val batch = Batch(
        id = "preview-5",
        ean = "8435408475366",
        quantity = 10,
        expiryDate = java.time.Instant.now().minusSeconds(1 * 24 * 60 * 60), // 1 day ago
        status = com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED,
        name = "Barrita Energética Chocolate", // Test long name
        packSize = 500,
        actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.DISCOUNTED
    )

    androidx.compose.material3.MaterialTheme {
        BatchCard(
            batch = batch,
            onEditClick = {},
            onDeleteClick = {},
            onToggleActionClick = {}
        )
    }
}
