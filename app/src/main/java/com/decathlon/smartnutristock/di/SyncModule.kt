package com.decathlon.smartnutristock.di

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
 * Binds SyncRepository interface to its implementation.
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

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
