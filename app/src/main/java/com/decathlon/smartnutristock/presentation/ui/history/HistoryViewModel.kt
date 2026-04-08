package com.decathlon.smartnutristock.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.domain.export.ExportFormat
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.usecase.ExportInventoryUseCase
import com.decathlon.smartnutristock.domain.usecase.GetAllBatchesUseCase
import com.decathlon.smartnutristock.domain.usecase.RestoreBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.SoftDeleteBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchActionUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateProductNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * ViewModel for History screen.
 *
 * Responsibilities:
 * - Loads all batches from GetAllBatchesUseCase ONCE
 * - Uses Batch model which already has dynamically calculated status
 * - Exposes reactive state via StateFlow
 * - Handles loading and error states
 * - Handles batch editing via ProductRegistrationBottomSheet
 * - Handles soft delete with undo functionality
 * - Handles inventory export to CSV/PDF formats
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllBatchesUseCase: GetAllBatchesUseCase,
    private val softDeleteBatchUseCase: SoftDeleteBatchUseCase,
    private val restoreBatchUseCase: RestoreBatchUseCase,
    private val updateBatchUseCase: UpdateBatchUseCase,
    private val updateProductNameUseCase: UpdateProductNameUseCase,
    private val updateBatchActionUseCase: UpdateBatchActionUseCase,
    private val exportInventoryUseCase: ExportInventoryUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<HistoryUiState>(
        value = HistoryUiState.Loading
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    // Undo State
    private val _undoState = MutableStateFlow<UndoState>(UndoState.Idle)
    val undoState: StateFlow<UndoState> = _undoState.asStateFlow()

    // Edit BottomSheet State
    private val _editBottomSheetState = MutableStateFlow<EditBottomSheetState>(EditBottomSheetState.Closed)
    val editBottomSheetState: StateFlow<EditBottomSheetState> = _editBottomSheetState.asStateFlow()

    // Action Filter State
    private val _actionFilter = MutableStateFlow(ActionFilter.ALL)
    val actionFilter: StateFlow<ActionFilter> = _actionFilter.asStateFlow()

    // Export State
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    // Export Event (one-shot)
    private val _exportEvent = MutableSharedFlow<ExportEvent>()
    val exportEvent = _exportEvent.asSharedFlow()

    // Undo timer job
    private var undoJob: Job? = null

    init {
        // Load batches ONCE on initialization
        loadBatchesOnce()
    }

    /**
     * Load all batches ONCE (not a flow collector).
     * This prevents infinite recomposition loop.
     * Status is already calculated dynamically in StockRepository.
     * Batches are filtered by action state based on current filter selection.
     */
    private fun loadBatchesOnce() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            try {
                // Use first() from flow to get batches once, not collect
                val allBatches = getAllBatchesUseCase().first()

                // Filter batches by action state based on current filter
                val filteredBatches = when (_actionFilter.value) {
                    ActionFilter.ALL -> allBatches
                    ActionFilter.PENDING -> allBatches.filter { it.actionTaken == com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING }
                    ActionFilter.WITH_ACTION -> allBatches.filter { it.actionTaken != com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING }
                }

                _uiState.value = HistoryUiState.Success(filteredBatches)
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.message ?: "Error al cargar lotes")
            }
        }
    }

    /**
     * Reload batches (user-triggered refresh).
     */
    fun refresh() {
        loadBatchesOnce()
    }

    /**
     * Toggle workflow action for a batch.
     *
     * - If PENDING → becomes DISCOUNTED (for YELLOW batches)
     * - If PENDING → becomes REMOVED (for RED/expired batches)
     * - If DISCOUNTED/REMOVED → reverts to PENDING
     *
     * @param batch The batch to toggle action for
     */
    fun toggleBatchAction(batch: Batch) {
        viewModelScope.launch {
            // Determine new action based on current state and semaphore status
            val newAction = when (batch.actionTaken) {
                com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING -> {
                    // If YELLOW (pending expiry), mark as DISCOUNTED
                    // If RED/EXPIRED, mark as REMOVED
                    if (batch.status == com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED) {
                        com.decathlon.smartnutristock.domain.model.WorkflowAction.REMOVED
                    } else {
                        com.decathlon.smartnutristock.domain.model.WorkflowAction.DISCOUNTED
                    }
                }
                else -> {
                    // If action already taken, revert to PENDING
                    com.decathlon.smartnutristock.domain.model.WorkflowAction.PENDING
                }
            }

            // Update the action in database
            updateBatchActionUseCase(batch.id, newAction)

            // Reload batches to show updated state
            loadBatchesOnce()
        }
    }

    /**
     * Set the action filter for batch display.
     *
     * @param filter The filter to apply (ALL, PENDING, WITH_ACTION)
     */
    fun setActionFilter(filter: ActionFilter) {
        _actionFilter.value = filter
        // Reload batches with new filter
        loadBatchesOnce()
    }

    /**
     * Open the edit bottom sheet for a specific batch.
     *
     * @param batch The batch to edit
     */
    fun openEditBottomSheet(batch: Batch) {
        _editBottomSheetState.value = EditBottomSheetState.Open(batch)
    }

    /**
     * Close the edit bottom sheet.
     */
    fun closeEditBottomSheet() {
        _editBottomSheetState.value = EditBottomSheetState.Closed
    }

    /**
     * Save batch updates from the edit bottom sheet.
     *
     * @param barcode The EAN code (should match the batch's EAN)
     * @param quantity The new quantity
     * @param expiryDate The new expiry date
     * @param batchId The ID of the batch to update
     * @param productName The updated product name (if changed)
     */
    @Suppress("UNUSED_PARAMETER")
    fun saveBatchUpdate(
        barcode: String,
        quantity: Int,
        expiryDate: LocalDate,
        batchId: String,
        productName: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Get the current batch from the edit state
                val currentBatch = (_editBottomSheetState.value as? EditBottomSheetState.Open)?.batch
                    ?: throw IllegalStateException("No batch selected for editing")

                // Convert LocalDate to Instant (UTC)
                val expiryInstant = expiryDate.atStartOfDay(ZoneId.of("UTC")).toInstant()

                // Create updated batch object
                val updatedBatch = currentBatch.copy(
                    quantity = quantity,
                    expiryDate = expiryInstant
                )

                // Update the batch via use case
                updateBatchUseCase(updatedBatch)

                // Update product name if provided and different from current
                if (productName != null && productName.isNotBlank() && productName != currentBatch.name) {
                    updateProductNameUseCase(barcode, productName)
                }

                // Close the bottom sheet
                closeEditBottomSheet()

                // Reload batches to show updated data
                loadBatchesOnce()
            } catch (e: Exception) {
                // Handle error - could show a toast or update error state
                _uiState.value = HistoryUiState.Error("Error al actualizar lote: ${e.message}")
            }
        }
    }

    /**
     * Soft delete a batch with undo support.
     *
     * This method performs a soft delete on the batch and starts a 5-second
     * countdown timer. During this countdown, the user can undo the deletion.
     * If the countdown expires, the deletion is finalized.
     *
     * The batch is immediately removed from the UI state for instant feedback.
     *
     * @param batch The batch to delete
     */
    fun softDeleteBatch(batch: Batch) {
        viewModelScope.launch {
            // Optimistic update: Remove batch from UI immediately
            val currentBatches = (_uiState.value as? HistoryUiState.Success)?.batches
            if (currentBatches != null) {
                val updatedBatches = currentBatches.filter { it.id != batch.id }
                _uiState.value = HistoryUiState.Success(updatedBatches)
            }

            // Perform soft delete
            val timestamp = Instant.now(Clock.systemUTC())
            softDeleteBatchUseCase(batch.id, timestamp)

            // Cancel any existing undo job
            undoJob?.cancel()

            // Start 5-second countdown
            undoJob = launch {
                for (secondsRemaining in 5 downTo 1) {
                    _undoState.value = UndoState.PendingDelete(batch, secondsRemaining)
                    delay(1000L) // Wait 1 second
                }

                // Timer expired - finalize delete
                finalizeDelete()
            }
        }
    }

    /**
     * Undo a soft delete operation.
     *
     * Cancels the countdown timer and restores the batch by calling
     * the RestoreBatchUseCase, making the batch visible again.
     *
     * The batch is immediately added back to the UI state for instant feedback.
     */
    fun undoDelete() {
        viewModelScope.launch {
            // Cancel the countdown timer
            undoJob?.cancel()
            undoJob = null

            // Optimistic update: Add batch back to UI immediately
            val currentUndoState = _undoState.value
            if (currentUndoState is UndoState.PendingDelete) {
                val currentBatches = (_uiState.value as? HistoryUiState.Success)?.batches
                if (currentBatches != null) {
                    // Add the batch back to the list, maintaining sorted order by expiry date
                    val updatedBatches = (currentBatches + currentUndoState.batch)
                        .sortedBy { it.expiryDate }
                    _uiState.value = HistoryUiState.Success(updatedBatches)
                }

                // Restore the batch in the database
                restoreBatchUseCase(currentUndoState.batch.id)
            }

            // Reset undo state to idle
            _undoState.value = UndoState.Idle

            // Reload batches to ensure UI is in sync with database
            loadBatchesOnce()
        }
    }

    /**
     * Finalize a soft delete operation.
     *
     * Called when the countdown timer expires. The soft delete has already
     * been performed, so we just need to clean up the state.
     * No additional DB action is needed since the soft delete happened
     * when softDeleteBatch was called.
     */
    private fun finalizeDelete() {
        _undoState.value = UndoState.Idle
        undoJob = null
    }

    /**
     * Export inventory to the specified format.
     *
     * This method orchestrates the export process:
     * 1. Sets export state to Loading
     * 2. Calls ExportInventoryUseCase with the selected format
     * 3. Updates export state to Success or Error
     * 4. Emits ShareFile event on success for the UI to trigger sharing
     *
     * @param format The export format (CSV or PDF)
     */
    fun onExportFormatSelected(format: ExportFormat) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading

            exportInventoryUseCase(format)
                .onSuccess { file ->
                    val mimeType = when (format) {
                        ExportFormat.CSV -> "text/csv"
                        ExportFormat.PDF -> "application/pdf"
                    }
                    _exportState.value = ExportState.Success(file)
                    _exportEvent.emit(ExportEvent.ShareFile(file, mimeType))
                }
                .onFailure { error ->
                    _exportState.value = ExportState.Error(error.message ?: "Error al exportar")
                }
        }
    }

    /**
     * Clear the export state back to Idle.
     *
     * This should be called after the share intent is launched or after
     * displaying an error message to the user.
     */
    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel the undo job to prevent memory leaks
        undoJob?.cancel()
    }
}

