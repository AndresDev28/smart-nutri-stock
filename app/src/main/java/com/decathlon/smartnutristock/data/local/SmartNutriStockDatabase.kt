package com.decathlon.smartnutristock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity

@Database(
    entities = [ProductCatalogEntity::class],
    version = 1,
    exportSchema = true  // Export schema to JSON for versioning
)
abstract class SmartNutriStockDatabase : RoomDatabase() {
    abstract fun productCatalogDao(): ProductCatalogDao

    companion object {
        const val DATABASE_NAME = "smart_nutri_stock.db"
    }
}
