package com.decathlon.smartnutristock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.local.InstantConverters

@Database(
    entities = [ProductCatalogEntity::class, ActiveStockEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(InstantConverters::class, WorkflowActionConverter::class)
abstract class SmartNutriStockDatabase : RoomDatabase() {
    abstract fun productCatalogDao(): ProductCatalogDao
    abstract fun stockDao(): StockDao

    companion object {
        const val DATABASE_NAME = "smart_nutri_stock.db"
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE product_catalog ADD COLUMN daysUntilExpiry INTEGER NOT NULL DEFAULT 0"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS active_stocks (
                id TEXT PRIMARY KEY NOT NULL,
                ean TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                expiryDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_active_stocks_ean_expiryDate
            ON active_stocks(ean, expiryDate)
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add deletedAt column for soft-delete support (nullable)
        database.execSQL(
            "ALTER TABLE active_stocks ADD COLUMN deletedAt INTEGER"
        )

        // Create index on deletedAt for query performance
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_active_stocks_deletedAt ON active_stocks(deletedAt)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add actionTaken column for workflow action tracking
        database.execSQL(
            "ALTER TABLE active_stocks ADD COLUMN actionTaken TEXT NOT NULL DEFAULT 'PENDING'"
        )
    }
}
