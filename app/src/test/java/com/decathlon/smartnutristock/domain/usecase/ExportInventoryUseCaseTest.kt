package com.decathlon.smartnutristock.domain.usecase

import android.content.Context
import com.decathlon.smartnutristock.domain.export.DocumentExporter
import com.decathlon.smartnutristock.domain.export.ExportFormat
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.WorkflowAction
import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant

class ExportInventoryUseCaseTest {

    private lateinit var useCase: ExportInventoryUseCase
    private lateinit var mockRepository: StockRepository
    private lateinit var mockCsvExporter: DocumentExporter
    private lateinit var mockPdfExporter: DocumentExporter
    private lateinit var mockContext: Context

    // Test data
    private val testBatches = listOf(
        Batch(
            id = "batch-1",
            ean = "1234567890123",
            quantity = 10,
            expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = "Arroz Blanco",
            packSize = 1000,
            actionTaken = WorkflowAction.PENDING
        ),
        Batch(
            id = "batch-2",
            ean = "9876543210987",
            quantity = 5,
            expiryDate = Instant.parse("2026-05-01T00:00:00Z"),
            status = SemaphoreStatus.YELLOW,
            name = "Barrita Proteína",
            packSize = 50,
            actionTaken = WorkflowAction.DISCOUNTED
        )
    )

    @Before
    fun setup() {
        mockRepository = mockk()
        mockCsvExporter = mockk()
        mockPdfExporter = mockk()
        mockContext = mockk()

        useCase = ExportInventoryUseCase(
            stockRepository = mockRepository,
            csvExporter = mockCsvExporter,
            pdfExporter = mockPdfExporter,
            context = mockContext
        )
    }

    // TEST 1: Export CSV format successfully
    @Test
    fun `invoke with CSV format should fetch products and delegate to CSV exporter`() = runTest {
        // Given: Repository returns batches (Flow<Batch> not Flow<List<Batch>>)
        val batchFlow = flowOf(testBatches[0], testBatches[1])
        coEvery { mockRepository.findAllWithProductInfo() } returns batchFlow

        // And: CSV exporter returns a file
        val mockFile = mockk<File>()
        coEvery { mockCsvExporter.export(testBatches, mockContext) } returns mockFile

        // When: Export is invoked with CSV format
        val result = useCase(ExportFormat.CSV)

        // Then: Result should be success
        assert(result.isSuccess)
        assert(result.getOrNull() == mockFile)

        // And: CSV exporter should be called
        coVerify { mockCsvExporter.export(testBatches, mockContext) }

        // And: PDF exporter should NOT be called
        coVerify(exactly = 0) { mockPdfExporter.export(any(), any()) }
    }

    // TEST 2: Export PDF format successfully
    @Test
    fun `invoke with PDF format should fetch products and delegate to PDF exporter`() = runTest {
        // Given: Repository returns batches
        val batchFlow = flowOf(testBatches[0], testBatches[1])
        coEvery { mockRepository.findAllWithProductInfo() } returns batchFlow

        // And: PDF exporter returns a file
        val mockFile = mockk<File>()
        coEvery { mockPdfExporter.export(testBatches, mockContext) } returns mockFile

        // When: Export is invoked with PDF format
        val result = useCase(ExportFormat.PDF)

        // Then: Result should be success
        assert(result.isSuccess)
        assert(result.getOrNull() == mockFile)

        // And: PDF exporter should be called
        coVerify { mockPdfExporter.export(testBatches, mockContext) }

        // And: CSV exporter should NOT be called
        coVerify(exactly = 0) { mockCsvExporter.export(any(), any()) }
    }

    // TEST 3: Export with empty inventory
    @Test
    fun `invoke with empty inventory should call exporter with empty list`() = runTest {
        // Given: Repository returns empty flow
        coEvery { mockRepository.findAllWithProductInfo() } returns flowOf()

        // And: CSV exporter returns a file
        val mockFile = mockk<File>()
        coEvery { mockCsvExporter.export(emptyList(), mockContext) } returns mockFile

        // When: Export is invoked with CSV format
        val result = useCase(ExportFormat.CSV)

        // Then: Result should be success
        assert(result.isSuccess)

        // And: CSV exporter should be called with empty list
        coVerify { mockCsvExporter.export(emptyList(), mockContext) }
    }

