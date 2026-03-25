package com.decathlon.smartnutristock.di

import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.data.repository.ProductRepositoryImpl
import com.decathlon.smartnutristock.domain.repository.StockRepository
import com.decathlon.smartnutristock.data.repository.StockRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings.
 * Binds repository interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Bind ProductRepository interface to ProductRepositoryImpl.
     * This allows Hilt to inject ProductRepository wherever needed.
     */
    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: ProductRepositoryImpl
    ): ProductRepository

    /**
     * Bind StockRepository interface to StockRepositoryImpl.
     */
    @Binds
    @Singleton
    abstract fun bindStockRepository(
        impl: StockRepositoryImpl
    ): StockRepository
}
