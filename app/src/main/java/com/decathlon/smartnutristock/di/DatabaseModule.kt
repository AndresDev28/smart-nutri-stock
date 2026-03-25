package com.decathlon.smartnutristock.di

import android.content.Context
import androidx.room.Room
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.local.MIGRATION_1_2
import com.decathlon.smartnutristock.data.local.MIGRATION_2_3
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
     *
     * Database is configured with migrations:
     * - MIGRATION_1_2: Add daysUntilExpiry column
     * - MIGRATION_2_3: Create active_stocks table with composite index
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SmartNutriStockDatabase {
        return Room.databaseBuilder(
            context,
            SmartNutriStockDatabase::class.java,
            SmartNutriStockDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
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

    /**
     * Provide StockDao from database.
     */
    @Provides
    @Singleton
    fun provideStockDao(
        database: SmartNutriStockDatabase
    ): StockDao {
        return database.stockDao()
    }
}
