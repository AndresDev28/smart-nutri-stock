package com.decathlon.smartnutristock.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.domain.usecase.GetSemaphoreCountersUseCase
import com.decathlon.smartnutristock.domain.usecase.GetAllBatchesUseCase
import com.decathlon.smartnutristock.domain.usecase.LogoutUseCase
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.Batch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * ViewModel for Dashboard screen.
 *
 * Responsibilities:
 * - Observes semaphore counters from GetSemaphoreCountersUseCase
 * - Observes product batches from GetAllBatchesUseCase
 * - Observes current user from AuthRepository for dynamic greeting
 * - Exposes reactive state via StateFlow
 * - Handles loading and error states
 *
 * SSOT: All status logic is calculated by CalculateStatusUseCase in domain layer.
 * UI renders state, never calculates status.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getSemaphoreCountersUseCase: GetSemaphoreCountersUseCase,
    private val getAllBatchesUseCase: GetAllBatchesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<DashboardUiState>(
        value = DashboardUiState.Loading
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // User email for dynamic greeting
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    // One-time event for logout navigation
    private val _logoutEvent = MutableStateFlow<Boolean>(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent.asStateFlow()

    init {
        // Load semaphore counters and batches on initialization
        loadDashboardData()
        // Load current user email for dynamic greeting
        loadUserEmail()
    }

    /**
     * Load current user email for dynamic greeting.
     */
    private fun loadUserEmail() {
        viewModelScope.launch {
            val user = authRepository.getCurrentSession()
            _userEmail.value = user?.email
        }
    }

    /**
     * Load semaphore counters and product batches from UseCases.
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            try {
                combine(
                    getSemaphoreCountersUseCase(),
                    getAllBatchesUseCase()
                ) { counters, batches ->
                    Pair(counters, batches)
                }.collect { (counters, batches) ->
                    _uiState.value = DashboardUiState.Success(
                        counters = counters,
                        batches = batches
                    )
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Error al cargar datos")
            }
        }
    }

    /**
     * Reload semaphore counters and batches (user-triggered refresh).
     */
    fun refresh() {
        loadDashboardData()
    }

    /**
     * Logout user.
     *
     * Calls LogoutUseCase which clears the session and updates auth state.
     * Triggers logout event for navigation to login screen.
     */
    fun logout() {
        viewModelScope.launch {
            val result = logoutUseCase.logout()
            if (result.isSuccess) {
                // Trigger logout navigation event
                _logoutEvent.value = true
            }
        }
    }

    /**
     * Clear logout event after navigation is handled.
     * This prevents re-navigation on recomposition.
     */
    fun clearLogoutEvent() {
        _logoutEvent.value = false
    }
}

/**
 * UI State for Dashboard screen.
 * Sealed class enables exhaustive `when()` expressions in Compose UI.
 */
sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(
        val counters: SemaphoreCounters,
        val batches: List<Batch>
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
