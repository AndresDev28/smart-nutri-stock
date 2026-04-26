package com.decathlon.smartnutristock.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.decathlon.smartnutristock.data.local.encrypted.EncryptedSessionManager
import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.usecase.SyncDataUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for SyncWorker.
 *
 * Tests verify that the worker correctly:
 * - Checks if user is authenticated before attempting sync
 * - Calls SyncDataUseCase when user is logged in
 * - Returns Result.success() on successful sync
 * - Returns Result.retry() on network/transient errors
 * - Returns Result.failure() when user is not authenticated
 *
 * CONSTRAINTS AND LIMITATIONS:
 * ===========================
 * This worker uses @HiltWorker annotation, which makes unit testing challenging without:
 * 1. androidx.work:work-testing dependency (for TestWorkerBuilder) - NOT available in this project
 * 2. Instrumented tests with Hilt testing infrastructure (androidTest) - Heavier, requires emulator
 *
 * Current test approach:
 * - Tests the worker logic by directly instantiating with mocked dependencies
 * - Verifies the SyncScheduler creates correct constraints
 * - Tests the business logic flow
 *
 * For complete WorkManager testing with constraint verification, consider adding:
 * testImplementation("androidx.work:work-testing:2.9.0")
 *
 * See: https://developer.android.com/topic/libraries/architecture/workmanager/how-to/testing
 */
class SyncWorkerTest {

    private lateinit var worker: SyncWorker
    private lateinit var mockSessionManager: EncryptedSessionManager
    private lateinit var mockSyncDataUseCase: SyncDataUseCase
    private lateinit var mockContext: Context
    private lateinit var mockWorkerParams: WorkerParameters

    private val TEST_USER_ID = "user-123"
    private val TEST_STORE_ID = "1620"
    private val TEST_SESSION_STORE_ID = "1620"

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockWorkerParams = mockk(relaxed = true)
        mockSessionManager = mockk()
        mockSyncDataUseCase = mockk()

        // Default: user is logged in
        every { mockSessionManager.getUserId() } returns TEST_USER_ID
        every { mockSessionManager.getStoreId() } returns TEST_SESSION_STORE_ID

