package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant

/**
 * Unit tests for SyncDataUseCase.
 *
 * Tests verify sync cycle: push dirty records then pull remote changes.
 */
class SyncDataUseCaseTest {

    private lateinit var useCase: SyncDataUseCase
    private lateinit var mockSyncRepository: SyncRepository
    private lateinit var testClock: Clock

    private val testNow = Instant.parse("2024-01-01T00:00:00Z")

    @Before
    fun setup() {
        mockSyncRepository = mockk()
        testClock = Clock.fixed(testNow, java.time.ZoneOffset.UTC)
        useCase = SyncDataUseCase(mockSyncRepository, testClock)
    }

    // TEST 1: Should push dirty records then pull remote changes
    @Test
    fun `should push dirty records then pull remote changes`() = runTest {
        // Given
        val storeId = "1620"
        val lastSyncTime = Instant.parse("2023-12-31T00:00:00Z")

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns lastSyncTime
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 5)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime)
        } returns SyncResult.Success(syncedCount = 3)

        // When
        val result = useCase(storeId)

        // Then
        coVerify { mockSyncRepository.getLastSyncTime(storeId) }
        coVerify { mockSyncRepository.pushDirtyRecords(storeId) }
        coVerify { mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime) }

        assert(result is SyncResult.Success)
        assert((result as SyncResult.Success).syncedCount == 8) // 5 pushed + 3 pulled
    }

    // TEST 2: Should return error when push fails (fail-fast, no pull)
    @Test
    fun `should return error when push fails (fail-fast, no pull)`() = runTest {
        // Given
        val storeId = "1620"
        val pushError = SyncResult.Error(
            message = "Network error",
            cause = RuntimeException("Connection failed")
        )

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns null
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns pushError

        // When
        val result = useCase(storeId)

        // Then
        coVerify { mockSyncRepository.pushDirtyRecords(storeId) }
        coVerify(exactly = 0) { mockSyncRepository.pullRemoteChanges(any(), any()) }

        assert(result is SyncResult.Error)
        assert((result as SyncResult.Error).message == "Network error")
    }

    // TEST 3: Should return partial success when push succeeds but pull fails
    @Test
    fun `should return partial success when push succeeds but pull fails`() = runTest {
        // Given
        val storeId = "1620"
        val lastSyncTime = Instant.parse("2023-12-31T00:00:00Z")

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns lastSyncTime
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 5)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime)
        } returns SyncResult.Error(
            message = "Pull failed",
            cause = Exception("Remote error")
        )

        // When
        val result = useCase(storeId)

        // Then
        assert(result is SyncResult.PartialSuccess)
        val partial = result as SyncResult.PartialSuccess
        assert(partial.syncedCount == 5) // Only pushed count
        assert(partial.failedCount == 0)
        assert(partial.error == "Push succeeded, but pull failed: Pull failed")
    }

    // TEST 4: Should return success with correct synced count
    @Test
    fun `should return success with correct synced count`() = runTest {
        // Given
        val storeId = "1620"
        val lastSyncTime = Instant.parse("2023-12-31T00:00:00Z")

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns lastSyncTime
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 10)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime)
        } returns SyncResult.Success(syncedCount = 15)

        // When
        val result = useCase(storeId)

        // Then
        assert(result is SyncResult.Success)
        assert((result as SyncResult.Success).syncedCount == 25) // 10 + 15
    }

    // TEST 5: Should handle partial success from push (no pull)
    @Test
    fun `should handle partial success from push (no pull)`() = runTest {
        // Given
        val storeId = "1620"

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns null
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.PartialSuccess(
            syncedCount = 3,
            failedCount = 2,
            error = "Some records failed"
        )

        // When
        val result = useCase(storeId)

        // Then
        coVerify { mockSyncRepository.pushDirtyRecords(storeId) }
        coVerify(exactly = 0) { mockSyncRepository.pullRemoteChanges(any(), any()) }

        assert(result is SyncResult.PartialSuccess)
        val partial = result as SyncResult.PartialSuccess
        assert(partial.syncedCount == 3)
        assert(partial.failedCount == 2)
    }

    // TEST 6: Should handle partial success from pull
    @Test
    fun `should handle partial success from pull`() = runTest {
        // Given
        val storeId = "1620"
        val lastSyncTime = Instant.parse("2023-12-31T00:00:00Z")

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns lastSyncTime
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 5)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime)
        } returns SyncResult.PartialSuccess(
            syncedCount = 7,
            failedCount = 1,
            error = "Some records failed to pull"
        )

        // When
        val result = useCase(storeId)

        // Then
        assert(result is SyncResult.PartialSuccess)
        val partial = result as SyncResult.PartialSuccess
        assert(partial.syncedCount == 12) // 5 pushed + 7 pulled
        assert(partial.failedCount == 1)
        assert(partial.error == "Some records failed to pull")
    }

    // TEST 7: Should use EPOCH as baseline when no last sync time
    @Test
    fun `should use EPOCH as baseline when no last sync time`() = runTest {
        // Given
        val storeId = "1620"

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns null
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 0)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, Instant.EPOCH)
        } returns SyncResult.Success(syncedCount = 0)

        // When
        val result = useCase(storeId)

        // Then
        coVerify {
            mockSyncRepository.pullRemoteChanges(storeId, Instant.EPOCH)
        }
        assert(result is SyncResult.Success)
    }

    // TEST 8: Should handle zero dirty records successfully
    @Test
    fun `should handle zero dirty records successfully`() = runTest {
        // Given
        val storeId = "1620"
        val lastSyncTime = Instant.parse("2023-12-31T00:00:00Z")

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns lastSyncTime
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 0)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime)
        } returns SyncResult.Success(syncedCount = 0)

        // When
        val result = useCase(storeId)

        // Then
        assert(result is SyncResult.Success)
        assert((result as SyncResult.Success).syncedCount == 0)
    }

    // TEST 9: Should accept custom storeId
    @Test
    fun `should accept custom storeId`() = runTest {
        // Given
        val storeId = "1621"
        val lastSyncTime = Instant.parse("2023-12-31T00:00:00Z")

        coEvery { mockSyncRepository.getLastSyncTime(storeId) } returns lastSyncTime
        coEvery {
            mockSyncRepository.pushDirtyRecords(storeId)
        } returns SyncResult.Success(syncedCount = 1)
        coEvery {
            mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime)
        } returns SyncResult.Success(syncedCount = 0)

        // When
        val result = useCase(storeId)

        // Then
        coVerify { mockSyncRepository.getLastSyncTime(storeId) }
        coVerify { mockSyncRepository.pushDirtyRecords(storeId) }
        coVerify { mockSyncRepository.pullRemoteChanges(storeId, lastSyncTime) }

        assert(result is SyncResult.Success)
    }

    // TEST 10: Should use default storeId when not provided
    @Test
    fun `should use default storeId when not provided`() = runTest {
        // Given
        val defaultStoreId = "1620"

        coEvery { mockSyncRepository.getLastSyncTime(defaultStoreId) } returns null
        coEvery {
            mockSyncRepository.pushDirtyRecords(defaultStoreId)
        } returns SyncResult.Success(syncedCount = 0)
        coEvery {
            mockSyncRepository.pullRemoteChanges(defaultStoreId, Instant.EPOCH)
        } returns SyncResult.Success(syncedCount = 0)

        // When
        val result = useCase()

        // Then
        coVerify { mockSyncRepository.getLastSyncTime(defaultStoreId) }
        coVerify { mockSyncRepository.pushDirtyRecords(defaultStoreId) }

        assert(result is SyncResult.Success)
    }
}
