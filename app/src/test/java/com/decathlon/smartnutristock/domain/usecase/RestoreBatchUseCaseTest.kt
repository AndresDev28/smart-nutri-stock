package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RestoreBatchUseCaseTest {

    private lateinit var useCase: RestoreBatchUseCase
    private lateinit var mockRepository: StockRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = RestoreBatchUseCase(mockRepository)
    }

    // TEST 1: Restore existing soft-deleted batch successfully
    @Test
    fun `invoke with existing soft-deleted batch should return 1`() = runTest {
        val batchId = "batch-1"

        coEvery { mockRepository.restoreBatch(batchId) } returns 1

        val result = useCase(batchId)

        assert(result == 1)
        coVerify { mockRepository.restoreBatch(batchId) }
    }

    // TEST 2: Restore non-existent batch returns 0
    @Test
    fun `invoke with non-existent batch should return 0`() = runTest {
        val batchId = "non-existent"

        coEvery { mockRepository.restoreBatch(batchId) } returns 0

        val result = useCase(batchId)

        assert(result == 0)
    }

    // TEST 3: Repository call verification
    @Test
    fun `invoke should call repository restoreBatch exactly once`() = runTest {
        val batchId = "batch-2"

        coEvery { mockRepository.restoreBatch(batchId) } returns 1

        useCase(batchId)

        coVerify(exactly = 1) { mockRepository.restoreBatch(batchId) }
    }

    // TEST 4: Restore batch makes it visible again
    @Test
    fun `invoke should restore batch and make it visible`() = runTest {
        val batchId = "batch-3"

        coEvery { mockRepository.restoreBatch(batchId) } returns 1

        val result = useCase(batchId)

        assert(result == 1)
        coVerify { mockRepository.restoreBatch(batchId) }
    }
}
