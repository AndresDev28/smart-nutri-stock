package com.decathlon.smartnutristock.di

import android.content.Context
import androidx.room.Room
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.local.SmartNutriStockDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database and DAO bindings.
 * Provides Room database and DAO instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provide Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SmartNutriStockDatabase {
        return Room.databaseBuilder(
            context,
            SmartNutriStockDatabase::class.java,
            "smart_nutri_stock.db"
        ).build()
    }

    /**
     * Provide ProductCatalogDao from database.
     */
    @Provides
    @Singleton
    fun provideProductDao(
        database: SmartNutriStockDatabase
    ): ProductCatalogDao {
        return database.productCatalogDao()
    }
}
