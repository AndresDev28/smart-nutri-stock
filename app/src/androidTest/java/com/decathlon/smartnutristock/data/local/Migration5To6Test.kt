package com.decathlon.smartnutristock.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.dao.UserDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.UserEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Migration test for MIGRATION_5_6: Add users table and sync columns to active_stocks.
 *
 * Verifies:
 * 1. Migration succeeds without data loss
 * 2. users table is created with correct schema
 * 3. New columns are added to active_stocks with correct defaults:
 *    - storeId defaults to "1620"
 *    - isDirty defaults to 1
 *    - version defaults to 1
 *    - userId defaults to null
 *    - syncedAt defaults to null
 *    - deviceId defaults to null
 * 4. Indexes are created for sync queries
 */
@RunWith(AndroidJUnit4::class)
class Migration5To6Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmartNutriStockDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Test that MIGRATION_5_6 creates users table.
     */
    @Test
    fun migrate_5_to_6_creates_users_table() {
        // Given - create database at version 5
        var db = helper.createDatabase(TEST_DB, 5).apply {
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify users table exists
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
        cursor.moveToFirst()
        val tableName = cursor.getString(0)
        cursor.close()

        assertThat(tableName).isEqualTo("users")

        db.close()
    }

    /**
     * Test that users table has correct schema.
     */
    @Test
    fun users_table_has_correct_schema() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify users table schema
        val cursor = db.query("PRAGMA table_info(users)")
        val columns = mutableMapOf<String, Boolean>()
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(1)
            val notNull = cursor.getInt(3) // 3 = notnull column
            val defaultValue = cursor.getString(4)
            columns[columnName] = (notNull == 1)
        }
        cursor.close()

        // Verify required columns exist
        assertThat(columns.containsKey("id")).isTrue()
        assertThat(columns.containsKey("email")).isTrue()
        assertThat(columns.containsKey("storeId")).isTrue()
        assertThat(columns.containsKey("role")).isTrue()
        assertThat(columns.containsKey("deviceId")).isTrue()
        assertThat(columns.containsKey("createdAt")).isTrue()

        // Verify NOT NULL constraints
        assertThat(columns["id"]).isTrue()
        assertThat(columns["email"]).isTrue()
        assertThat(columns["storeId"]).isTrue()
        assertThat(columns["role"]).isTrue()
        assertThat(columns["deviceId"]).isTrue()
        assertThat(columns["createdAt"]).isTrue()

        db.close()
    }

    /**
     * Test that users table has indexes.
     */
    @Test
    fun users_table_has_indexes() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify indexes exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='users'")
        val indexes = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val indexName = cursor.getString(0)
            indexes.add(indexName)
        }
        cursor.close()

        assertThat(indexes).contains("index_users_email")
        assertThat(indexes).contains("index_users_storeId")

        db.close()
    }

    /**
     * Test that active_stocks has new sync columns.
     */
    @Test
    fun active_stocks_has_new_sync_columns() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify new columns exist
        val cursor = db.query("PRAGMA table_info(active_stocks)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(1)
            columns.add(columnName)
        }
        cursor.close()

        assertThat(columns).contains("userId")
        assertThat(columns).contains("storeId")
        assertThat(columns).contains("syncedAt")
        assertThat(columns).contains("version")
        assertThat(columns).contains("deviceId")
        assertThat(columns).contains("isDirty")

        db.close()
    }

    /**
     * Test that storeId defaults to "1620".
     */
    @Test
    fun storeId_defaults_to_1620() {
        // Given - create database at version 5 with existing data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify storeId defaults to "1620"
        val cursor = db.query("SELECT storeId FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val storeId = cursor.getString(cursor.getColumnIndex("storeId"))
        cursor.close()

        assertThat(storeId).isEqualTo("1620")

        db.close()
    }

    /**
     * Test that isDirty defaults to 1.
     */
    @Test
    fun isDirty_defaults_to_1() {
        // Given - create database at version 5 with existing data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify isDirty defaults to 1
        val cursor = db.query("SELECT isDirty FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val isDirty = cursor.getInt(cursor.getColumnIndex("isDirty"))
        cursor.close()

        assertThat(isDirty).isEqualTo(1)

        db.close()
    }

    /**
     * Test that version defaults to 1.
     */
    @Test
    fun version_defaults_to_1() {
        // Given - create database at version 5 with existing data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify version defaults to 1
        val cursor = db.query("SELECT version FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val version = cursor.getInt(cursor.getColumnIndex("version"))
        cursor.close()

        assertThat(version).isEqualTo(1)

        db.close()
    }

    /**
     * Test that userId defaults to null.
     */
    @Test
    fun userId_defaults_to_null() {
        // Given - create database at version 5 with existing data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify userId defaults to null
        val cursor = db.query("SELECT userId FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val userId = cursor.isNull(cursor.getColumnIndex("userId"))
        cursor.close()

        assertThat(userId).isTrue()

        db.close()
    }

    /**
     * Test that syncedAt defaults to null.
     */
    @Test
    fun syncedAt_defaults_to_null() {
        // Given - create database at version 5 with existing data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify syncedAt defaults to null
        val cursor = db.query("SELECT syncedAt FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val syncedAt = cursor.isNull(cursor.getColumnIndex("syncedAt"))
        cursor.close()

        assertThat(syncedAt).isTrue()

        db.close()
    }

    /**
     * Test that deviceId defaults to null.
     */
    @Test
    fun deviceId_defaults_to_null() {
        // Given - create database at version 5 with existing data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify deviceId defaults to null
        val cursor = db.query("SELECT deviceId FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        val deviceId = cursor.isNull(cursor.getColumnIndex("deviceId"))
        cursor.close()

        assertThat(deviceId).isTrue()

        db.close()
    }

    /**
     * Test that existing data is preserved after migration.
     */
    @Test
    fun existing_data_is_preserved_after_migration() {
        // Given - create database at version 5 with test data
        var db = helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-1', '8435408475366', 10, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'PENDING')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken)
                VALUES ('batch-2', '1234567890123', 20, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, ${Instant.now().toEpochMilli()}, NULL, 'DISCOUNTED')
                """.trimIndent()
            )
            close()
        }

        // When - migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify existing data is preserved
        val cursor = db.query("SELECT id, ean, quantity, actionTaken FROM active_stocks ORDER BY id")
        assertThat(cursor.count).isEqualTo(2)

        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("batch-1")
        assertThat(cursor.getString(cursor.getColumnIndex("ean"))).isEqualTo("8435408475366")
        assertThat(cursor.getInt(cursor.getColumnIndex("quantity"))).isEqualTo(10)
        assertThat(cursor.getString(cursor.getColumnIndex("actionTaken"))).isEqualTo("PENDING")

        cursor.moveToNext()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("batch-2")
        assertThat(cursor.getString(cursor.getColumnIndex("ean"))).isEqualTo("1234567890123")
        assertThat(cursor.getInt(cursor.getColumnIndex("quantity"))).isEqualTo(20)
        assertThat(cursor.getString(cursor.getColumnIndex("actionTaken"))).isEqualTo("DISCOUNTED")

        cursor.close()
        db.close()
    }

    /**
     * Test that indexes are created for sync queries.
     */
    @Test
    fun indexes_are_created_for_sync_queries() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // Then - verify sync indexes exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='active_stocks'")
        val indexes = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val indexName = cursor.getString(0)
            indexes.add(indexName)
        }
        cursor.close()

        assertThat(indexes).contains("index_active_stocks_user_id")
        assertThat(indexes).contains("index_active_stocks_store_id")
        assertThat(indexes).contains("index_active_stocks_is_dirty")

        db.close()
    }

    /**
     * Test that we can insert and query users after migration.
     */
    @Test
    fun can_insert_and_query_users_after_migration() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        // When - insert a user
        val now = Instant.now().toEpochMilli()
        db.execSQL(
            """
            INSERT INTO users (id, email, storeId, role, deviceId, createdAt)
            VALUES ('user-123', 'test@decathlon.com', '1620', 'STAFF', 'device-abc', $now)
            """.trimIndent()
        )

        // Then - verify we can query the user
        val cursor = db.query("SELECT * FROM users WHERE id = 'user-123'")
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("user-123")
        assertThat(cursor.getString(cursor.getColumnIndex("email"))).isEqualTo("test@decathlon.com")
        assertThat(cursor.getString(cursor.getColumnIndex("storeId"))).isEqualTo("1620")
        assertThat(cursor.getString(cursor.getColumnIndex("role"))).isEqualTo("STAFF")
        assertThat(cursor.getString(cursor.getColumnIndex("deviceId"))).isEqualTo("device-abc")
        cursor.close()

        db.close()
    }

    /**
     * Test that we can insert and query active_stocks with sync columns after migration.
     */
    @Test
    fun can_insert_and_query_active_stocks_with_sync_columns_after_migration() {
        // Given - migrate database
        var db = helper.createDatabase(TEST_DB, 5).apply { close() }
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        val now = Instant.now().toEpochMilli()
        val syncedTime = Instant.now().plusSeconds(3600).toEpochMilli()

        // When - insert a record with sync columns
        db.execSQL(
            """
            INSERT INTO active_stocks (id, ean, quantity, expiryDate, createdAt, updatedAt, deletedAt, actionTaken, userId, storeId, syncedAt, version, deviceId, isDirty)
            VALUES ('batch-1', '8435408475366', 10, $now, $now, $now, NULL, 'PENDING', 'user-123', '1620', $syncedTime, 2, 'device-abc', 0)
            """.trimIndent()
        )

        // Then - verify we can query the record with all sync columns
        val cursor = db.query("SELECT * FROM active_stocks WHERE id = 'batch-1'")
        cursor.moveToFirst()
        assertThat(cursor.getString(cursor.getColumnIndex("id"))).isEqualTo("batch-1")
        assertThat(cursor.getString(cursor.getColumnIndex("userId"))).isEqualTo("user-123")
        assertThat(cursor.getString(cursor.getColumnIndex("storeId"))).isEqualTo("1620")
        assertThat(cursor.getLong(cursor.getColumnIndex("syncedAt"))).isEqualTo(syncedTime)
        assertThat(cursor.getInt(cursor.getColumnIndex("version"))).isEqualTo(2)
        assertThat(cursor.getString(cursor.getColumnIndex("deviceId"))).isEqualTo("device-abc")
        assertThat(cursor.getInt(cursor.getColumnIndex("isDirty"))).isEqualTo(0)
        cursor.close()

        db.close()
    }
}