    // TEST 4: Export fails with repository error
    @Test
    fun `invoke when repository throws exception should return Result failure`() = runTest {
        // Given: Repository throws exception
        val exception = RuntimeException("Database connection failed")
        coEvery { mockRepository.findAllWithProductInfo() } throws exception

        // When: Export is invoked
        val result = useCase(ExportFormat.CSV)

        // Then: Result should be failure
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message == "Database connection failed")

        // And: Exporters should NOT be called
        coVerify(exactly = 0) { mockCsvExporter.export(any(), any()) }
        coVerify(exactly = 0) { mockPdfExporter.export(any(), any()) }
    }

    // TEST 5: Export fails with exporter IO exception
    @Test
    fun `invoke when exporter throws IOException should return Result failure`() = runTest {
        // Given: Repository returns batches
        val batchFlow = flowOf(testBatches[0], testBatches[1])
        coEvery { mockRepository.findAllWithProductInfo() } returns batchFlow

        // And: CSV exporter throws exception
        val exception = java.io.IOException("Failed to write to cache directory")
        coEvery { mockCsvExporter.export(testBatches, mockContext) } throws exception

        // When: Export is invoked
        val result = useCase(ExportFormat.CSV)

        // Then: Result should be failure
        assert(result.isFailure)
        assert(result.exceptionOrNull()?.message == "Failed to write to cache directory")
    }

    // TEST 6: Export with single batch
    @Test
    fun `invoke with single batch should export correctly`() = runTest {
        // Given: Repository returns single batch
        val singleBatch = testBatches[0]
        val batchFlow = flowOf(singleBatch)
        coEvery { mockRepository.findAllWithProductInfo() } returns batchFlow

        // And: CSV exporter returns a file
        val mockFile = mockk<File>()
        coEvery { mockCsvExporter.export(listOf(singleBatch), mockContext) } returns mockFile

        // When: Export is invoked
        val result = useCase(ExportFormat.CSV)

        // Then: Result should be success
        assert(result.isSuccess)

        // And: Exporter should be called with single batch
        coVerify { mockCsvExporter.export(listOf(singleBatch), mockContext) }
    }

    // TEST 7: Export with multiple batches (stress test)
    @Test
    fun `invoke with many batches should export all batches`() = runTest {
        // Given: Repository returns 100 batches
        val manyBatches = (1..100).map { index ->
            Batch(
                id = "batch-$index",
                ean = "1234567890123",
                quantity = index,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.GREEN,
                name = "Product $index",
                packSize = 1000,
                actionTaken = WorkflowAction.PENDING
            )
        }
        // Create Flow from list
        val batchFlow = flowOf(*manyBatches.toTypedArray())
        coEvery { mockRepository.findAllWithProductInfo() } returns batchFlow

        // And: PDF exporter returns a file
        val mockFile = mockk<File>()
        coEvery { mockPdfExporter.export(manyBatches, mockContext) } returns mockFile

        // When: Export is invoked
        val result = useCase(ExportFormat.PDF)

        // Then: Result should be success
        assert(result.isSuccess)

        // And: Exporter should be called with all 100 batches
        coVerify { mockPdfExporter.export(manyBatches, mockContext) }
    }

    // TEST 8: Export with batches containing null values
    @Test
    fun `invoke with batches containing null name and packSize should handle gracefully`() = runTest {
        // Given: Repository returns batch with null values
        val batchWithNulls = Batch(
            id = "batch-null",
            ean = "1234567890123",
            quantity = 10,
            expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = null,
            packSize = null,
            actionTaken = WorkflowAction.PENDING
        )
        val batchFlow = flowOf(batchWithNulls)
        coEvery { mockRepository.findAllWithProductInfo() } returns batchFlow

        // And: CSV exporter returns a file
        val mockFile = mockk<File>()
        coEvery { mockCsvExporter.export(listOf(batchWithNulls), mockContext) } returns mockFile

        // When: Export is invoked
        val result = useCase(ExportFormat.CSV)

        // Then: Result should be success
        assert(result.isSuccess)

        // And: Exporter should handle null values
        coVerify { mockCsvExporter.export(listOf(batchWithNulls), mockContext) }
    }
}
