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
 * Migration test for MIGRATION_4_5: Add actionTaken column.
 *
 * Verifies:
 * 1. Migration succeeds without data loss
 * 2. actionTaken column exists with NOT NULL constraint
 * 3. Existing data has actionTaken = "PENDING" by default
 * 4. Can insert and query batches with different action states
 */
@RunWith(AndroidJUnit4::class)
class Migration4To5Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmartNutriStockDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test that MIGRATION_4_5 adds actionTaken column correctly.
     */
    @Test
    fun migrate_4_to_5_adds_actionTaken_column() {
        // Given - create database at version 4
        var db = helper.createDatabase(TEST_DB, 4).apply {
            // Insert test data at version 4
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL)
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // Then - verify column exists
        val cursor = db.query("PRAGMA table_info(active_stocks)")
        var hasActionTakenColumn = false
        var actionTakenNotNull = false
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(1)
            if (columnName == "actionTaken") {
                hasActionTakenColumn = true
                val notNull = cursor.getInt(3) // 3 = notnull column
                actionTakenNotNull = (notNull == 1)
                break
            }
        }
        cursor.close()

        assertThat(hasActionTakenColumn).isTrue()
        assertThat(actionTakenNotNull).isTrue()

        db.close()
    }

    /**
     * Test that existing data has actionTaken = "PENDING" by default.
     */
    @Test
    fun existing_data_has_actionTaken_PENDING_by_default() {
        // Given - create database at version 4 with existing data
        var db = helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL)
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        // Then - verify actionTaken is "PENDING" for existing data
        val cursor = db.query("SELECT actionTaken FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val actionTaken = cursor.getString(cursor.getColumnIndex("actionTaken"))
        cursor.close()

        assertThat(actionTaken).isEqualTo("PENDING")

        db.close()
    }

    /**
     * Test that we can insert and query batches with different action states after migration.
     */
    @Test
    fun can_insert_and_query_batches_with_different_action_states_after_migration() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 4).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        val now = Instant.now().toEpochMilli()

        // When - insert batches with different action states
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
            VALUES ('batch-1', '8435408475366', 10, $now, $now, $now, NULL, 'PENDING')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
            VALUES ('batch-2', '8435408475367', 20, $now, $now, $now, NULL, 'DISCOUNTED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
            VALUES ('batch-3', '8435408475368', 30, $now, $now, $now, NULL, 'REMOVED')
            """.trimIndent()
        )

        // Then - verify we can query all batches with correct action states
        val cursor = db.query("SELECT id, actionTaken FROM active_stocks ORDER BY id")
        assertThat(cursor.count).isEqualTo(3)

        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("batch-1")
        assertThat(cursor.getString(cursor.getColumnIndex("actionTaken"))).isEqualTo("PENDING")

        cursor.moveToNext()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("batch-2")
        assertThat(cursor.getString(cursor.getColumnIndex("actionTaken"))).isEqualTo("DISCOUNTED")

        cursor.moveToNext()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("batch-3")
        assertThat(cursor.getString(cursor.getColumnIndex("actionTaken"))).isEqualTo("REMOVED")

        cursor.close()
        db.close()
    }

    /**
     * Test that we can filter by actionTaken after migration.
     */
    @Test
    fun can_filter_by_actionTaken_after_migration() {
        // Given - migrate database and insert test data
        var db = helper.createDatabase(TEST_DB, 4).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        val now = Instant.now().toEpochMilli()

        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
            VALUES ('batch-1', '8435408475366', 10, $now, $now, $now, NULL, 'PENDING')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
            VALUES ('batch-2', '8435408475367', 20, $now, $now, $now, NULL, 'DISCOUNTED')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
            VALUES ('batch-3', '8435408475368', 30, $now, $now, $now, NULL, 'REMOVED')
            """.trimIndent()
        )

        // When - filter by actionTaken = 'PENDING'
        val cursor = db.query("SELECT COUNT(*) FROM active_stocks WHERE actionTaken = 'PENDING'")
        cursor.moveToFirst()
        val pendingCount = cursor.getInt(0)
        cursor.close()

        // Then - verify only PENDING batches are returned
        assertThat(pendingCount).isEqualTo(1)

        // When - filter by actionTaken != 'PENDING' (with action)
        val cursor2 = db.query("SELECT COUNT(*) FROM active_stocks WHERE actionTaken != 'PENDING'")
        cursor2.moveToFirst()
        val withActionCount = cursor2.getInt(0)
        cursor2.close()

        // Then - verify DISCOUNTED and REMOVED batches are returned
        assertThat(withActionCount).isEqualTo(2)

        db.close()
    }
}
