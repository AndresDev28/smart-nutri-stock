package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class UpsertStockUseCaseTest {

    private lateinit var useCase: UpsertStockUseCase
    private lateinit var mockRepository: StockRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = UpsertStockUseCase(mockRepository)
    }

    // TEST 1: Create new stock entry with valid data
    @Test
    fun `upsertStock with valid data should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 10
        val batch = Batch(
            id = "batch-1",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
        val status = (result as UpsertBatchResult.Success).status
        assert(status == SemaphoreStatus.GREEN)
    }

    // TEST 2: Update existing stock entry
    @Test
    fun `upsertStock should update existing entry`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 15
        val batch = Batch(
            id = "batch-2",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.YELLOW
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.YELLOW)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
        val status = (result as UpsertBatchResult.Success).status
        assert(status == SemaphoreStatus.YELLOW)
    }

    // TEST 3: Upsert with valid quantity
    @Test
    fun `upsertStock with positive quantity should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 100
        val batch = Batch(
            id = "batch-3",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
    }

    // TEST 4: Delete batch with zero quantity (Golden Rule)
    @Test
    fun `upsertStock with zero quantity should return Deleted`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 0
        val batch = Batch(
            id = "batch-4",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.EXPIRED
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Deleted

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Deleted)
    }

    // TEST 5: Delete batch with negative quantity (Golden Rule)
    @Test
    fun `upsertStock with negative quantity should return Deleted`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = -5
        val batch = Batch(
            id = "batch-5",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.EXPIRED
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Deleted

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Deleted)
    }

    // TEST 6: Repository error handling
    @Test
    fun `upsertStock should return Error when repository fails`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 10
        val batch = Batch(
            id = "batch-6",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Error("Database error")

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Error)
        assert((result as UpsertBatchResult.Error).message == "Database error")
    }

    // TEST 7: Edge case - quantity equals 1 (boundary)
    @Test
    fun `upsertStock with quantity equals 1 should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 1
        val batch = Batch(
            id = "batch-7",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.EXPIRED
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.EXPIRED)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
    }

    // TEST 8: Batch with EXPIRED semaphore status
    @Test
    fun `upsertStock with EXPIRED status should return Success with EXPIRED`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now().minusSeconds(10 * 24 * 60 * 60) // 10 days ago (expired)
        val quantity = 10
        val batch = Batch(
            id = "batch-8",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.EXPIRED
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.EXPIRED)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
        val status = (result as UpsertBatchResult.Success).status
        assert(status == SemaphoreStatus.EXPIRED)
    }

    // TEST 9: Batch with YELLOW semaphore status
    @Test
    fun `upsertStock with YELLOW status should return Success with YELLOW`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now().plusSeconds(20 * 24 * 60 * 60) // 20 days from now
        val quantity = 25
        val batch = Batch(
            id = "batch-9",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.YELLOW
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.YELLOW)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
        val status = (result as UpsertBatchResult.Success).status
        assert(status == SemaphoreStatus.YELLOW)
    }

    // TEST 10: Verify repository is called for upsert
    @Test
    fun `upsertStock should call repository for upsert`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 10
        val batch = Batch(
            id = "batch-10",
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)

        useCase.upsert(batch)

        coVerify { mockRepository.upsert(batch) }
    }

    // TEST 11: Same EAN + same expiry date → quantities should sum (repository handles it)
    @Test
    fun `upsert same EAN with same date should delegate to repository for quantity merge`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val originalBatch = Batch(
            id = "batch-existing",
            ean = ean,
            quantity = 5,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )
        val newBatch = Batch(
            id = "batch-new-uuid",
            ean = ean,
            quantity = 10,
            expiryDate = expiryDate,
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(newBatch) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)

        val result = useCase.upsert(newBatch)

        assert(result is UpsertBatchResult.Success)
        coVerify { mockRepository.upsert(newBatch) }
    }

    // TEST 12: Same EAN + different expiry date → creates new record
    @Test
    fun `upsert same EAN with different date should create new record`() = runTest {
        val ean = "8435489901234"
        val expiryDate1 = Instant.now()
        val expiryDate2 = Instant.now().plusSeconds(86400)
        val batch1 = Batch(
            id = "batch-1",
            ean = ean,
            quantity = 5,
            expiryDate = expiryDate1,
            status = SemaphoreStatus.GREEN
        )
        val batch2 = Batch(
            id = "batch-2",
            ean = ean,
            quantity = 10,
            expiryDate = expiryDate2,
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(batch1) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)
        coEvery { mockRepository.upsert(batch2) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)

        val result1 = useCase.upsert(batch1)
        val result2 = useCase.upsert(batch2)

        assert(result1 is UpsertBatchResult.Success)
        assert(result2 is UpsertBatchResult.Success)
        coVerify { mockRepository.upsert(batch1) }
        coVerify { mockRepository.upsert(batch2) }
    }

    // TEST 13: New EAN → creates new record
    @Test
    fun `upsert new EAN should create new record`() = runTest {
        val batch = Batch(
            id = "batch-new",
            ean = "9999999999999",
            quantity = 3,
            expiryDate = Instant.now(),
            status = SemaphoreStatus.GREEN
        )

        coEvery { mockRepository.upsert(batch) } returns UpsertBatchResult.Success(SemaphoreStatus.GREEN)

        val result = useCase.upsert(batch)

        assert(result is UpsertBatchResult.Success)
        coVerify { mockRepository.upsert(batch) }
    }
}
