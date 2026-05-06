package com.decathlon.smartnutristock.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.data.local.encrypted.EncryptedSessionManager
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import com.decathlon.smartnutristock.domain.usecase.ClaimOrphanRecordsUseCase
import com.decathlon.smartnutristock.domain.usecase.TriggerSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: EncryptedSessionManager,
    private val claimOrphanRecordsUseCase: ClaimOrphanRecordsUseCase,
    private val triggerSyncUseCase: TriggerSyncUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onLoginClick() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.login(_email.value, _password.value)
            result.fold(
                onSuccess = {
                    claimOrphanRecords()
                    triggerImmediateSync()
                    _uiState.value = LoginUiState.Success
                },
                onFailure = { _uiState.value = LoginUiState.Error(it.message ?: "Login failed") }
            )
        }
    }

    private suspend fun claimOrphanRecords() {
        val userId = sessionManager.getUserId()
        val storeId = sessionManager.getStoreId() ?: "1620"
        if (userId != null) {
            try {
                val result = claimOrphanRecordsUseCase(userId, storeId)
                if (result.isSuccess) {
                    Timber.i("Orphan cleanup: ${result.getOrNull()} records claimed after login")
                }
            } catch (e: Exception) {
                Timber.e(e, "Orphan cleanup: Failed after login")
            }
        }
    }

    private suspend fun triggerImmediateSync() {
        val storeId = sessionManager.getStoreId() ?: "1620"
        try {
            triggerSyncUseCase(storeId)
        } catch (e: Exception) {
            Timber.e(e, "Immediate sync: Failed to trigger after login")
        }
    }
}

sealed class LoginUiState {
    object Initial : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
