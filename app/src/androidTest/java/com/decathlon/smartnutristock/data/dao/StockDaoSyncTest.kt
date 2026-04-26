package com.decathlon.smartnutristock.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.local.SmartNutriStockDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Integration tests for StockDao sync queries.
 *
 * Tests verify dirty flag management, orphan record claiming,
 * and store-specific queries added for multi-device synchronization.
 */
@RunWith(AndroidJUnit4::class)
class StockDaoSyncTest {

    private lateinit var database: SmartNutriStockDatabase
    private lateinit var stockDao: StockDao

    private val testNow = Instant.parse("2024-01-01T00:00:00Z")

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartNutriStockDatabase::class.java
        ).build()
        stockDao = database.stockDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test getDirtyRecords returns only records with isDirty = 1.
     */
    @Test
    fun getDirtyRecords_returns_only_records_with_isDirty_1() = runTest {
        // Given - insert some records with different isDirty values
        val cleanRecord = ActiveStockEntity(
            id = "clean-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 0
        )
        stockDao.insert(cleanRecord)

        val dirtyRecord1 = ActiveStockEntity(
            id = "dirty-1",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 1
        )
        stockDao.insert(dirtyRecord1)

        val dirtyRecord2 = ActiveStockEntity(
            id = "dirty-2",
            ean = "ean3",
            quantity = 30,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 1
        )
        stockDao.insert(dirtyRecord2)

        // When
        val result = stockDao.getDirtyRecords("1620").first()

        // Then - should only return dirty records
        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactly("dirty-1", "dirty-2")
        assertThat(result.map { it.isDirty }).doesNotContain(0)
    }

    /**
     * Test getDirtyRecords filters by storeId.
     */
    @Test
    fun getDirtyRecords_filters_by_storeId() = runTest {
        // Given - insert dirty records for different stores
        val store1620Dirty = ActiveStockEntity(
            id = "store1620-dirty",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 1
        )
        stockDao.insert(store1620Dirty)

        val store1621Dirty = ActiveStockEntity(
            id = "store1621-dirty",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1621",
            isDirty = 1
        )
        stockDao.insert(store1621Dirty)

        // When - query for store 1620
        val result1620 = stockDao.getDirtyRecords("1620").first()

        // Then - should only return records for store 1620
        assertThat(result1620).hasSize(1)
        assertThat(result1620[0].id).isEqualTo("store1620-dirty")
        assertThat(result1620[0].storeId).isEqualTo("1620")

        // When - query for store 1621
        val result1621 = stockDao.getDirtyRecords("1621").first()

        // Then - should only return records for store 1621
        assertThat(result1621).hasSize(1)
        assertThat(result1621[0].id).isEqualTo("store1621-dirty")
        assertThat(result1621[0].storeId).isEqualTo("1621")
    }

    /**
     * Test markAsSynced sets isDirty = 0 and updates syncedAt.
     */
    @Test
    fun markAsSynced_sets_isDirty_0_and_updates_syncedAt() = runTest {
        // Given
        val beforeSync = Instant.parse("2024-01-01T00:00:00Z")
        val afterSync = Instant.parse("2024-01-01T01:00:00Z")

        val record = ActiveStockEntity(
            id = "record-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = beforeSync,
            updatedAt = beforeSync,
            userId = "user-123",
            storeId = "1620",
            isDirty = 1,
            syncedAt = null,
            version = 1
        )
        stockDao.insert(record)

        // When
        val updatedCount = stockDao.markAsSynced(listOf("record-1"), afterSync)

        // Then
        assertThat(updatedCount).isEqualTo(1)

        val updated = stockDao.findById("record-1")
        assertThat(updated).isNotNull()
        assertThat(updated!!.isDirty).isEqualTo(0)
        assertThat(updated.syncedAt).isEqualTo(afterSync)
        assertThat(updated.version).isEqualTo(2) // version incremented
    }

    /**
     * Test markAsSynced updates multiple records.
     */
    @Test
    fun markAsSynced_updates_multiple_records() = runTest {
        // Given
        val syncedAt = Instant.parse("2024-01-01T01:00:00Z")

        val record1 = ActiveStockEntity(
            id = "record-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 1
        )
        stockDao.insert(record1)

        val record2 = ActiveStockEntity(
            id = "record-2",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 1
        )
        stockDao.insert(record2)

        // When
        val updatedCount = stockDao.markAsSynced(listOf("record-1", "record-2"), syncedAt)

        // Then
        assertThat(updatedCount).isEqualTo(2)

        val record1Updated = stockDao.findById("record-1")
        val record2Updated = stockDao.findById("record-2")

        assertThat(record1Updated!!.isDirty).isEqualTo(0)
        assertThat(record2Updated!!.isDirty).isEqualTo(0)
    }

    /**
     * Test claimOrphanRecords updates userId and storeId where userId is null.
     */
    @Test
    fun claimOrphanRecords_updates_userId_and_storeId_where_userId_is_null() = runTest {
        // Given
        val orphanRecord1 = ActiveStockEntity(
            id = "orphan-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "", // Orphan
            storeId = "",
            isDirty = 0
        )
        stockDao.insert(orphanRecord1)

        val orphanRecord2 = ActiveStockEntity(
            id = "orphan-2",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "", // Empty string is also orphan
            storeId = "",
            isDirty = 0
        )
        stockDao.insert(orphanRecord2)

        val ownedRecord = ActiveStockEntity(
            id = "owned-1",
            ean = "ean3",
            quantity = 30,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "other-user",
            storeId = "1620",
            isDirty = 0
        )
        stockDao.insert(ownedRecord)

        // When
        val updatedCount = stockDao.claimOrphanRecords("user-123", "1620")

        // Then
        assertThat(updatedCount).isEqualTo(2)

        val orphan1Updated = stockDao.findById("orphan-1")
        val orphan2Updated = stockDao.findById("orphan-2")
        val ownedUpdated = stockDao.findById("owned-1")

        assertThat(orphan1Updated!!.userId).isEqualTo("user-123")
        assertThat(orphan1Updated.storeId).isEqualTo("1620")
        assertThat(orphan1Updated.isDirty).isEqualTo(1) // Should be marked dirty

        assertThat(orphan2Updated!!.userId).isEqualTo("user-123")
        assertThat(orphan2Updated.storeId).isEqualTo("1620")
        assertThat(orphan2Updated.isDirty).isEqualTo(1)

        // Owned record should not be updated
        assertThat(ownedUpdated!!.userId).isEqualTo("other-user")
        assertThat(ownedUpdated.storeId).isEqualTo("1620")
        assertThat(ownedUpdated.isDirty).isEqualTo(0)
    }

    /**
     * Test claimOrphanRecords ignores soft-deleted records.
     */
    @Test
    fun claimOrphanRecords_ignores_soft_deleted_records() = runTest {
        // Given
        val deletedOrphan = ActiveStockEntity(
            id = "deleted-orphan",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "",
            storeId = "",
            isDirty = 0,
            deletedAt = testNow // Soft deleted
        )
        stockDao.insert(deletedOrphan)

        val activeOrphan = ActiveStockEntity(
            id = "active-orphan",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "",
            storeId = "",
            isDirty = 0,
            deletedAt = null // Not deleted
        )
        stockDao.insert(activeOrphan)

        // When
        val updatedCount = stockDao.claimOrphanRecords("user-123", "1620")

        // Then - should only update active orphan
        assertThat(updatedCount).isEqualTo(1)

        val activeUpdated = stockDao.findById("active-orphan")
        assertThat(activeUpdated!!.userId).isEqualTo("user-123")
        assertThat(activeUpdated.storeId).isEqualTo("1620")
    }

    /**
     * Test markAsDirty sets isDirty = 1 for specific record.
     */
    @Test
    fun markAsDirty_sets_isDirty_1_for_specific_record() = runTest {
        // Given
        val record = ActiveStockEntity(
            id = "record-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 0
        )
        stockDao.insert(record)

        // When
        val updatedCount = stockDao.markAsDirty("record-1")

        // Then
        assertThat(updatedCount).isEqualTo(1)

        val updated = stockDao.findById("record-1")
        assertThat(updated!!.isDirty).isEqualTo(1)
    }

    /**
     * Test markAsDirty returns 0 when record not found.
     */
    @Test
    fun markAsDirty_returns_0_when_record_not_found() = runTest {
        // When - try to mark non-existent record as dirty
        val updatedCount = stockDao.markAsDirty("non-existent")

        // Then
        assertThat(updatedCount).isEqualTo(0)
    }

    /**
     * Test getAllByStore returns only records for given storeId.
     */
    @Test
    fun getAllByStore_returns_only_records_for_given_storeId() = runTest {
        // Given
        val store1620Record1 = ActiveStockEntity(
            id = "store1620-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 0
        )
        stockDao.insert(store1620Record1)

        val store1620Record2 = ActiveStockEntity(
            id = "store1620-2",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 0
        )
        stockDao.insert(store1620Record2)

        val store1621Record = ActiveStockEntity(
            id = "store1621-1",
            ean = "ean3",
            quantity = 30,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1621",
            isDirty = 0
        )
        stockDao.insert(store1621Record)

        // When - query for store 1620
        val result1620 = stockDao.getAllByStore("1620").first()

        // Then
        assertThat(result1620).hasSize(2)
        assertThat(result1620.map { it.id }).containsExactly("store1620-1", "store1620-2")
        assertThat(result1620.map { it.storeId }).doesNotContain("1621")

        // When - query for store 1621
        val result1621 = stockDao.getAllByStore("1621").first()

        // Then
        assertThat(result1621).hasSize(1)
        assertThat(result1621[0].id).isEqualTo("store1621-1")
        assertThat(result1621[0].storeId).isEqualTo("1621")
    }

    /**
     * Test getAllByStore ignores soft-deleted records.
     */
    @Test
    fun getAllByStore_ignores_soft_deleted_records() = runTest {
        // Given
        val activeRecord = ActiveStockEntity(
            id = "active-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 0,
            deletedAt = null
        )
        stockDao.insert(activeRecord)

        val deletedRecord = ActiveStockEntity(
            id = "deleted-1",
            ean = "ean2",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            userId = "user-123",
            storeId = "1620",
            isDirty = 0,
            deletedAt = testNow // Soft deleted
        )
        stockDao.insert(deletedRecord)

        // When
        val result = stockDao.getAllByStore("1620").first()

        // Then - should only return active record
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("active-1")
    }

    /**
     * Test getAllByStore orders records by expiryDate ASC.
     */
    @Test
    fun getAllByStore_orders_records_by_expiryDate_ASC() = runTest {
        // Given
        val expiry1 = testNow
        val expiry2 = testNow.plusSeconds(86400) // 1 day later
        val expiry3 = testNow.plusSeconds(172800) // 2 days later

        stockDao.insert(ActiveStockEntity("record-2", "ean2", 20, expiry2, testNow, testNow, userId = "user-123", storeId = "1620", isDirty = 0))
        stockDao.insert(ActiveStockEntity("record-3", "ean3", 30, expiry3, testNow, testNow, userId = "user-123", storeId = "1620", isDirty = 0))
        stockDao.insert(ActiveStockEntity("record-1", "ean1", 10, expiry1, testNow, testNow, userId = "user-123", storeId = "1620", isDirty = 0))

        // When
        val result = stockDao.getAllByStore("1620").first()

        // Then - should be ordered by expiryDate ASC
        assertThat(result).hasSize(3)
        assertThat(result[0].expiryDate).isEqualTo(expiry1)
        assertThat(result[1].expiryDate).isEqualTo(expiry2)
        assertThat(result[2].expiryDate).isEqualTo(expiry3)
    }
}
