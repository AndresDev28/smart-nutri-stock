package com.decathlon.smartnutristock.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Migration test for MIGRATION_3_4: Add deletedAt column and index.
 *
 * Verifies:
 * 1. Migration succeeds without data loss
 * 2. deletedAt column exists and is nullable
 * 3. Index on deletedAt column exists
 * 4. Existing data has deletedAt = NULL by default
 */
@RunWith(AndroidJUnit4::class)
class Migration3To4Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmartNutriStockDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test that MIGRATION_3_4 adds deletedAt column correctly.
     */
    @Test
    fun migrate_3_to_4_adds_deletedAt_column() {
        // Given - create database at version 3
        var db = helper.createDatabase(TEST_DB, 3).apply {
            // Insert test data at version 3
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()})
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 4
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // Then - verify column exists
        val cursor = db.query("PRAGMA table_info(active_stocks)")
        var hasDeletedAtColumn = false
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(1)
            if (columnName == "deletedAt") {
                hasDeletedAtColumn = true
                break
            }
        }
        cursor.close()

        assertThat(hasDeletedAtColumn).isTrue()

        // Verify index exists
        val indexCursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='index_active_stocks_deletedAt'"
        )
        assertThat(indexCursor.count).isEqualTo(1)
        indexCursor.close()

        // Verify existing data is preserved
        val dataCursor = db.query("SELECT * FROM active_stocks")
        assertThat(dataCursor.count).isEqualTo(1)
        dataCursor.moveToFirst()
        assertThat(dataCursor.getString(dataCursor.getColumnIndex("id"))).isEqualTo("batch-1")
        assertThat(dataCursor.getString(dataCursor.getColumnIndex("ean"))).isEqualTo("8435408475366")
        assertThat(dataCursor.getInt(dataCursor.getColumnIndex("quantity"))).isEqualTo(10)
        dataCursor.close()

        db.close()
    }

    /**
     * Test that deletedAt column is nullable and defaults to NULL for existing data.
     */
    @Test
    fun deletedAt_column_is_nullable_and_defaults_to_NULL() {
        // Given - create database at version 3 with existing data
        var db = helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()})
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 4
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        // Then - verify deletedAt is NULL for existing data
        val cursor = db.query("SELECT deletedAt FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val deletedAt = cursor.isNull(cursor.getColumnIndex("deletedAt"))
        cursor.close()

        assertThat(deletedAt).isTrue()

        db.close()
    }

    /**
     * Test that we can insert and query soft-deleted batches after migration.
     */
    @Test
    fun can_insert_and_query_soft_deleted_batches_after_migration() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 3).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        val now = Instant.now().toEpochMilli()

        // When - insert a soft-deleted batch
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt)
            VALUES ('batch-1', '8435408475366', 10, $now, $now, $now, $now)
            """.trimIndent()
        )

        // Then - verify we can query it
        val cursor = db.query("SELECT deletedAt FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val deletedAt = cursor.getLong(cursor.getColumnIndex("deletedAt"))
        cursor.close()

        assertThat(deletedAt).isEqualTo(now)

        db.close()
    }

    /**
     * Test that queries can filter by deletedAt IS NULL after migration.
     */
    @Test
    fun can_filter_by_deletedAt_IS_NULL_after_migration() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 3).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        val now = Instant.now().toEpochMilli()

        // When - insert one active and one soft-deleted batch
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt)
            VALUES ('batch-1', '8435408475366', 10, $now, $now, $now, NULL)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt)
            VALUES ('batch-2', '8435408475367', 20, $now, $now, $now, $now)
            """.trimIndent()
        )

        // Then - verify query filters correctly
        val cursor = db.query("SELECT COUNT(*) FROM active_stocks WHERE deletedAt IS NULL")
        cursor.moveToFirst()
        val activeCount = cursor.getInt(0)
        cursor.close()

        assertThat(activeCount).isEqualTo(1)

        db.close()
    }
}