/**
 * UI State for History screen.
 * Sealed class enables exhaustive `when()` expressions in Compose UI.
 */
sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Success(val batches: List<Batch>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}

/**
 * Undo state for batch deletion with countdown timer.
 *
 * This sealed class represents the different states of the undo mechanism
 * for soft-deleting batches. The pending delete state includes a countdown
 * timer that allows users to undo the deletion within 5 seconds.
 */
sealed class UndoState {
    /**
     * No undo operation in progress.
     */
    data object Idle : UndoState()

    /**
     * A batch is pending deletion with an active countdown timer.
     *
     * @property batch The batch that was soft deleted
     * @property secondsRemaining Number of seconds left in the countdown (1-5)
     */
    data class PendingDelete(val batch: Batch, val secondsRemaining: Int) : UndoState()
}

/**
 * State for the edit bottom sheet.
 *
 * This sealed class represents whether the edit bottom sheet is closed or open,
 * and if open, which batch is being edited.
 */
sealed class EditBottomSheetState {
    /**
     * The bottom sheet is closed.
     */
    data object Closed : EditBottomSheetState()

    /**
     * The bottom sheet is open with a specific batch for editing.
     *
     * @property batch The batch being edited
     */
    data class Open(val batch: Batch) : EditBottomSheetState()
}

