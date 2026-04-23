package com.decathlon.smartnutristock.di

import android.content.Context
import com.decathlon.smartnutristock.BuildConfig
import com.decathlon.smartnutristock.data.local.encrypted.EncryptedSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.createSupabaseClient
import javax.inject.Singleton

/**
 * Hilt module for authentication and network dependencies.
 *
 * Provides:
 * - EncryptedSessionManager (EncryptedSharedPreferences wrapper)
 * - SupabaseClient singleton
 * - Supabase Auth (GoTrue) and Postgrest clients
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * Provide EncryptedSessionManager for secure token storage.
     *
     * Uses EncryptedSharedPreferences backed by Android Keystore.
     */
    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): EncryptedSessionManager {
        return EncryptedSessionManager(context)
    }

    /**
     * Provide Supabase client singleton.
     *
     * Configured with Auth (GoTrue) and Postgrest plugins.
     * Reads credentials from BuildConfig (injected from local.properties).
     *
     * Uses custom EncryptedSessionManager (EncryptedSharedPreferences) for session persistence.
     * Supabase will automatically restore sessions on app startup.
     *
     * @param sessionManager Custom EncryptedSessionManager implementing Supabase's interface
     * @return Configured SupabaseClient instance
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(sessionManager: EncryptedSessionManager): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            // Install Auth (GoTrue) plugin for authentication
            install(Auth) {
                // Configure custom SessionManager for secure token storage
                this.sessionManager = sessionManager

                // Enable automatic session restoration on app startup (default: true)
                autoLoadFromStorage = true

                // Enable automatic session saving when session changes (default: true)
                autoSaveToStorage = true
            }

            // Install Postgrest plugin for database operations
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun providePostgrest(supabaseClient: SupabaseClient): Postgrest {
        return supabaseClient.postgrest
    }
}
