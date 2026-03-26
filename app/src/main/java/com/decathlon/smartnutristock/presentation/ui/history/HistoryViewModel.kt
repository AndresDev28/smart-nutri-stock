package com.decathlon.smartnutristock.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.usecase.GetAllBatchesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for History screen.
 *
 * Responsibilities:
 * - Loads all batches from GetAllBatchesUseCase ONCE
 * - Uses Batch model which already has dynamically calculated status
 * - Exposes reactive state via StateFlow
 * - Handles loading and error states
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllBatchesUseCase: GetAllBatchesUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<HistoryUiState>(
        value = HistoryUiState.Loading
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

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
