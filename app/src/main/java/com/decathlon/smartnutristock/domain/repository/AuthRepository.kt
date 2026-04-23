package com.decathlon.smartnutristock.domain.repository

import com.decathlon.smartnutristock.domain.model.AuthState
import com.decathlon.smartnutristock.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 *
 * This interface defines the contract for authentication-related operations
 * including login, logout, session management, and token refresh.
 *
 * Implementations should use Supabase Auth for authentication and
 * EncryptedSharedPreferences for session persistence.
 */
interface AuthRepository {

    /**
     * Authenticate user with email and password.
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing User on success, or error on failure
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Log out the current user.
     *
     * Clears session tokens and updates auth state.
     *
     * @return Result indicating success or failure
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get the current authenticated user.
     *
     * @return User if authenticated, null otherwise
     */
    suspend fun getCurrentSession(): User?

    /**
     * Refresh the access token using the refresh token.
     *
     * Called when access token expires or is invalid.
     *
     * @return Result containing User on success, or error on failure
     */
    suspend fun refreshToken(): Result<User>

    /**
     * Observe authentication state as a Flow.
     *
     * Emits auth state changes in real-time.
     * UI components should collect this flow to react to auth changes.
     *
     * @return Flow of AuthState
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Restore session from persistent storage.
     *
     * With Supabase SessionManager, this is typically not needed as Supabase
     * automatically loads the session on initialization. However, this method
     * can be used to manually trigger a session restore after network errors.
     *
     * @return Result containing User on success, or error on failure
     */
    suspend fun restoreSession(): Result<User>
}