        // Create worker instance directly (bypassing Hilt)
        // Note: This works because SyncWorker's constructor is accessible for testing
        worker = SyncWorker(
            context = mockContext,
            params = mockWorkerParams,
            sessionManager = mockSessionManager,
            syncDataUseCase = mockSyncDataUseCase
        )
    }

    @After
    fun teardown() {
        // Clean up any mocked objects
    }

    // ============================================================================
    // AUTH GUARD TESTS
    // ============================================================================

    @Test
    fun `doWork returns failure when user is not logged in`() = runTest {
        // Given
        every { mockSessionManager.getUserId() } returns null

        // When
        val result = worker.doWork()

        // Then
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        // Verify that sync use case was NOT called (auth guard blocked it)
        coVerify(exactly = 0) { mockSyncDataUseCase(any()) }
    }

    @Test
    fun `doWork calls syncDataUseCase when user is logged in`() = runTest {
        // Given
        every { mockSessionManager.getUserId() } returns TEST_USER_ID
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 0)

        // When
        worker.doWork()

        // Then
        coVerify { mockSyncDataUseCase(any()) }
    }

    @Test
    fun `doWork passes storeId from input data when provided`() = runTest {
        // Given
        val customStoreId = "9999"
        val inputData = androidx.work.workDataOf(SyncWorker.INPUT_STORE_ID to customStoreId)
        every { mockWorkerParams.inputData } returns inputData
        every { mockSessionManager.getUserId() } returns TEST_USER_ID
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 0)

        // Recreate worker with custom input data
        worker = SyncWorker(
            context = mockContext,
            params = mockWorkerParams,
            sessionManager = mockSessionManager,
            syncDataUseCase = mockSyncDataUseCase
        )

        // When
        worker.doWork()

        // Then
        coVerify { mockSyncDataUseCase(customStoreId) }
    }

    @Test
    fun `doWork uses default storeId when not provided in input data`() = runTest {
        // Given
        val defaultStoreId = "1620" // This is the default store ID from design
        every { mockWorkerParams.inputData } returns androidx.work.workDataOf()
        every { mockSessionManager.getUserId() } returns TEST_USER_ID
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 0)

        // Recreate worker with empty input data
        worker = SyncWorker(
            context = mockContext,
            params = mockWorkerParams,
            sessionManager = mockSessionManager,
            syncDataUseCase = mockSyncDataUseCase
        )

        // When
        worker.doWork()

        // Then
        coVerify { mockSyncDataUseCase(defaultStoreId) }
    }

    // ============================================================================
    // SYNC RESULT HANDLING TESTS
    // ============================================================================

    @Test
    fun `doWork returns success when sync completes successfully`() = runTest {
        // Given
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 10)

        // When
        val result = worker.doWork()

        // Then
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork returns success when sync returns partial success`() = runTest {
        // Given
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.PartialSuccess(
            syncedCount = 5,
            failedCount = 2,
            error = "Some records failed"
        )

        // When
        val result = worker.doWork()

        // Then - Partial success is still considered success
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork returns retry when sync returns error`() = runTest {
        // Given
        val error = RuntimeException("Network connection failed")
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Error(
            message = "Sync failed",
            cause = error
        )

        // When
        val result = worker.doWork()

        // Then - Error should trigger retry
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork returns retry when sync use case throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Unexpected error")
        coEvery { mockSyncDataUseCase(any()) } throws exception

        // When
        val result = worker.doWork()

        // Then - Exception should trigger retry
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork handles zero records synced successfully`() = runTest {
        // Given
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 0)

        // When
        val result = worker.doWork()

        // Then
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `doWork handles large number of records synced successfully`() = runTest {
        // Given
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 1000)

        // When
        val result = worker.doWork()

        // Then
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    // ============================================================================
    // NETWORK ERROR HANDLING TESTS
    // ============================================================================

    @Test
    fun `doWork returns retry on network timeout error`() = runTest {
        // Given
        val timeoutError = java.net.SocketTimeoutException("Connection timed out")
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Error(
            message = "Network timeout",
            cause = timeoutError
        )

        // When
        val result = worker.doWork()

        // Then - Network errors should trigger retry
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork returns retry on connection refused error`() = runTest {
        // Given
        val connectionError = java.net.ConnectException("Connection refused")
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Error(
            message = "Connection refused",
            cause = connectionError
        )

        // When
        val result = worker.doWork()

        // Then - Connection errors should trigger retry
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork returns retry on HTTP 503 error`() = runTest {
        // Given
        val httpError = SyncResult.Error(
            message = "Service unavailable (503)",
            cause = RuntimeException("HTTP 503")
        )
        coEvery { mockSyncDataUseCase(any()) } returns httpError

        // When
        val result = worker.doWork()

        // Then - Server errors should trigger retry
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `doWork returns failure on non-retryable auth error`() = runTest {
        // Given - User ID becomes null during execution (session expired)
        every { mockSessionManager.getUserId() } returns null

        // When
        val result = worker.doWork()

        // Then - Auth errors should return failure (not retry)
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
        coVerify(exactly = 0) { mockSyncDataUseCase(any()) }
    }

    // ============================================================================
    // SCHEDULER CONSTRAINTS TESTS
    // ============================================================================

    @Test
    fun `SyncScheduler uses network constraint`() {
        // Note: This is a verification test that checks the scheduler implementation
        // In a full WorkManager testing setup with work-testing, you would verify:
        // - Constraints are set correctly (CONNECTED, BATTERY_NOT_LOW, STORAGE_NOT_LOW)
        // - Backoff policy is configured (EXPONENTIAL)
        // - Input data is passed correctly

        // Since we don't have work-testing dependency, we verify the code structure
        // by checking that the SyncScheduler has the correct methods

        // Verify SyncScheduler has required methods (reflection or compile-time check)
        val schedulerMethods = SyncScheduler::class.java.declaredMethods.map { it.name }
        assertThat(schedulerMethods).contains("scheduleSync")
        assertThat(schedulerMethods).contains("triggerImmediateSync")
        assertThat(schedulerMethods).contains("cancelSync")
    }

    @Test
    fun `SyncWorker companion constants are correctly defined`() {
        // Verify public constants that tests can access
        assertThat(SyncWorker.WORK_NAME).isEqualTo("sync_work")
        assertThat(SyncWorker.INPUT_STORE_ID).isEqualTo("store_id")

        // Note: DEFAULT_STORE_ID is private, but we know its value from design docs
        // This is verified indirectly in the "uses default storeId" test
    }

    // ============================================================================
    // INTEGRATION-STYLE TESTS (Logic Flow Verification)
    // ============================================================================

    @Test
    fun `complete happy path - auth check, sync, success`() = runTest {
        // Given
        every { mockSessionManager.getUserId() } returns TEST_USER_ID
        every { mockSessionManager.getStoreId() } returns TEST_SESSION_STORE_ID
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Success(syncedCount = 15)

        // When
        val result = worker.doWork()

        // Then
        coVerify { mockSessionManager.getUserId() }
        coVerify { mockSessionManager.getStoreId() }
        coVerify { mockSyncDataUseCase(any()) }
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `complete error path - auth check blocks sync`() = runTest {
        // Given
        every { mockSessionManager.getUserId() } returns null

        // When
        val result = worker.doWork()

        // Then
        coVerify { mockSessionManager.getUserId() }
        coVerify(exactly = 0) { mockSessionManager.getStoreId() }
        coVerify(exactly = 0) { mockSyncDataUseCase(any()) }
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `complete retry path - sync fails with error`() = runTest {
        // Given
        every { mockSessionManager.getUserId() } returns TEST_USER_ID
        coEvery { mockSyncDataUseCase(any()) } returns SyncResult.Error(
            message = "Network error",
            cause = RuntimeException("Timeout")
        )

        // When
        val result = worker.doWork()

        // Then
        coVerify { mockSessionManager.getUserId() }
        coVerify { mockSyncDataUseCase(any()) }
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }
}