/**
 * Action filter enum for filtering batches by workflow action state.
 */
enum class ActionFilter {
    /**
     * Show all batches regardless of action state.
     */
    ALL,

    /**
     * Show only batches with PENDING action (no action taken yet).
     */
    PENDING,

    /**
     * Show only batches with action taken (DISCOUNTED or REMOVED).
     */
    WITH_ACTION
}

/**
 * Export state for inventory export operation.
 *
 * This sealed class represents the different states of the export process,
 * allowing the UI to show appropriate feedback (loading indicator, error message, etc.).
 */
sealed class ExportState {
    /**
     * No export operation in progress.
     */
    data object Idle : ExportState()

    /**
     * Export is currently in progress.
     */
    data object Loading : ExportState()

    /**
     * Export completed successfully with a generated file.
     *
     * @property file The exported file (CSV or PDF)
     */
    data class Success(val file: File) : ExportState()

    /**
     * Export failed with an error.
     *
     * @property message The error message to display to the user
     */
    data class Error(val message: String) : ExportState()
}

/**
 * Export event for one-shot actions (e.g., triggering share intent).
 *
 * This sealed class represents events that should be handled once by the UI,
 * such as launching the share sheet after a successful export.
 */
sealed class ExportEvent {
    /**
     * Event to trigger sharing of the exported file.
     *
     * @property file The exported file to share
     * @property mimeType The MIME type of the file (text/csv or application/pdf)
     */
    data class ShareFile(val file: File, val mimeType: String) : ExportEvent()
}
