package com.decathlon.smartnutristock.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.dao.UserDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.entity.UserEntity
import com.decathlon.smartnutristock.data.local.InstantConverters

@Database(
    entities = [UserEntity::class, ProductCatalogEntity::class, ActiveStockEntity::class],
    version = 6,
    exportSchema = true
)
@TypeConverters(InstantConverters::class, WorkflowActionConverter::class)
abstract class SmartNutriStockDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
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

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create users table
        database.execSQL(
            """
            CREATE TABLE users (
                id TEXT PRIMARY KEY NOT NULL,
                email TEXT NOT NULL,
                storeId TEXT NOT NULL DEFAULT '1620',
                role TEXT NOT NULL DEFAULT 'STAFF',
                deviceId TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS index_users_email ON users(email)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_users_storeId ON users(storeId)")

        // Add sync columns to active_stocks with EXACT defaults as specified by Lead Architect
        database.execSQL("ALTER TABLE active_stocks ADD COLUMN userId TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE active_stocks ADD COLUMN storeId TEXT NOT NULL DEFAULT '1620'")
        database.execSQL("ALTER TABLE active_stocks ADD COLUMN syncedAt INTEGER DEFAULT NULL")
        database.execSQL("ALTER TABLE active_stocks ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE active_stocks ADD COLUMN deviceId TEXT")
        database.execSQL("ALTER TABLE active_stocks ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 1")

        // Create indexes for sync queries
        database.execSQL("CREATE INDEX IF NOT EXISTS index_active_stocks_user_id ON active_stocks(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_active_stocks_store_id ON active_stocks(storeId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_active_stocks_is_dirty ON active_stocks(isDirty)")
    }
}
