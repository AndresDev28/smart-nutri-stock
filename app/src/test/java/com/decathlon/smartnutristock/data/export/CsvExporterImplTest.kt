package com.decathlon.smartnutristock.data.export

import android.content.Context
import com.decathlon.smartnutristock.domain.export.DocumentExporter
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.WorkflowAction
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class CsvExporterImplTest {

    private lateinit var exporter: CsvExporterImpl
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
        mockContext = mockk()
        exporter = CsvExporterImpl()

        // Mock cache directory
        val mockCacheDir = File(System.getProperty("java.io.tmpdir"), "csv_test_${System.currentTimeMillis()}")
        mockCacheDir.mkdirs()

        every { mockContext.cacheDir } returns mockCacheDir
    }

    // TEST 1: Export product with semicolon in name (RFC 4180)
    @Test
    fun `export with semicolon in name should wrap field in quotes`() = runTest {
        // Given: Batch with semicolon in name (our CSV uses semicolon as separator)
        val batchWithSemicolon = Batch(
            id = "batch-semicolon",
            ean = "1234567890123",
            quantity = 10,
            expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = "Barrita; Proteína Chocolate",
            packSize = 50,
            actionTaken = WorkflowAction.PENDING
        )

        // When: Export is called
        val file = exporter.export(listOf(batchWithSemicolon), mockContext)

        // Then: File should exist
        assertTrue(file.exists())

        // And: Field should be wrapped in quotes (RFC 4180 requires quoting when field contains separator)
        val content = file.readText()
        assertTrue(content.contains("\"Barrita; Proteína Chocolate\""))
    }

    // TEST 2: Export product with double quotes in name (RFC 4180)
    @Test
    fun `export with double quotes in name should escape quotes as double quotes`() = runTest {
        // Given: Batch with quotes in name
        val batchWithQuotes = Batch(
            id = "batch-quotes",
            ean = "1234567890123",
            quantity = 10,
            expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = "Producto \"Especial\"",
            packSize = 50,
            actionTaken = WorkflowAction.PENDING
        )

        // When: Export is called
        val file = exporter.export(listOf(batchWithQuotes), mockContext)

        // Then: File should exist
        assertTrue(file.exists())

        // And: Quotes should be escaped as double quotes
        val content = file.readText()
        assertTrue(content.contains("\"Producto \"\"Especial\"\"\""))
    }

    // TEST 3: Export product with special characters (accents)
    @Test
    fun `export with accents should preserve characters in UTF-8`() = runTest {
        // Given: Batch with accents
        val batchWithAccents = Batch(
            id = "batch-accents",
            ean = "1234567890123",
            quantity = 10,
            expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = "Café con Leche",
            packSize = 50,
            actionTaken = WorkflowAction.PENDING
        )

        // When: Export is called
        val file = exporter.export(listOf(batchWithAccents), mockContext)

        // Then: File should exist
        assertTrue(file.exists())

        // And: Accents should be preserved
        val content = file.readText()
        assertTrue(content.contains("Café con Leche"))
    }

    // TEST 4: Export empty inventory generates valid CSV
    @Test
    fun `export with empty inventory should generate CSV with only headers`() = runTest {
        // When: Export is called with empty list
        val file = exporter.export(emptyList(), mockContext)

        // Then: File should exist
        assertTrue(file.exists())

        // And: Should contain only header row (2 lines: header + empty = header only)
        val lines = file.readLines()
        assertTrue(lines.isNotEmpty())

        // And: First line should be header
        val header = lines[0]
        assertTrue(header.contains("EAN") || header.contains("Nombre"))
    }

    // TEST 5: Export normal data generates correct structure
    @Test
    fun `export with normal data should generate CSV with header and rows`() = runTest {
        // When: Export is called with test batches
        val file = exporter.export(testBatches, mockContext)

        // Then: File should exist
        assertTrue(file.exists())

        // And: Should contain header + 2 data rows = 3 lines
        val lines = file.readLines()
        assertEquals(3, lines.size)

        // And: First line should be header
        assertTrue(lines[0].contains("EAN") || lines[0].contains("Nombre"))

        // And: Should contain batch data
        val content = file.readText()
        assertTrue(content.contains("Arroz Blanco"))
        assertTrue(content.contains("Barrita Proteína"))
    }

    // TEST 6: WorkflowAction enum serialization
    @Test
    fun `export should serialize WorkflowAction enum correctly`() = runTest {
        // Given: Batches with different actions
        val batches = listOf(
            Batch(
                id = "batch-1",
                ean = "1234567890123",
                quantity = 10,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.GREEN,
                name = "Product 1",
                packSize = 1000,
                actionTaken = WorkflowAction.PENDING
            ),
            Batch(
                id = "batch-2",
                ean = "1234567890123",
                quantity = 5,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.YELLOW,
                name = "Product 2",
                packSize = 50,
                actionTaken = WorkflowAction.DISCOUNTED
            ),
            Batch(
                id = "batch-3",
                ean = "1234567890123",
                quantity = 2,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.EXPIRED,
                name = "Product 3",
                packSize = 50,
                actionTaken = WorkflowAction.REMOVED
            )
        )

        // When: Export is called
        val file = exporter.export(batches, mockContext)

        // Then: Should contain all action enum values
        val content = file.readText()
        assertTrue(content.contains("PENDING"))
        assertTrue(content.contains("DISCOUNTED"))
        assertTrue(content.contains("REMOVED"))
    }

    // TEST 7: SemaphoreStatus enum serialization
    @Test
    fun `export should serialize SemaphoreStatus enum correctly`() = runTest {
        // Given: Batches with different statuses
        val batches = listOf(
            Batch(
                id = "batch-1",
                ean = "1234567890123",
                quantity = 10,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.GREEN,
                name = "Product 1",
                packSize = 1000,
                actionTaken = WorkflowAction.PENDING
            ),
            Batch(
                id = "batch-2",
                ean = "1234567890123",
                quantity = 5,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.YELLOW,
                name = "Product 2",
                packSize = 50,
                actionTaken = WorkflowAction.PENDING
            ),
            Batch(
                id = "batch-3",
                ean = "1234567890123",
                quantity = 2,
                expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
                status = SemaphoreStatus.EXPIRED,
                name = "Product 3",
                packSize = 50,
                actionTaken = WorkflowAction.REMOVED
            )
        )

        // When: Export is called
        val file = exporter.export(batches, mockContext)

        // Then: Should contain all status enum values
        val content = file.readText()
        assertTrue(content.contains("GREEN"))
        assertTrue(content.contains("YELLOW"))
        assertTrue(content.contains("EXPIRED"))
    }

    // TEST 8: CSV file is created in cache directory
    @Test
    fun `export should create file in cache directory`() = runTest {
        // Given: Mock cache directory
        val mockCacheDir = mockContext.cacheDir

        // When: Export is called
        val file = exporter.export(testBatches, mockContext)

        // Then: File should be in exports subdirectory of cache directory
        assertEquals("exports", file.parentFile.name)
        assertEquals(mockCacheDir, file.parentFile.parentFile)

        // And: File should exist
        assertTrue(file.exists())

        // And: File should have .csv extension
        assertTrue(file.name.endsWith(".csv"))
    }

    // TEST 9: CSV should use semicolon separator
    @Test
    fun `export should use semicolon as separator`() = runTest {
        // When: Export is called
        val file = exporter.export(testBatches, mockContext)

        // Then: Header should use semicolons
        val headerLine = file.readLines()[0]
        val semicolonCount = headerLine.count { it == ';' }
        assertTrue(semicolonCount > 0)
    }

    // TEST 10: CSV should include UTF-8 BOM for Excel compatibility
    @Test
    fun `export should include UTF-8 BOM at start of file`() = runTest {
        // When: Export is called
        val file = exporter.export(testBatches, mockContext)

        // Then: File should start with UTF-8 BOM (U+FEFF)
        val bytes = file.readBytes()
        assertTrue(bytes.size >= 3)
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
    }

    // TEST 11: Export with null name and packSize
    @Test
    fun `export with null name and packSize should handle gracefully`() = runTest {
        // Given: Batch with null values
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

        // When: Export is called
        val file = exporter.export(listOf(batchWithNulls), mockContext)

        // Then: File should exist
        assertTrue(file.exists())

        // And: Content should handle nulls (should show N/A or similar)
        val content = file.readText()
        assertTrue(content.contains("N/A") || content.contains("\"\""))
    }

    // TEST 12: Date format should be dd-MM-yyyy
    @Test
    fun `export should format dates as dd-MM-yyyy`() = runTest {
        // Given: Batch with known expiry date
        val batch = Batch(
            id = "batch-date",
            ean = "1234567890123",
            quantity = 10,
            expiryDate = Instant.parse("2026-12-31T00:00:00Z"),
            status = SemaphoreStatus.GREEN,
            name = "Product",
            packSize = 1000,
            actionTaken = WorkflowAction.PENDING
        )

        // When: Export is called
        val file = exporter.export(listOf(batch), mockContext)

        // Then: Date should be in dd/MM/yyyy format
        val content = file.readText()
        assertTrue(content.contains("31/12/2026"))
    }

    // TEST 13: File name should include timestamp
    @Test
    fun `export should include timestamp in file name`() = runTest {
        // When: Export is called
        val file = exporter.export(testBatches, mockContext)

        // Then: File name should contain timestamp pattern (yyyyMMdd_HHmmss)
        val fileName = file.name
        assertTrue(fileName.matches(Regex(".*\\d{8}_\\d{6}\\.csv")))
    }

    // TEST 14: Multiple batches in CSV
    @Test
    fun `export with multiple batches should create multiple rows`() = runTest {
        // Given: 10 batches
        val batches = (1..10).map { index ->
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

        // When: Export is called
        val file = exporter.export(batches, mockContext)

        // Then: Should have 1 header + 10 data rows = 11 lines
        val lines = file.readLines()
        assertEquals(11, lines.size)
    }
}
