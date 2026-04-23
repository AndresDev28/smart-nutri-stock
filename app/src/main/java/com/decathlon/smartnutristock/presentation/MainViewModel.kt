package com.decathlon.smartnutristock.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.domain.model.AuthState
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for managing application-wide state.
 *
 * Responsibilities:
 * - Observes authentication state from AuthRepository
 * - Exposes auth state as StateFlow for navigation guard
 * - Provides reactive auth state updates to MainActivity
 *
 * This ViewModel is used by MainActivity to implement Auth Guard:
 * - Authenticated → allow navigation to Dashboard, Scanner, History
 * - NotAuthenticated → redirect to LoginScreen
 * - Loading → show loading indicator
 *
 * @property authRepository Repository for authentication operations
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Private mutable state flow - MUST be initialized before init blocks
    private val _authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.Loading)

    // Auth state exposed as StateFlow
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        // Observe auth state from AuthRepository
        viewModelScope.launch {
            authRepository.observeAuthState().collect { state ->
                _authState.value = state
            }
        }
    }
}
