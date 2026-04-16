package com.decathlon.smartnutristock.di

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for WorkManager configuration.
 *
 * This module provides the WorkManager configuration with Hilt Worker factory support,
 * enabling dependency injection for workers like StatusCheckWorker.
 *
 * IMPORTANT: The Application class MUST implement Configuration.Provider and
 * use the configuration provided by this module.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    /**
     * Provides the WorkManager configuration with Hilt Worker factory.
     *
     * This configuration enables Hilt to inject dependencies into workers
     * annotated with @HiltWorker.
     *
     * @param workerFactory The Hilt worker factory for dependency injection
     * @return WorkManager configuration
     */
    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(
        workerFactory: HiltWorkerFactory
    ): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    /**
     * Provides the WorkManager instance.
     *
     * Note: This is provided for convenience, but typically WorkManager is accessed
     * via WorkManager.getInstance(context) directly in the Application class.
     *
     * @param context Application context
     * @return WorkManager instance
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
        configuration: Configuration
    ): WorkManager {
        // WorkManager should be initialized in the Application class
        // This provider is available if direct injection is needed
        return WorkManager.getInstance(context)
    }
}
