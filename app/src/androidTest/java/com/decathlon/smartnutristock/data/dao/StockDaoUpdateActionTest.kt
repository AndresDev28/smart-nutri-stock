package com.decathlon.smartnutristock.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.local.SmartNutriStockDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Integration tests for StockDao updateAction method.
 *
 * Verifies that the actionTaken field can be updated correctly.
 */
@RunWith(AndroidJUnit4::class)
class StockDaoUpdateActionTest {

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
     * Test updateAction updates actionTaken from PENDING to DISCOUNTED.
     */
    @Test
    fun updateAction_changes_PENDING_to_DISCOUNTED() = runTest {
        // Given - insert batch with PENDING action
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "PENDING"
        )
        stockDao.insert(stock)

        // When - update action to DISCOUNTED
        val rowsAffected = stockDao.updateAction("batch-1", "DISCOUNTED")

        // Then - verify update succeeded
        assertThat(rowsAffected).isEqualTo(1)

        // Verify action was updated
        val updated = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(updated).isNotNull()
        assertThat(updated!!.actionTaken).isEqualTo("DISCOUNTED")
    }

    /**
     * Test updateAction updates actionTaken from PENDING to REMOVED.
     */
    @Test
    fun updateAction_changes_PENDING_to_REMOVED() = runTest {
        // Given - insert batch with PENDING action
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "PENDING"
        )
        stockDao.insert(stock)

        // When - update action to REMOVED
        val rowsAffected = stockDao.updateAction("batch-1", "REMOVED")

        // Then - verify update succeeded
        assertThat(rowsAffected).isEqualTo(1)

        // Verify action was updated
        val updated = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(updated).isNotNull()
        assertThat(updated!!.actionTaken).isEqualTo("REMOVED")
    }

    /**
     * Test updateAction updates actionTaken from DISCOUNTED back to PENDING.
     */
    @Test
    fun updateAction_changes_DISCOUNTED_to_PENDING() = runTest {
        // Given - insert batch with DISCOUNTED action
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "DISCOUNTED"
        )
        stockDao.insert(stock)

        // When - update action back to PENDING
        val rowsAffected = stockDao.updateAction("batch-1", "PENDING")

        // Then - verify update succeeded
        assertThat(rowsAffected).isEqualTo(1)

        // Verify action was updated
        val updated = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(updated).isNotNull()
        assertThat(updated!!.actionTaken).isEqualTo("PENDING")
    }

    /**
     * Test updateAction updates actionTaken from REMOVED back to PENDING.
     */
    @Test
    fun updateAction_changes_REMOVED_to_PENDING() = runTest {
        // Given - insert batch with REMOVED action
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "REMOVED"
        )
        stockDao.insert(stock)

        // When - update action back to PENDING
        val rowsAffected = stockDao.updateAction("batch-1", "PENDING")

        // Then - verify update succeeded
        assertThat(rowsAffected).isEqualTo(1)

        // Verify action was updated
        val updated = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(updated).isNotNull()
        assertThat(updated!!.actionTaken).isEqualTo("PENDING")
    }

    /**
     * Test updateAction returns 0 when batch not found.
     */
    @Test
    fun updateAction_returns_0_when_batch_not_found() = runTest {
        // Given - empty database

        // When - try to update non-existent batch
        val rowsAffected = stockDao.updateAction("non-existent", "DISCOUNTED")

        // Then - verify no rows were affected
        assertThat(rowsAffected).isEqualTo(0)
    }

    /**
     * Test updateAction only updates the specified batch, not others.
     */
    @Test
    fun updateAction_only_updates_specified_batch() = runTest {
        // Given - insert multiple batches
        val stock1 = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "PENDING"
        )
        val stock2 = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475367",
            quantity = 20,
            expiryDate = testNow.plusSeconds(86400),
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "PENDING"
        )
        stockDao.insert(stock1)
        stockDao.insert(stock2)

        // When - update only batch-1 to DISCOUNTED
        stockDao.updateAction("batch-1", "DISCOUNTED")

        // Then - verify batch-1 was updated, batch-2 was not
        val updated1 = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(updated1!!.actionTaken).isEqualTo("DISCOUNTED")

        val updated2 = stockDao.findByEanAndExpiryDate("8435408475367", testNow.plusSeconds(86400))
        assertThat(updated2!!.actionTaken).isEqualTo("PENDING")
    }

    /**
     * Test updateAction persists across multiple updates.
     */
    @Test
    fun updateAction_persists_across_multiple_updates() = runTest {
        // Given - insert batch with PENDING action
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            actionTaken = "PENDING"
        )
        stockDao.insert(stock)

        // When - update action multiple times
        stockDao.updateAction("batch-1", "DISCOUNTED")
        stockDao.updateAction("batch-1", "REMOVED")
        stockDao.updateAction("batch-1", "PENDING")

        // Then - verify final action is PENDING
        val updated = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(updated).isNotNull()
        assertThat(updated!!.actionTaken).isEqualTo("PENDING")
    }
}
