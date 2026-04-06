package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class UpdateBatchUseCaseTest {

    private lateinit var useCase: UpdateBatchUseCase
    private lateinit var mockRepository: StockRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = UpdateBatchUseCase(mockRepository)
    }

    // TEST 1: Update existing batch successfully
    @Test
    fun `invoke with existing batch should return rows affected`() = runTest {
        val batch = Batch(
            id = "batch-1",
            ean = "8435489901234",
            quantity = 20,
            expiryDate = Instant.now().plusSeconds(30 * 24 * 60 * 60),
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.updateBatch(batch) } returns 1

        val result = useCase(batch)

        assert(result == 1)
        coVerify { mockRepository.updateBatch(batch) }
    }

    // TEST 2: Update non-existent batch returns 0
    @Test
    fun `invoke with non-existent batch should return 0`() = runTest {
        val batch = Batch(
            id = "batch-2",
            ean = "8435489901234",
            quantity = 15,
            expiryDate = Instant.now().plusSeconds(30 * 24 * 60 * 60),
            status = SemaphoreStatus.YELLOW
        )

        coEvery { mockRepository.updateBatch(batch) } returns 0

        val result = useCase(batch)

        assert(result == 0)
    }

    // TEST 3: Update batch with different quantity
    @Test
    fun `invoke should update batch with new quantity`() = runTest {
        val batch = Batch(
            id = "batch-3",
            ean = "8435489901234",
            quantity = 50,
            expiryDate = Instant.now().plusSeconds(15 * 24 * 60 * 60),
            status = SemaphoreStatus.EXPIRED
        )

        coEvery { mockRepository.updateBatch(batch) } returns 1

        val result = useCase(batch)

        assert(result == 1)
        coVerify { mockRepository.updateBatch(batch) }
    }

    // TEST 4: Repository call verification
    @Test
    fun `invoke should call repository updateBatch exactly once`() = runTest {
        val batch = Batch(
            id = "batch-4",
            ean = "8435489901234",
            quantity = 25,
            expiryDate = Instant.now().plusSeconds(30 * 24 * 60 * 60),
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.updateBatch(batch) } returns 1

        useCase(batch)

        coVerify(exactly = 1) { mockRepository.updateBatch(batch) }
    }
}
