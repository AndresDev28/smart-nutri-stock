package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.local.encrypted.EncryptedSessionManager
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant

/**
 * Unit tests for StockRepositoryImpl.
 *
 * Tests verify upsert logic (insert/update/delete), zero quantity rule,
 * and transaction safety.
 */
class StockRepositoryImplTest {

    private val stockDao = mockk<StockDao>()
    private val productCatalogDao = mockk<ProductCatalogDao>()
    private val calculateStatusUseCase = mockk<CalculateStatusUseCase>()
    private val sessionManager = mockk<EncryptedSessionManager>()
    private val repository = StockRepositoryImpl(stockDao, productCatalogDao, calculateStatusUseCase, sessionManager)

    private val testClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
    private val testNow = Instant.now(testClock)

    /**
     * Test upsert with new batch (insert).
     */
    @Test
    fun `upsert inserts new batch when not exists`() = runTest {
        // Given
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = Instant.parse("2026-07-01T00:00:00Z"),
            status = SemaphoreStatus.GREEN
        )

        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns null
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.insert(any()) } returns 1L

        // When
        val result = repository.upsert(batch)

        // Then
        assertTrue(result is UpsertBatchResult.Success)
        coVerify { stockDao.insert(any()) }
        coVerify(exactly = 0) { stockDao.update(any()) }
        coVerify(exactly = 0) { stockDao.deleteByEanAndExpiryDate(any(), any()) }
    }

    /**
     * Test upsert with existing batch (update).
     */
    @Test
    fun `upsert updates existing batch when exists`() = runTest {
        // Given
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 20,
            expiryDate = Instant.parse("2026-07-01T00:00:00Z"),
            status = SemaphoreStatus.GREEN
        )

        val existingEntity = ActiveStockEntity(
            id = "batch-1",
            ean = batch.ean,
            quantity = 10,
            expiryDate = batch.expiryDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.update(any()) } returns 1

        // When
        val result = repository.upsert(batch)

        // Then
        assertTrue(result is UpsertBatchResult.Success)
        coVerify { stockDao.update(any()) }
        coVerify(exactly = 0) { stockDao.insert(any()) }
        coVerify(exactly = 0) { stockDao.deleteByEanAndExpiryDate(any(), any()) }
    }

    /**
     * Test upsert deletes batch when quantity is zero (Golden Rule).
     */
    @Test
    fun `upsert deletes batch when quantity is zero`() = runTest {
        // Given
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 0,
            expiryDate = Instant.parse("2026-07-01T00:00:00Z"),
            status = SemaphoreStatus.EXPIRED
        )

        val existingEntity = ActiveStockEntity(
            id = "batch-1",
            ean = batch.ean,
            quantity = 10,
            expiryDate = batch.expiryDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.deleteByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns 1

        // When
        val result = repository.upsert(batch)

        // Then
        assertEquals(UpsertBatchResult.Deleted, result)
        coVerify { stockDao.deleteByEanAndExpiryDate(batch.ean, batch.expiryDate) }
        coVerify(exactly = 0) { stockDao.insert(any()) }
        coVerify(exactly = 0) { stockDao.update(any()) }
    }

    /**
     * Test upsert deletes batch when quantity is negative (Golden Rule).
     */
    @Test
    fun `upsert deletes batch when quantity is negative`() = runTest {
        // Given
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = -5,
            expiryDate = Instant.parse("2026-07-01T00:00:00Z"),
            status = SemaphoreStatus.EXPIRED
        )

        val existingEntity = ActiveStockEntity(
            id = "batch-1",
            ean = batch.ean,
            quantity = 10,
            expiryDate = batch.expiryDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.deleteByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns 1

        // When
        val result = repository.upsert(batch)

        // Then
        assertEquals(UpsertBatchResult.Deleted, result)
        coVerify { stockDao.deleteByEanAndExpiryDate(batch.ean, batch.expiryDate) }
        coVerify(exactly = 0) { stockDao.insert(any()) }
        coVerify(exactly = 0) { stockDao.update(any()) }
    }

    /**
     * Test findByEan returns batches for given EAN.
     */
    @Test
    fun `findByEan returns batches for given EAN`() = runTest {
        // Given
        val ean = "8435408475366"
        val entities = listOf(
            ActiveStockEntity("batch-1", ean, 10, Instant.parse("2026-07-01T00:00:00Z"), testNow, testNow),
            ActiveStockEntity("batch-2", ean, 5, Instant.parse("2026-08-01T00:00:00Z"), testNow, testNow)
        )

        coEvery { stockDao.findByEan(ean) } returns entities
        every { calculateStatusUseCase(any()) } returns SemaphoreStatus.GREEN

        // When
        val flow = repository.findByEan(ean)
        val batches = flow.toList()

        // Then
        assertEquals(2, batches.size)
        assertEquals(ean, batches[0].ean)
        assertEquals(ean, batches[1].ean)
    }

    /**
     * Test findByEanAndExpiryDate returns batch when exists.
     */
    @Test
    fun `findByEanAndExpiryDate returns batch when exists`() = runTest {
        // Given
        val ean = "8435408475366"
        val expiryDate = Instant.parse("2026-07-01T00:00:00Z")
        val entity = ActiveStockEntity("batch-1", ean, 10, expiryDate, testNow, testNow)

        coEvery { stockDao.findByEanAndExpiryDate(ean, expiryDate) } returns entity
        every { calculateStatusUseCase(any()) } returns SemaphoreStatus.GREEN

        // When
        val batch = repository.findByEanAndExpiryDate(ean, expiryDate)

        // Then
        assertNotNull(batch)
        assertEquals(ean, batch!!.ean)
        assertEquals(expiryDate, batch.expiryDate)
    }

    /**
     * Test findByEanAndExpiryDate returns null when not exists.
     */
    @Test
    fun `findByEanAndExpiryDate returns null when not exists`() = runTest {
        // Given
        val ean = "8435408475366"
        val expiryDate = Instant.parse("2026-07-01T00:00:00Z")

        coEvery { stockDao.findByEanAndExpiryDate(ean, expiryDate) } returns null

        // When
        val batch = repository.findByEanAndExpiryDate(ean, expiryDate)

        // Then
        assertEquals(null, batch)
    }

    /**
     * Test findAll returns all batches.
     */
    @Test
    fun `findAll returns all batches`() = runTest {
        // Given
        val entities = listOf(
            ActiveStockEntity("batch-1", "8435408475366", 10, Instant.parse("2026-07-01T00:00:00Z"), testNow, testNow),
            ActiveStockEntity("batch-2", "1234567890123", 5, Instant.parse("2026-08-01T00:00:00Z"), testNow, testNow)
        )

        coEvery { stockDao.findAll() } returns entities
        every { calculateStatusUseCase(any()) } returns SemaphoreStatus.GREEN

        // When
        val flow = repository.findAll()
        val batches = flow.toList()

        // Then
        assertEquals(2, batches.size)
    }

    /**
     * Test getSemaphoreCounters returns correct counts.
     */
    @Test
    fun `getSemaphoreCounters returns correct counts`() = runTest {
        // Given
        val entities = listOf(
            ActiveStockEntity("batch-1", "ean1", 10, Instant.parse("2026-07-01T00:00:00Z"), testNow, testNow),
            ActiveStockEntity("batch-2", "ean2", 5, Instant.parse("2026-08-01T00:00:00Z"), testNow, testNow),
            ActiveStockEntity("batch-3", "ean3", 15, Instant.parse("2026-09-01T00:00:00Z"), testNow, testNow),
            ActiveStockEntity("batch-4", "ean4", 20, Instant.parse("2024-01-01T00:00:00Z"), testNow, testNow)
        )

        coEvery { stockDao.findAll() } returns entities
        // Mock different statuses for different dates
        every { calculateStatusUseCase(Instant.parse("2026-07-01T00:00:00Z")) } returns SemaphoreStatus.EXPIRED
        every { calculateStatusUseCase(Instant.parse("2026-08-01T00:00:00Z")) } returns SemaphoreStatus.YELLOW
        every { calculateStatusUseCase(Instant.parse("2026-09-01T00:00:00Z")) } returns SemaphoreStatus.GREEN
        every { calculateStatusUseCase(Instant.parse("2024-01-01T00:00:00Z")) } returns SemaphoreStatus.EXPIRED

        // When
        val flow = repository.getSemaphoreCounters()
        val counters = flow.toList()

        // Then
        assertEquals(1, counters.size)
        val result = counters[0]
        assertEquals(1, result.yellow)
        assertEquals(1, result.green)
        assertEquals(2, result.expired)
    }

    /**
     * Test deleteByEanAndExpiryDate deletes batch.
     */
    @Test
    fun `deleteByEanAndExpiryDate deletes batch`() = runTest {
        // Given
        val ean = "8435408475366"
        val expiryDate = Instant.parse("2026-07-01T00:00:00Z")
        coEvery { stockDao.deleteByEanAndExpiryDate(ean, expiryDate) } returns 1

        // When
        val count = repository.deleteByEanAndExpiryDate(ean, expiryDate)

        // Then
        assertEquals(1, count)
        coVerify { stockDao.deleteByEanAndExpiryDate(ean, expiryDate) }
    }

    /**
     * Test deleteByEan deletes all batches for EAN.
     */
    @Test
    fun `deleteByEan deletes all batches for EAN`() = runTest {
        // Given
        val ean = "8435408475366"
        coEvery { stockDao.deleteByEan(ean) } returns 3

        // When
        val count = repository.deleteByEan(ean)

        // Then
        assertEquals(3, count)
        coVerify { stockDao.deleteByEan(ean) }
    }

    /**
     * Test upsert returns Error when insert fails.
     */
    @Test
    fun `upsert returns Error when insert fails`() = runTest {
        // Given
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = Instant.parse("2026-07-01T00:00:00Z"),
            status = SemaphoreStatus.GREEN
        )

        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate) } returns null
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.insert(any()) } returns -1L // Insert failed

        // When
        val result = repository.upsert(batch)

        // Then
        assertTrue(result is UpsertBatchResult.Error)
    }

    @Test
    fun `updateBatch updates existing batch when expiry date unchanged`() = runTest {
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 20,
            expiryDate = Instant.parse("2026-07-01T00:00:00Z"),
            status = SemaphoreStatus.GREEN
        )

        val existingEntity = ActiveStockEntity(
            id = "batch-1",
            ean = batch.ean,
            quantity = 10,
            expiryDate = batch.expiryDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findById(batch.id) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.update(any()) } returns 1

        val result = repository.updateBatch(batch)

        assertEquals(1, result)
        coVerify { stockDao.update(match { entity ->
            entity.id == "batch-1" &&
            entity.ean == "8435408475366" &&
            entity.quantity == 20 &&
            entity.expiryDate == Instant.parse("2026-07-01T00:00:00Z")
        }) }
    }

    @Test
    fun `updateBatch deletes old and inserts new when expiry date changes`() = runTest {
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val oldDate = Instant.parse("2026-07-01T00:00:00Z")
        val newDate = Instant.parse("2026-12-01T00:00:00Z")

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 20,
            expiryDate = newDate,
            status = SemaphoreStatus.GREEN
        )

        val existingEntity = ActiveStockEntity(
            id = "batch-1",
            ean = batch.ean,
            quantity = 10,
            expiryDate = oldDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findById(batch.id) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.deleteById("batch-1") } returns 1
        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, newDate) } returns null
        coEvery { stockDao.insert(any()) } returns 1L

        val result = repository.updateBatch(batch)

        assertEquals(1, result)
        coVerify { stockDao.deleteById("batch-1") }
        coVerify { stockDao.insert(match { entity ->
            entity.id == "batch-1" &&
            entity.ean == "8435408475366" &&
            entity.quantity == 20 &&
            entity.expiryDate == newDate
        }) }
    }

    @Test
    fun `updateBatch merges quantities when new date has existing batch`() = runTest {
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val oldDate = Instant.parse("2026-07-01T00:00:00Z")
        val newDate = Instant.parse("2026-12-01T00:00:00Z")

        val batch = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 5,
            expiryDate = newDate,
            status = SemaphoreStatus.GREEN
        )

        val existingEntity = ActiveStockEntity(
            id = "batch-1",
            ean = batch.ean,
            quantity = 10,
            expiryDate = oldDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        val targetExisting = ActiveStockEntity(
            id = "batch-2",
            ean = batch.ean,
            quantity = 15,
            expiryDate = newDate,
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findById(batch.id) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.deleteById("batch-1") } returns 1
        coEvery { stockDao.findByEanAndExpiryDate(batch.ean, newDate) } returns targetExisting
        coEvery { stockDao.update(any()) } returns 1

        val result = repository.updateBatch(batch)

        assertEquals(1, result)
        coVerify { stockDao.deleteById("batch-1") }
        coVerify { stockDao.update(match { entity ->
            entity.id == "batch-2" &&
            entity.quantity == 20 &&
            entity.expiryDate == newDate
        }) }
    }

    @Test
    fun `upsert sums quantities when same EAN and same expiry date`() = runTest {
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val expiryDate = Instant.parse("2026-07-01T00:00:00Z")

        val existingEntity = ActiveStockEntity(
            id = "existing-uuid",
            ean = "8435408475366",
            quantity = 5,
            expiryDate = expiryDate,
            createdAt = testNow.minusSeconds(1000),
            updatedAt = testNow
        )

        val newBatch = Batch(
            id = "new-uuid-from-scanner",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )

        coEvery { stockDao.findByEanAndExpiryDate(newBatch.ean, newBatch.expiryDate) } returns existingEntity
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.update(any()) } returns 1

        val result = repository.upsert(newBatch)

        assertTrue(result is UpsertBatchResult.Success)
        coVerify { stockDao.update(match { entity ->
            entity.id == "existing-uuid" &&
            entity.quantity == 15 &&
            entity.createdAt == testNow.minusSeconds(1000)
        }) }
        coVerify(exactly = 0) { stockDao.insert(any()) }
    }

    @Test
    fun `upsert creates new record when same EAN but different expiry date`() = runTest {
        every { sessionManager.getUserId() } returns "user-123"
        every { sessionManager.getStoreId() } returns "1620"

        val date1 = Instant.parse("2026-07-01T00:00:00Z")
        val date2 = Instant.parse("2026-12-01T00:00:00Z")

        val batch1 = Batch(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 5,
            expiryDate = date1,
            status = SemaphoreStatus.GREEN
        )
        val batch2 = Batch(
            id = "batch-2",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = date2,
            status = SemaphoreStatus.GREEN
        )

        coEvery { stockDao.findByEanAndExpiryDate(batch1.ean, batch1.expiryDate) } returns null
        coEvery { stockDao.findByEanAndExpiryDate(batch2.ean, batch2.expiryDate) } returns null
        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        coEvery { stockDao.insert(any()) } returns 1L

        val result1 = repository.upsert(batch1)
        val result2 = repository.upsert(batch2)

        assertTrue(result1 is UpsertBatchResult.Success)
        assertTrue(result2 is UpsertBatchResult.Success)
        coVerify(exactly = 2) { stockDao.insert(any()) }
        coVerify(exactly = 0) { stockDao.update(any()) }
    }

    /**
     * Test softDeleteBatch sets deletedAt timestamp.
     */
    @Test
    fun `softDeleteBatch sets deletedAt timestamp via DAO`() = runTest {
        // Given
        val batchId = "batch-1"
        val deleteTimestamp = testNow

        coEvery { stockDao.softDelete(batchId, deleteTimestamp) } returns 1

        // When
        val result = repository.softDeleteBatch(batchId, deleteTimestamp)

        // Then
        assertEquals(1, result)
        coVerify { stockDao.softDelete(batchId, deleteTimestamp) }
    }

    /**
     * Test softDeleteBatch returns 0 when batch not found.
     */
    @Test
    fun `softDeleteBatch returns 0 when batch not found`() = runTest {
        // Given
        val batchId = "non-existent"
        val deleteTimestamp = testNow

        coEvery { stockDao.softDelete(batchId, deleteTimestamp) } returns 0

        // When
        val result = repository.softDeleteBatch(batchId, deleteTimestamp)

        // Then
        assertEquals(0, result)
    }

    /**
     * Test restoreBatch sets deletedAt to NULL.
     */
    @Test
    fun `restoreBatch sets deletedAt to NULL via DAO`() = runTest {
        // Given
        val batchId = "batch-1"

        coEvery { stockDao.restoreBatch(batchId) } returns 1

        // When
        val result = repository.restoreBatch(batchId)

        // Then
        assertEquals(1, result)
        coVerify { stockDao.restoreBatch(batchId) }
    }

    /**
     * Test restoreBatch returns 0 when batch not found.
     */
    @Test
    fun `restoreBatch returns 0 when batch not found`() = runTest {
        // Given
        val batchId = "non-existent"

        coEvery { stockDao.restoreBatch(batchId) } returns 0

        // When
        val result = repository.restoreBatch(batchId)

        // Then
        assertEquals(0, result)
    }
}
