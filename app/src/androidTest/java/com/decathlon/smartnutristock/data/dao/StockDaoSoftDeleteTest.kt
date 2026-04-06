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
 * Integration tests for StockDao soft-delete functionality.
 *
 * Tests verify that:
 * - All SELECT queries filter by deletedAt IS NULL
 * - softDelete() sets deletedAt timestamp
 * - restoreBatch() sets deletedAt to NULL
 * - update() works correctly
 */
@RunWith(AndroidJUnit4::class)
class StockDaoSoftDeleteTest {

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
     * Test that findAll() excludes soft-deleted batches.
     */
    @Test
    fun findAll_excludes_soft_deleted_batches() = runTest {
        // Given
        val activeBatch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = null
        )
        val deletedBatch = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475367",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = testNow
        )

        stockDao.insert(activeBatch)
        stockDao.insert(deletedBatch)

        // When
        val result = stockDao.findAll()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("batch-1")
    }

    /**
     * Test that findByEan() excludes soft-deleted batches.
     */
    @Test
    fun findByEan_excludes_soft_deleted_batches() = runTest {
        // Given
        val activeBatch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = null
        )
        val deletedBatch = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475366",
            quantity = 20,
            expiryDate = testNow.plusSeconds(86400),
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = testNow
        )

        stockDao.insert(activeBatch)
        stockDao.insert(deletedBatch)

        // When
        val result = stockDao.findByEan("8435408475366")

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("batch-1")
    }

    /**
     * Test that findByEanAndExpiryDate() excludes soft-deleted batches.
     */
    @Test
    fun findByEanAndExpiryDate_returns_null_for_soft_deleted_batch() = runTest {
        // Given
        val deletedBatch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = testNow
        )
        stockDao.insert(deletedBatch)

        // When
        val result = stockDao.findByEanAndExpiryDate("8435408475366", testNow)

        // Then
        assertThat(result).isNull()
    }

    /**
     * Test that findAllWithProductInfo() excludes soft-deleted batches.
     */
    @Test
    fun findAllWithProductInfo_excludes_soft_deleted_batches() = runTest {
        // Given
        val activeBatch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = null
        )
        val deletedBatch = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475367",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = testNow
        )

        stockDao.insert(activeBatch)
        stockDao.insert(deletedBatch)

        // When
        val result = stockDao.findAllWithProductInfo()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].id).isEqualTo("batch-1")
    }

    /**
     * Test that count() excludes soft-deleted batches.
     */
    @Test
    fun count_excludes_soft_deleted_batches() = runTest {
        // Given
        val activeBatch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = null
        )
        val deletedBatch = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475367",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = testNow
        )

        stockDao.insert(activeBatch)
        stockDao.insert(deletedBatch)

        // When
        val count = stockDao.count()

        // Then
        assertThat(count).isEqualTo(1)
    }

    /**
     * Test that softDelete() sets deletedAt timestamp.
     */
    @Test
    fun softDelete_sets_deletedAt_timestamp() = runTest {
        // Given
        val batch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = null
        )
        stockDao.insert(batch)

        // When
        val deleteTimestamp = testNow.plusSeconds(3600)
        val rowsAffected = stockDao.softDelete("batch-1", deleteTimestamp)

        // Then
        assertThat(rowsAffected).isEqualTo(1)

        val found = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(found).isNull() // Should be excluded by WHERE deletedAt IS NULL

        // But we can still update it (restore test will verify this)
        val updatedRows = stockDao.restoreBatch("batch-1")
        assertThat(updatedRows).isEqualTo(1)

        val restored = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(restored).isNotNull()
    }

    /**
     * Test that softDelete() returns 0 when batch not found.
     */
    @Test
    fun softDelete_returns_0_when_batch_not_found() = runTest {
        // When
        val rowsAffected = stockDao.softDelete("non-existent", testNow)

        // Then
        assertThat(rowsAffected).isEqualTo(0)
    }

    /**
     * Test that restoreBatch() sets deletedAt to NULL.
     */
    @Test
    fun restoreBatch_sets_deletedAt_to_NULL() = runTest {
        // Given
        val deletedBatch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = testNow
        )
        stockDao.insert(deletedBatch)

        // When
        val rowsAffected = stockDao.restoreBatch("batch-1")

        // Then
        assertThat(rowsAffected).isEqualTo(1)

        val found = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(found).isNotNull()
        assertThat(found!!.deletedAt).isNull()
    }

    /**
     * Test that restoreBatch() returns 0 when batch not found.
     */
    @Test
    fun restoreBatch_returns_0_when_batch_not_found() = runTest {
        // When
        val rowsAffected = stockDao.restoreBatch("non-existent")

        // Then
        assertThat(rowsAffected).isEqualTo(0)
    }

    /**
     * Test that update() works correctly with soft-delete field.
     */
    @Test
    fun update_works_with_deletedAt_field() = runTest {
        // Given
        val batch = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow,
            deletedAt = null
        )
        stockDao.insert(batch)

        // When
        val updatedBatch = batch.copy(
            quantity = 20,
            updatedAt = testNow.plusSeconds(3600)
        )
        val rowsAffected = stockDao.update(updatedBatch)

        // Then
        assertThat(rowsAffected).isEqualTo(1)

        val found = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(found).isNotNull()
        assertThat(found!!.quantity).isEqualTo(20)
    }
}
