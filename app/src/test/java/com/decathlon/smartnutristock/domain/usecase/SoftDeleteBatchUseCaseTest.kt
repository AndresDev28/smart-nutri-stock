package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class SoftDeleteBatchUseCaseTest {

    private lateinit var useCase: SoftDeleteBatchUseCase
    private lateinit var mockRepository: StockRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = SoftDeleteBatchUseCase(mockRepository)
    }

    // TEST 1: Soft delete existing batch successfully
    @Test
    fun `invoke with existing batch should return 1`() = runTest {
        val batchId = "batch-1"
        val timestamp = Instant.now()

        coEvery { mockRepository.softDeleteBatch(batchId, timestamp) } returns 1

        val result = useCase(batchId, timestamp)

        assert(result == 1)
        coVerify { mockRepository.softDeleteBatch(batchId, timestamp) }
    }

    // TEST 2: Soft delete non-existent batch returns 0
    @Test
    fun `invoke with non-existent batch should return 0`() = runTest {
        val batchId = "non-existent"
        val timestamp = Instant.now()

        coEvery { mockRepository.softDeleteBatch(batchId, timestamp) } returns 0

        val result = useCase(batchId, timestamp)

        assert(result == 0)
    }

    // TEST 3: Repository call verification
    @Test
    fun `invoke should call repository softDeleteBatch exactly once`() = runTest {
        val batchId = "batch-2"
        val timestamp = Instant.now()

        coEvery { mockRepository.softDeleteBatch(batchId, timestamp) } returns 1

        useCase(batchId, timestamp)

        coVerify(exactly = 1) { mockRepository.softDeleteBatch(batchId, timestamp) }
    }

    // TEST 4: Verify timestamp is passed correctly
    @Test
    fun `invoke should pass timestamp to repository`() = runTest {
        val batchId = "batch-3"
        val timestamp = Instant.parse("2024-01-01T00:00:00Z")

        coEvery { mockRepository.softDeleteBatch(batchId, timestamp) } returns 1

        useCase(batchId, timestamp)

        coVerify { mockRepository.softDeleteBatch(batchId, timestamp) }
    }
}
