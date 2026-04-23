package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.local.encrypted.EncryptedSessionManager
import com.decathlon.smartnutristock.domain.model.AuthState
import com.decathlon.smartnutristock.domain.model.User
import com.decathlon.smartnutristock.domain.model.UserRole
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Supabase Auth and EncryptedSharedPreferences.
 *
 * This repository:
 * - Authenticates users via Supabase GoTrue
 * - Delegates session persistence to Supabase's SessionManager (our custom implementation)
 * - Maps Supabase user to domain User model
 * - Provides reactive auth state via Flow
 *
 * Session persistence is now handled automatically by Supabase through our custom SessionManager.
 * When the app starts, Supabase automatically loads the session from EncryptedSharedPreferences.
 *
 * @property supabaseClient Supabase client with Auth plugin installed
 * @property sessionManager EncryptedSessionManager wrapper for token storage (implements Supabase's SessionManager)
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: EncryptedSessionManager
) : AuthRepository {

    // Coroutine scope for this repository
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Private state flow for auth state changes
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: Flow<AuthState> = _authState.asStateFlow()

    init {
        // Observe Supabase's session status and update our auth state accordingly
        // This collection runs for the lifetime of the repository (which is @Singleton)
        repositoryScope.launch {
            supabaseClient.auth.sessionStatus.collect { sessionStatus ->
                when (sessionStatus) {
                    is SessionStatus.Authenticated -> {
                        val user = sessionStatus.session.user
                        if (user != null) {
                            val domainUser = mapToDomainUser(user)
                            _authState.value = AuthState.Authenticated(domainUser)
                        } else {
                            _authState.value = AuthState.NotAuthenticated
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _authState.value = AuthState.NotAuthenticated
                    }
                    is SessionStatus.LoadingFromStorage -> {
                        _authState.value = AuthState.Loading
                    }
                    is SessionStatus.NetworkError -> {
                        _authState.value = AuthState.Error("Network error during session refresh")
                    }
                }
            }
        }
    }

    /**
     * Authenticate user with email and password.
     *
     * On success:
     * 1. Authenticates with Supabase Auth
     * 2. Supabase automatically saves the session via our custom SessionManager
     * 3. Maps Supabase user to domain User
     *
     * Note: Session persistence is now handled automatically by Supabase.
     * The auth state will be updated via the sessionStatus observer in init.
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing User on success, or error on failure
     */
    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            // Authenticate with Supabase Auth
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Get the current session after login
            val session = supabaseClient.auth.currentSessionOrNull() ?: throw IllegalStateException("Session not found")

            // Extract user and metadata
            val user = session.user ?: throw IllegalStateException("User not found in session")

            // Map Supabase user to domain User
            val domainUser = mapToDomainUser(user)

            // Note: authState will be updated automatically via sessionStatus observer
            // Supabase automatically saves the session to SessionManager

            Result.success(domainUser)
        } catch (e: Exception) {
            // Update auth state with error
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }

    /**
     * Log out the current user.
     *
     * Clears Supabase session. SessionManager is automatically cleared by Supabase.
     *
     * @return Result indicating success or failure
     */
    override suspend fun logout(): Result<Unit> {
        return try {
            // Sign out from Supabase
            // This will automatically call SessionManager.deleteSession()
            supabaseClient.auth.signOut()

            // Note: authState will be updated automatically via sessionStatus observer
            // Supabase automatically clears the session from SessionManager

            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Logout failed")
            Result.failure(e)
        }
    }

    /**
     * Get the current authenticated user.
     *
     * Returns the user from Supabase's current session.
     *
     * @return User if authenticated, null otherwise
     */
    override suspend fun getCurrentSession(): User? {
        return try {
            val user = supabaseClient.auth.currentUserOrNull() ?: return null
            mapToDomainUser(user)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Refresh the access token using the refresh token.
     *
     * Called when access token expires or is invalid.
     * Supabase automatically updates the session in SessionManager.
     *
     * @return Result containing User on success, or error on failure
     */
    override suspend fun refreshToken(): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            // Refresh current session with Supabase
            // This will automatically update the session in SessionManager
            supabaseClient.auth.refreshCurrentSession()

            // Get the updated session
            val session = supabaseClient.auth.currentSessionOrNull() ?: throw IllegalStateException("Session not found after refresh")

            // Extract user and metadata
            val user = session.user ?: throw IllegalStateException("User not found in session")

            // Map Supabase user to domain User
            val domainUser = mapToDomainUser(user)

            // Note: authState will be updated automatically via sessionStatus observer
            // Supabase automatically saves the refreshed session to SessionManager

            Result.success(domainUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Token refresh failed")
            Result.failure(e)
        }
    }

    /**
     * Observe authentication state as a Flow.
     *
     * Emits auth state changes in real-time.
     * UI components should collect this flow to react to auth changes.
     *
     * @return Flow of AuthState
     */
    override fun observeAuthState(): Flow<AuthState> {
        return _authState.asStateFlow()
    }

    /**
     * Restore session from persistent storage.
     *
     * With Supabase SessionManager, Supabase automatically loads the session on
     * initialization (via autoLoadFromStorage = true). This method can be used to
     * manually trigger a session restore after network errors.
     *
     * The method checks if Supabase has a current session and returns the user.
     *
     * @return Result containing User on success, or error on failure
     */
    override suspend fun restoreSession(): Result<User> {
        return try {
            _authState.value = AuthState.Loading

            // Check if Supabase has a current session
            // Supabase automatically loads the session from SessionManager on initialization
            val session = supabaseClient.auth.currentSessionOrNull()
                ?: return Result.failure(Exception("No session found"))

            val user = session.user
                ?: return Result.failure(Exception("User not found in session"))

            // Map Supabase user to domain User
            val domainUser = mapToDomainUser(user)

            // Note: authState will be updated automatically via sessionStatus observer

            Result.success(domainUser)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.message ?: "Session restore failed")
            Result.failure(e)
        }
    }

    /**
     * Map Supabase UserInfo to domain User model.
     *
     * Extracts user metadata (store_id, device_id, username, role) and maps
     * Supabase user fields to domain model.
     *
     * @param userInfo Supabase UserInfo object
     * @return Domain User model
     */
    private fun mapToDomainUser(userInfo: io.github.jan.supabase.gotrue.user.UserInfo): User {
        // Extract storeId from user metadata (default to "1620" if not present)
        val storeId = userInfo.userMetadata?.get("store_id")?.jsonPrimitive?.content ?: "1620"

        val deviceId = userInfo.userMetadata?.get("device_id")?.jsonPrimitive?.content

        val username = userInfo.userMetadata?.get("username")?.jsonPrimitive?.content

        val roleStr = userInfo.userMetadata?.get("role")?.jsonPrimitive?.content?.uppercase() ?: "STAFF"
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: IllegalArgumentException) {
            UserRole.STAFF // Default to STAFF if invalid role
        }

        return User(
            id = userInfo.id,
            email = userInfo.email ?: throw IllegalStateException("User email not found"),
            username = username,
            storeId = storeId,
            role = role,
            deviceId = deviceId
        )
    }
}
