package com.decathlon.smartnutristock.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.usecase.GetAllBatchesUseCase
import com.decathlon.smartnutristock.domain.usecase.RestoreBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.SoftDeleteBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateProductNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllBatchesUseCase: GetAllBatchesUseCase,
    private val softDeleteBatchUseCase: SoftDeleteBatchUseCase,
    private val restoreBatchUseCase: RestoreBatchUseCase,
    private val updateBatchUseCase: UpdateBatchUseCase,
    private val updateProductNameUseCase: UpdateProductNameUseCase
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
     */
    private fun loadBatchesOnce() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            try {
                // Use first() from flow to get batches once, not collect
                val batches = getAllBatchesUseCase().first()

                _uiState.value = HistoryUiState.Success(batches)
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
