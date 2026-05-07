package com.decathlon.smartnutristock.di

import com.decathlon.smartnutristock.data.remote.SupabaseSyncRemoteDataSource
import com.decathlon.smartnutristock.data.remote.SyncRemoteDataSource
import com.decathlon.smartnutristock.data.repository.SyncRepositoryImpl
import com.decathlon.smartnutristock.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Hilt module for synchronization dependencies.
 *
 * Binds SyncRepository and SyncRemoteDataSource interfaces
 * to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    /**
     * Bind SyncRepository interface to SyncRepositoryImpl.
     * This allows Hilt to inject SyncRepository wherever needed.
     */
    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository

    /**
     * Bind SyncRemoteDataSource interface to SupabaseSyncRemoteDataSource.
     * Decouples the repository from the Supabase SDK for testability.
     */
    @Binds
    @Singleton
    abstract fun bindSyncRemoteDataSource(
        impl: SupabaseSyncRemoteDataSource
    ): SyncRemoteDataSource

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
