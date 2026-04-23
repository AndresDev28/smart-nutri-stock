package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case for user logout.
 *
 * This use case handles logout by:
 * - Calling AuthRepository.logout() to clear session
 * - Cleaning up any local session data
 *
 * @property authRepository Repository for authentication operations
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {

    /**
     * Execute logout.
     *
     * Calls AuthRepository.logout() which:
     * - Signs out from Supabase
     * - Clears session tokens from SessionManager
     * - Updates auth state to NotAuthenticated
     *
     * @return Result indicating success or failure
     */
    suspend fun logout(): Result<Unit> {
        return authRepository.logout()
    }
}
