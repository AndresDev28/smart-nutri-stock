package com.decathlon.smartnutristock.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.domain.usecase.GetSemaphoreCountersUseCase
import com.decathlon.smartnutristock.domain.usecase.SemaphoreCounters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 *
 * Responsibilities:
 * - Observes semaphore counters from GetSemaphoreCountersUseCase
 * - Exposes reactive state via StateFlow
 * - Handles loading and error states
 *
 * Theme Colors (from CalculateStatusUseCase):
 * - 🔴 Red: #FF4444 (expired, days ≤ 0)
 * - 🟡 Yellow: #FFC107 (warning, 1-7 days)
 * - 🟢 Green: #4CAF50 (safe, 8+ days)
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSemaphoreCountersUseCase: GetSemaphoreCountersUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<DashboardUiState>(
        value = DashboardUiState.Loading
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Load semaphore counters on initialization
        loadSemaphoreCounters()
    }

    /**
     * Load semaphore counters from UseCase.
     */
    private fun loadSemaphoreCounters() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            try {
                getSemaphoreCountersUseCase().collect { counters ->
                    _uiState.value = DashboardUiState.Success(counters)
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Error al cargar contadores")
            }
        }
    }

    /**
     * Reload semaphore counters (user-triggered refresh).
     */
    fun refresh() {
        loadSemaphoreCounters()
    }
}

/**
 * UI State for Dashboard screen.
 * Sealed class enables exhaustive `when()` expressions in Compose UI.
 */
sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(val counters: SemaphoreCounters) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
