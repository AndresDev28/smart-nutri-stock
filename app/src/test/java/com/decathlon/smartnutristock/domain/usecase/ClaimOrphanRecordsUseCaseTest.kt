package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ClaimOrphanRecordsUseCase.
 *
 * Tests verify orphan records cleanup logic.
 */
class ClaimOrphanRecordsUseCaseTest {

    private lateinit var useCase: ClaimOrphanRecordsUseCase
    private lateinit var mockSyncRepository: SyncRepository

    @Before
    fun setup() {
        mockSyncRepository = mockk()
        useCase = ClaimOrphanRecordsUseCase(mockSyncRepository)
    }

    // TEST 1: Should call syncRepository with correct userId and storeId
    @Test
    fun `should call syncRepository claimOrphanRecords with correct userId and storeId`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1620"
        val updatedCount = 5

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } returns Result.success(updatedCount)

        // When
        val result = useCase(userId, storeId)

        // Then
        coVerify { mockSyncRepository.claimOrphanRecords(userId, storeId) }
        assert(result.isSuccess)
        assert(result.getOrNull() == updatedCount)
    }

    // TEST 2: Should use default storeId when not provided
    @Test
    fun `should use default storeId when not provided`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1620" // Default
        val updatedCount = 3

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } returns Result.success(updatedCount)

        // When
        val result = useCase(userId)

        // Then
        coVerify { mockSyncRepository.claimOrphanRecords(userId, storeId) }
        assert(result.isSuccess)
    }

    // TEST 3: Should return success when cleanup completes
    @Test
    fun `should return success when cleanup completes`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1620"
        val updatedCount = 10

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } returns Result.success(updatedCount)

        // When
        val result = useCase(userId, storeId)

        // Then
        assert(result.isSuccess)
        assert(result.getOrNull() == updatedCount)
    }

    // TEST 4: Should return failure when cleanup throws
    @Test
    fun `should return failure when cleanup throws`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1620"
        val exception = Exception("Database error")

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } returns Result.failure(exception)

        // When
        val result = useCase(userId, storeId)

        // Then
        assert(result.isFailure)
        assert(result.exceptionOrNull() == exception)
    }

    // TEST 5: Should handle repository exception gracefully
    @Test
    fun `should handle repository exception gracefully`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1620"
        val exception = RuntimeException("Unexpected error")

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } throws exception

        // When
        val result = useCase(userId, storeId)

        // Then
        assert(result.isFailure)
        assert(result.exceptionOrNull() is Exception)
    }

    // TEST 6: Should return zero when no orphan records exist
    @Test
    fun `should return zero when no orphan records exist`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1620"
        val updatedCount = 0

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } returns Result.success(updatedCount)

        // When
        val result = useCase(userId, storeId)

        // Then
        assert(result.isSuccess)
        assert(result.getOrNull() == 0)
    }

    // TEST 7: Should accept custom storeId
    @Test
    fun `should accept custom storeId`() = runTest {
        // Given
        val userId = "user-123"
        val storeId = "1621" // Custom store ID
        val updatedCount = 2

        coEvery {
            mockSyncRepository.claimOrphanRecords(userId, storeId)
        } returns Result.success(updatedCount)

        // When
        val result = useCase(userId, storeId)

        // Then
        coVerify { mockSyncRepository.claimOrphanRecords(userId, storeId) }
        assert(result.isSuccess)
        assert(result.getOrNull() == updatedCount)
    }
}
