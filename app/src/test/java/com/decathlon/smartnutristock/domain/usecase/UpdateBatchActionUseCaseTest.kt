package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.WorkflowAction
import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for UpdateBatchActionUseCase.
 *
 * Verifies that the use case correctly delegates to the repository.
 */
class UpdateBatchActionUseCaseTest {

    private lateinit var useCase: UpdateBatchActionUseCase
    private lateinit var mockRepository: StockRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = UpdateBatchActionUseCase(mockRepository)
    }

    @Test
    fun `invoke with DISCOUNTED action should update batch correctly`() = runTest {
        val batchId = "batch-1"
        val action = WorkflowAction.DISCOUNTED

        coEvery { mockRepository.updateBatchAction(batchId, action) } returns 1

        useCase(batchId, action)

        coVerify { mockRepository.updateBatchAction(batchId, action) }
    }

    @Test
    fun `invoke with REMOVED action should update batch correctly`() = runTest {
        val batchId = "batch-2"
        val action = WorkflowAction.REMOVED

        coEvery { mockRepository.updateBatchAction(batchId, action) } returns 1

        useCase(batchId, action)

        coVerify { mockRepository.updateBatchAction(batchId, action) }
    }

    @Test
    fun `invoke with PENDING action should update batch correctly`() = runTest {
        val batchId = "batch-3"
        val action = WorkflowAction.PENDING

        coEvery { mockRepository.updateBatchAction(batchId, action) } returns 1

        useCase(batchId, action)

        coVerify { mockRepository.updateBatchAction(batchId, action) }
    }

    @Test
    fun `invoke should call repository updateBatchAction exactly once`() = runTest {
        val batchId = "batch-4"
        val action = WorkflowAction.DISCOUNTED

        coEvery { mockRepository.updateBatchAction(batchId, action) } returns 1

        useCase(batchId, action)

        coVerify(exactly = 1) { mockRepository.updateBatchAction(batchId, action) }
    }

    @Test
    fun `invoke with non-existent batch should return 0`() = runTest {
        val batchId = "non-existent"
        val action = WorkflowAction.DISCOUNTED

        coEvery { mockRepository.updateBatchAction(batchId, action) } returns 0

        useCase(batchId, action)

        coVerify { mockRepository.updateBatchAction(batchId, action) }
    }
}
