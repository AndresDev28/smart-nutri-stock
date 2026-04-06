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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            when (uiState) {
                is HistoryUiState.Loading -> {
                    // Show loading spinner
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                            onDeleteClick = { batch -> viewModel.softDeleteBatch(batch) }
                        )
                    }
                }

                is HistoryUiState.Error -> {
                    // Show error message
                    ErrorScreen(message = (uiState as HistoryUiState.Error).message)
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
    onDeleteClick: (Batch) -> Unit
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
                onDeleteClick = onDeleteClick
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
 */
@Composable
private fun BatchCard(
    batch: Batch,
    onEditClick: (Batch) -> Unit,
    onDeleteClick: (Batch) -> Unit
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
        }
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
