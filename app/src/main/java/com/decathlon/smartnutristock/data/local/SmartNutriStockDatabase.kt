package com.decathlon.smartnutristock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity

@Database(
    entities = [ProductCatalogEntity::class],
    version = 2,
    exportSchema = true  // Export schema to JSON for versioning
)
abstract class SmartNutriStockDatabase : RoomDatabase() {
    abstract fun productCatalogDao(): ProductCatalogDao

    companion object {
        const val DATABASE_NAME = "smart_nutri_stock.db"
    }
}

/**
 * Migration from version 1 to 2.
 * Adds `daysUntilExpiry` field to product_catalog table.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE product_catalog ADD COLUMN daysUntilExpiry INTEGER NOT NULL DEFAULT 0"
        )
    }
}
