package com.decathlon.smartnutristock.data.local.encrypted

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.gotrue.SessionManager as SupabaseSessionManager
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Session manager using EncryptedSharedPreferences for secure token storage.
 *
 * This class implements Supabase's SessionManager interface, allowing Supabase
 * to automatically save and restore sessions using our secure EncryptedSharedPreferences.
 *
 * The encryption is hardware-backed on most devices, providing strong security
 * for sensitive authentication tokens.
 *
 * @property context Application context for creating SharedPreferences
 */
class EncryptedSessionManager(context: Context) : SupabaseSessionManager {

    companion object {
        private const val PREFS_NAME = "secure_session_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_STORE_ID = "store_id"
        private const val KEY_USER_SESSION = "user_session"
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ============================================================================
    // Supabase SessionManager Interface Implementation
    // ============================================================================

    /**
     * Save session using Supabase's SessionManager interface.
     * Called automatically by Supabase when the session changes.
     *
     * @param session UserSession from Supabase containing tokens and user info
     */
    override suspend fun saveSession(session: UserSession) {
        encryptedPrefs.edit().apply {
            // Store the complete UserSession as JSON for Supabase auto-restore
            putString(KEY_USER_SESSION, json.encodeToString(session))

            // Also store individual fields for backward compatibility
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putLong(KEY_TOKEN_EXPIRY, session.expiresAt.toEpochMilliseconds())

            // Extract user metadata
            session.user?.let { user ->
                putString(KEY_USER_ID, user.id)
                val storeId = user.userMetadata?.get("store_id")?.jsonPrimitive?.content
                if (storeId != null) {
                    putString(KEY_STORE_ID, storeId)
                }
            }
        }.apply()
    }

    /**
     * Load session from storage.
     * Called automatically by Supabase on initialization.
     *
     * @return UserSession if exists, null otherwise
     */
    override suspend fun loadSession(): UserSession? {
        val sessionJson = encryptedPrefs.getString(KEY_USER_SESSION, null)
        return if (sessionJson != null) {
            try {
                json.decodeFromString<UserSession>(sessionJson)
            } catch (e: Exception) {
                // If JSON deserialization fails, return null
                null
            }
        } else {
            null
        }
    }

    /**
     * Delete session from storage.
     * Called automatically by Supabase on logout.
     */
    override suspend fun deleteSession() {
        encryptedPrefs.edit().clear().apply()
    }

    // ============================================================================
    // Legacy methods for backward compatibility
    // ============================================================================

    /**
     * Save session tokens and user information (legacy method).
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param tokenExpiry Epoch timestamp when access token expires
     * @param userId User ID from Supabase auth
     * @param storeId Store ID for multitenancy
     *
     * @deprecated Supabase now handles session persistence via saveSession(UserSession)
     */
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        tokenExpiry: Long,
        userId: String,
        storeId: String
    ) {
        encryptedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRY, tokenExpiry)
            putString(KEY_USER_ID, userId)
            putString(KEY_STORE_ID, storeId)
        }.apply()
    }

    /**
     * Get the stored access token.
     *
     * @return Access token if exists, null otherwise
     */
    fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Get the stored refresh token.
     *
     * @return Refresh token if exists, null otherwise
     */
    fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Get the access token expiry timestamp.
     *
     * @return Epoch timestamp of token expiry if exists, null otherwise
     */
    fun getTokenExpiry(): Long? {
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, -1)
        return if (expiry != -1L) expiry else null
    }

    /**
     * Get the stored user ID.
     *
     * @return User ID if exists, null otherwise
     */
    fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    /**
     * Get the stored store ID.
     *
     * @return Store ID if exists, null otherwise
     */
    fun getStoreId(): String? {
        return encryptedPrefs.getString(KEY_STORE_ID, null)?.trim('"')
    }

    /**
     * Check if user is logged in (has valid tokens).
     *
     * Note: This checks for token existence, not validity.
     * Token expiry should be checked separately for validation.
     *
     * @return true if access token exists, false otherwise
     */
    fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    /**
     * Check if access token is expired.
     *
     * @return true if token is expired or doesn't exist, false otherwise
     */
    fun isTokenExpired(): Boolean {
        val expiry = getTokenExpiry() ?: return true
        return System.currentTimeMillis() >= expiry
    }

    /**
     * Clear all session data.
     *
     * Removes all stored tokens and user information.
     * Should be called on logout.
     */
    fun clearSession() {
        encryptedPrefs.edit().clear().apply()
    }
}
