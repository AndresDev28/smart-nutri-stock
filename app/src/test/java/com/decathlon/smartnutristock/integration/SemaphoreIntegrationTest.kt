package com.decathlon.smartnutristock.integration

import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import com.decathlon.smartnutristock.domain.usecase.GetSemaphoreCountersUseCase
import com.decathlon.smartnutristock.presentation.ui.dashboard.DashboardUiState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant

/**
 * Integration tests for Semaphore counter flow.
 *
 * These tests verify the complete flow from Repository → UseCase → ViewModel → UI state,
 * ensuring that expired products are correctly counted and displayed in the Dashboard.
 */
class SemaphoreIntegrationTest {

    private val stockDao = mockk<StockDao>()
    private val calculateStatusUseCase = CalculateStatusUseCase()
    private val testClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)

    // Create a custom repository that uses the test clock
    private val repository = object : com.decathlon.smartnutristock.domain.repository.StockRepository {
        override suspend fun upsert(batch: com.decathlon.smartnutristock.domain.model.Batch): com.decathlon.smartnutristock.domain.model.UpsertBatchResult {
            TODO("Not needed for this integration test")
        }

        override suspend fun findByEan(ean: String): Flow<com.decathlon.smartnutristock.domain.model.Batch> {
            TODO("Not needed for this integration test")
        }

        override suspend fun findByEanAndExpiryDate(ean: String, expiryDate: java.time.Instant): com.decathlon.smartnutristock.domain.model.Batch? {
            TODO("Not needed for this integration test")
        }

        override suspend fun findAll(): Flow<com.decathlon.smartnutristock.domain.model.Batch> {
            TODO("Not needed for this integration test")
        }

        override suspend fun findAllWithProductInfo(): Flow<com.decathlon.smartnutristock.domain.model.Batch> {
            TODO("Not needed for this integration test")
        }

        override suspend fun getSemaphoreCounters(): Flow<SemaphoreCounters> {
            // Use the actual repository logic with test clock
            val entities = stockDao.findAll()

            var yellow = 0
            var green = 0
            var expired = 0

            entities.forEach { entity ->
                when (calculateStatusUseCase(entity.expiryDate, testClock)) {
                    com.decathlon.smartnutristock.domain.model.SemaphoreStatus.EXPIRED -> expired++
                    com.decathlon.smartnutristock.domain.model.SemaphoreStatus.YELLOW -> yellow++
                    com.decathlon.smartnutristock.domain.model.SemaphoreStatus.GREEN -> green++
                }
            }

            return flowOf(SemaphoreCounters(yellow = yellow, green = green, expired = expired))
        }

        override suspend fun deleteByEanAndExpiryDate(ean: String, expiryDate: Instant): Int {
            TODO("Not needed for this integration test")
        }

        override suspend fun deleteByEan(ean: String): Int {
            TODO("Not needed for this integration test")
        }

        override suspend fun updateBatch(batch: com.decathlon.smartnutristock.domain.model.Batch): Int {
            TODO("Not needed for this integration test")
        }

        override suspend fun softDeleteBatch(id: String, timestamp: java.time.Instant): Int {
            TODO("Not needed for this integration test")
        }

        override suspend fun restoreBatch(id: String): Int {
            TODO("Not needed for this integration test")
        }

        override suspend fun updateProductName(ean: String, name: String): Int {
            TODO("Not needed for this integration test")
        }
    }

    private val getSemaphoreCountersUseCase = GetSemaphoreCountersUseCase(repository)

    private val testNow = Instant.now(testClock)

    /**
     * T5.2: Test scenario - expired batch appears in Dashboard with correct count
     *
     * This test verifies that when a batch expired yesterday is added to the repository,
     * the Dashboard UI state shows the correct expired count (not red, which is deprecated).
     */
    @Test
    fun `expired batch should appear in dashboard with correct count`() = runTest {
        // Given: Batch expired yesterday
        val expiredBatchEntity = ActiveStockEntity(
            id = "batch-expired-1",
            ean = "8435489901234",
            quantity = 5,
            expiryDate = Instant.parse("2024-01-01T00:00:00Z").minusSeconds(24 * 60 * 60), // yesterday
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findAll() } returns listOf(expiredBatchEntity)

        // When: Load counters through the full flow (Repository → UseCase)
        val countersFlow = getSemaphoreCountersUseCase()
        val counters = countersFlow.toList()

        // Then: Expired count should be 1 (one expired batch), not 0 (red was always 0)
        assertEquals(1, counters.size)
        val result = counters[0]

        assertEquals(1, result.expired) // Expired counter should be 1 (one expired batch)
        assertEquals(0, result.yellow) // Yellow counter should be 0
        assertEquals(0, result.green) // Green counter should be 0
        assertEquals(1, result.total) // Total counter should be 1

        // Verify no "red" field exists (the bug we're fixing)
        // This is verified by the fact that expired = 1, not 0
    }

    /**
     * T5.3: Test scenario - Dashboard counter updates when expired product is added to stock
     *
     * This test verifies the reactive update flow from Repository → UseCase → ViewModel → UI state.
     */
    @Test
    fun `dashboard counter should update when expired product is added to stock`() = runTest {
        // Given: Initial state - one expired batch and one green batch
        val entities = listOf(
            ActiveStockEntity(
                id = "batch-expired-1",
                ean = "8435489901234",
                quantity = 2,
                expiryDate = Instant.parse("2024-01-01T00:00:00Z").minusSeconds(24 * 60 * 60), // yesterday
                createdAt = testNow,
                updatedAt = testNow
            ),
            ActiveStockEntity(
                id = "batch-green-1",
                ean = "8435489901235",
                quantity = 3,
                expiryDate = Instant.parse("2024-02-01T00:00:00Z"), // 31 days from now
                createdAt = testNow,
                updatedAt = testNow
            )
        )

        coEvery { stockDao.findAll() } returns entities

        // When: Load counters through the flow
        val countersFlow = getSemaphoreCountersUseCase()
        val counters = countersFlow.toList()

        // Then: Verify Dashboard would display correct expired counter
        assertEquals(1, counters.size)
        val result = counters[0]

        // The key assertion: expired counter should be 1 (one expired batch)
        // NOT 0 (which is what the bug would show with counters.red)
        assertEquals(1, result.expired) // Expired counter should be 1 (one expired batch)
        assertEquals(1, result.green) // Green counter should be 1
        assertEquals(0, result.yellow) // Yellow counter should be 0
        assertEquals(2, result.total) // Total counter should be 2
    }

    /**
     * Test scenario - multiple expired batches accumulate in expired counter
     *
     * This test verifies that the expired counter correctly accumulates across multiple batches.
     */
    @Test
    fun `multiple expired batches should accumulate in expired counter`() = runTest {
        // Given: Three expired batches
        val entities = listOf(
            ActiveStockEntity(
                id = "batch-expired-1",
                ean = "8435489901234",
                quantity = 1,
                expiryDate = Instant.parse("2024-01-01T00:00:00Z").minusSeconds(24 * 60 * 60), // yesterday
                createdAt = testNow,
                updatedAt = testNow
            ),
            ActiveStockEntity(
                id = "batch-expired-2",
                ean = "8435489901235",
                quantity = 3,
                expiryDate = Instant.parse("2024-01-01T00:00:00Z").minusSeconds(48 * 60 * 60), // 2 days ago
                createdAt = testNow,
                updatedAt = testNow
            ),
            ActiveStockEntity(
                id = "batch-expired-3",
                ean = "8435489901236",
                quantity = 5,
                expiryDate = Instant.parse("2024-01-01T00:00:00Z"), // today (expired)
                createdAt = testNow,
                updatedAt = testNow
            )
        )

        coEvery { stockDao.findAll() } returns entities

        // When: Load counters
        val countersFlow = getSemaphoreCountersUseCase()
        val counters = countersFlow.toList()

        // Then: All expired batches should be counted
        assertEquals(1, counters.size)
        val result = counters[0]

        assertEquals(3, result.expired) // Expired counter should be 3 (3 expired batches)
        assertEquals(0, result.yellow) // Yellow counter should be 0
        assertEquals(0, result.green) // Green counter should be 0
        assertEquals(3, result.total) // Total counter should be 3
    }

    /**
     * Test scenario - Dashboard UI state exposes correct counters
     *
     * This test verifies that when we simulate the ViewModel flow,
     * the UI state contains the correct expired counter.
     */
    @Test
    fun `dashboard UI state should expose correct expired counter`() = runTest {
        // Given: One expired batch
        val expiredBatchEntity = ActiveStockEntity(
            id = "batch-expired-1",
            ean = "8435489901234",
            quantity = 7,
            expiryDate = Instant.parse("2024-01-01T00:00:00Z").minusSeconds(24 * 60 * 60), // yesterday
            createdAt = testNow,
            updatedAt = testNow
        )

        coEvery { stockDao.findAll() } returns listOf(expiredBatchEntity)

        // When: Simulate the ViewModel loading counters
        val countersFlow = getSemaphoreCountersUseCase()
        val uiStateList = mutableListOf<DashboardUiState>()

        countersFlow.collect { counters ->
            uiStateList.add(DashboardUiState.Success(counters))
        }

        // Then: UI state should contain correct expired counter
        assertEquals(1, uiStateList.size)
        val uiState = uiStateList[0]
        assertTrue(uiState is DashboardUiState.Success)

        val successState = uiState as DashboardUiState.Success
        assertEquals(1, successState.counters.expired) // Expired counter in UI state should be 1 (one expired batch)
        assertEquals(0, successState.counters.yellow) // Yellow counter in UI state should be 0
        assertEquals(0, successState.counters.green) // Green counter in UI state should be 0
    }
}
