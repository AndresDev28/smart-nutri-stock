package com.decathlon.smartnutristock.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.decathlon.smartnutristock.data.notification.NotificationHelper
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.repository.StockRepository
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for StatusCheckWorker.
 *
 * Tests verify that the worker correctly:
 * - Fetches all batches from the repository
 * - Calculates status for each batch
 * - Groups batches by YELLOW/EXPIRED status
 * - Sends grouped notifications for each status
 */
class StatusCheckWorkerTest {

    private lateinit var worker: StatusCheckWorker
    private lateinit var mockRepository: StockRepository
    private lateinit var mockNotificationHelper: NotificationHelper
    private lateinit var mockCalculateStatusUseCase: CalculateStatusUseCase
    private lateinit var mockContext: Context
    private lateinit var mockWorkerParams: WorkerParameters

    private val testBatches = listOf(
        Batch(
            id = "batch-1",
            ean = "ean1",
            quantity = 10,
            expiryDate = Instant.parse("2024-01-15T00:00:00Z"),
            status = SemaphoreStatus.EXPIRED,
            name = "Protein Bar",
            packSize = 50
        ),
        Batch(
            id = "batch-2",
            ean = "ean2",
            quantity = 5,
            expiryDate = Instant.parse("2024-01-20T00:00:00Z"),
            status = SemaphoreStatus.YELLOW,
            name = "Energy Drink",
            packSize = 500
        ),
        Batch(
            id = "batch-3",
            ean = "ean3",
            quantity = 15,
            expiryDate = Instant.parse("2024-01-18T00:00:00Z"),
            status = SemaphoreStatus.EXPIRED,
            name = "Isotonic Gel",
            packSize = 30
        ),
        Batch(
            id = "batch-4",
            ean = "ean4",
            quantity = 20,
            expiryDate = Instant.parse("2024-02-01T00:00:00Z"),
            status = SemaphoreStatus.YELLOW,
            name = "Recovery Bar",
            packSize = 60
        ),
        Batch(
            id = "batch-5",
            ean = "ean5",
            quantity = 25,
            expiryDate = Instant.parse("2024-03-01T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = "Protein Shake",
            packSize = 500
        )
    )

    @Before
    fun setup() {
        mockContext = mockk()
        mockWorkerParams = mockk(relaxed = true) // relaxed to provide default answers for infrastructure methods
        mockRepository = mockk()
        mockNotificationHelper = mockk(relaxed = true)
        mockCalculateStatusUseCase = mockk()

        // Mock repository to return test batches
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*testBatches.toTypedArray())

        // Mock CalculateStatusUseCase to return the status based on expiry date
        // Since test batches already have correct status, return batch.status
        coEvery { mockCalculateStatusUseCase(any()) } answers { call ->
            val expiryDate = call.invocation.args[0] as Instant
            // Find the test batch with this expiry date and return its status
            testBatches.find { it.expiryDate == expiryDate }?.status ?: SemaphoreStatus.GREEN
        }

        // Create worker instance with mocked dependencies
        worker = StatusCheckWorker(
            context = mockContext,
            params = mockWorkerParams,
            stockRepository = mockRepository,
            notificationHelper = mockNotificationHelper,
            calculateStatusUseCase = mockCalculateStatusUseCase
        )
    }

    @Test
    fun `doWork fetches all batches from repository`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*testBatches.toTypedArray())

        // When
        worker.doWork()

        // Then
        coVerify { mockRepository.findAllWithProductInfo() }
    }

    @Test
    fun `doWork sends YELLOW notification with correct count and names`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*testBatches.toTypedArray())

        // When
        worker.doWork()

        // Then - verify YELLOW notification was sent with count=2 and correct names
        coVerify {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.YELLOW,
                2,
                listOf("Energy Drink", "Recovery Bar")
            )
        }
    }

    @Test
    fun `doWork sends EXPIRED notification with correct count and names`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*testBatches.toTypedArray())

        // When
        worker.doWork()

        // Then - verify EXPIRED notification was sent with count=2 and correct names
        coVerify {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.EXPIRED,
                2,
                listOf("Protein Bar", "Isotonic Gel")
            )
        }
    }

    @Test
    fun `doWork does not send GREEN notification`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*testBatches.toTypedArray())

        // When
        worker.doWork()

        // Then - GREEN notification should NOT be sent
        coVerify(exactly = 0) {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.GREEN,
                any(),
                any()
            )
        }
    }

    @Test
    fun `doWork returns Result success`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*testBatches.toTypedArray())

        // When
        val result = worker.doWork()

        // Then
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork handles empty batch list correctly`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf()

        // When
        worker.doWork()

        // Then - no notifications should be sent
        coVerify(exactly = 0) {
            mockNotificationHelper.sendGroupedNotification(any(), any(), any())
        }
    }

    @Test
    fun `doWork handles only YELLOW batches correctly`() = runTest {
        // Given
        val yellowOnlyBatches = testBatches.filter { it.status == SemaphoreStatus.YELLOW }
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*yellowOnlyBatches.toTypedArray())

        // When
        worker.doWork()

        // Then
        coVerify {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.YELLOW,
                2,
                listOf("Energy Drink", "Recovery Bar")
            )
        }
        coVerify(exactly = 0) {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.EXPIRED,
                any(),
                any()
            )
        }
    }

    @Test
    fun `doWork handles only EXPIRED batches correctly`() = runTest {
        // Given
        val expiredOnlyBatches = testBatches.filter { it.status == SemaphoreStatus.EXPIRED }
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*expiredOnlyBatches.toTypedArray())

        // When
        worker.doWork()

        // Then
        coVerify {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.EXPIRED,
                2,
                listOf("Protein Bar", "Isotonic Gel")
            )
        }
        coVerify(exactly = 0) {
            mockNotificationHelper.sendGroupedNotification(
                SemaphoreStatus.YELLOW,
                any(),
                any()
            )
        }
    }

    @Test
    fun `doWork handles batches without product names correctly`() = runTest {
        // Given - batches without names
        val batchesWithoutNames = listOf(
            Batch(
                id = "batch-1",
                ean = "ean1",
                quantity = 10,
                expiryDate = Instant.parse("2024-01-15T00:00:00Z"),
                status = SemaphoreStatus.EXPIRED,
                name = null,
                packSize = 50
            ),
            Batch(
                id = "batch-2",
                ean = "ean2",
                quantity = 5,
                expiryDate = Instant.parse("2024-01-20T00:00:00Z"),
                status = SemaphoreStatus.YELLOW,
                name = null,
                packSize = 500
            )
        )

        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf(*batchesWithoutNames.toTypedArray())

        // When
        val result = worker.doWork()

        // Then - no notifications should be sent (batches without names are filtered out)
        coVerify(exactly = 0) {
            mockNotificationHelper.sendGroupedNotification(any(), any(), any())
        }
        // And the worker should still return success
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork returns failure when exception occurs`() = runTest {
        // Given
        coEvery { mockRepository.findAllWithProductInfo() } throws RuntimeException("Database error")

        // When
        val result = worker.doWork()

        // Then
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }
}
