package com.decathlon.smartnutristock.domain.model

import com.decathlon.smartnutristock.domain.model.User

/**
 * Authentication state sealed class.
 *
 * Represents the current authentication status of the application.
 * Used by ViewModels and UI to determine navigation and UI state.
 *
 * Pure Kotlin sealed class - NO Room or Supabase annotations.
 */
sealed class AuthState {
    /**
     * User is not authenticated.
     */
    object NotAuthenticated : AuthState()

    /**
     * User is authenticated with valid session.
     *
     * @property user The authenticated user
     */
    data class Authenticated(val user: User) : AuthState()

    /**
     * Authentication error occurred.
     *
     * @property message Error message describing what went wrong
     */
    data class Error(val message: String) : AuthState()

    /**
     * Authentication operation is in progress.
     */
    object Loading : AuthState()
}
