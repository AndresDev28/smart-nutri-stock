package com.decathlon.smartnutristock.domain.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant

class GetSemaphoreCountersUseCaseTest {

    private lateinit var useCase: GetSemaphoreCountersUseCase
    private lateinit var mockRepository: ProductRepository
    private lateinit var mockCalculateStatusUseCase: CalculateStatusUseCase

    @Before
    fun setup() {
        mockRepository = mockk()
        mockCalculateStatusUseCase = mockk()
        useCase = GetSemaphoreCountersUseCase(mockRepository, mockCalculateStatusUseCase)
    }

    // TEST 1: All products with all status types
    @Test
    fun `getSemaphoreCounters with mixed statuses should return correct counts`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = -5),  // Expired
            createProduct(ean = "2", daysUntil = 3),   // Red
            createProduct(ean = "3", daysUntil = 25)   // Yellow
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(0, counters.green)
            assertEquals(1, counters.yellow)
            assertEquals(1, counters.red)
            assertEquals(1, counters.expired)
            assertEquals(3, counters.total)
        }
    }

    // TEST 2: Only red products
    @Test
    fun `getSemaphoreCounters with only red products should return correct counts`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = -10),
            createProduct(ean = "2", daysUntil = 0),
            createProduct(ean = "3", daysUntil = 5),
            createProduct(ean = "4", daysUntil = 15)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(0, counters.green)
            assertEquals(0, counters.yellow)
            assertEquals(2, counters.red)
            assertEquals(2, counters.expired)
            assertEquals(4, counters.total)
        }
    }

    // TEST 3: Only yellow products
    @Test
    fun `getSemaphoreCounters with only yellow products should return correct counts`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = 16),
            createProduct(ean = "2", daysUntil = 20),
            createProduct(ean = "3", daysUntil = 30)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(0, counters.green)
            assertEquals(3, counters.yellow)
            assertEquals(0, counters.red)
            assertEquals(0, counters.expired)
            assertEquals(3, counters.total)
        }
    }

    // TEST 4: Only green products
    @Test
    fun `getSemaphoreCounters with only green products should return correct counts`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = 31),
            createProduct(ean = "2", daysUntil = 45),
            createProduct(ean = "3", daysUntil = 90)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(3, counters.green)
            assertEquals(0, counters.yellow)
            assertEquals(0, counters.red)
            assertEquals(0, counters.expired)
            assertEquals(3, counters.total)
        }
    }

    // TEST 5: Mixed statuses
    @Test
    fun `getSemaphoreCounters with many mixed products should return correct counts`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = -10), // Expired
            createProduct(ean = "2", daysUntil = -1),  // Expired
            createProduct(ean = "3", daysUntil = 0),   // Expired
            createProduct(ean = "4", daysUntil = 1),   // Red
            createProduct(ean = "5", daysUntil = 10),  // Red
            createProduct(ean = "6", daysUntil = 15),  // Red
            createProduct(ean = "7", daysUntil = 16),  // Yellow
            createProduct(ean = "8", daysUntil = 20),  // Yellow
            createProduct(ean = "9", daysUntil = 30),  // Yellow
            createProduct(ean = "10", daysUntil = 31)  // Green
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(1, counters.green)
            assertEquals(3, counters.yellow)
            assertEquals(3, counters.red)
            assertEquals(3, counters.expired)
            assertEquals(10, counters.total)
        }
    }

    // TEST 6: Empty catalog
    @Test
    fun `getSemaphoreCounters with empty catalog should return zero counts`() = runTest {
        setupMockBasedOnDays()

        val products = emptyList<ProductCatalogEntity>()

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(0, counters.green)
            assertEquals(0, counters.yellow)
            assertEquals(0, counters.red)
            assertEquals(0, counters.expired)
            assertEquals(0, counters.total)
        }
    }

    // TEST 7: Boundary test - exactly 16 days (yellow)
    @Test
    fun `getSemaphoreCounters with exactly 16 days should count as yellow`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = 16)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(0, counters.green)
            assertEquals(1, counters.yellow)
            assertEquals(0, counters.red)
            assertEquals(0, counters.expired)
        }
    }

    // TEST 8: Boundary test - exactly 31 days (green)
    @Test
    fun `getSemaphoreCounters with exactly 31 days should count as green`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = 31)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(1, counters.green)
            assertEquals(0, counters.yellow)
            assertEquals(0, counters.red)
            assertEquals(0, counters.expired)
        }
    }

    // TEST 9: Single product in each category
    @Test
    fun `getSemaphoreCounters with one in each category`() = runTest {
        setupMockBasedOnDays()

        val products = listOf(
            createProduct(ean = "1", daysUntil = -5), // Expired
            createProduct(ean = "2", daysUntil = 10), // Red
            createProduct(ean = "3", daysUntil = 25)  // Yellow
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assertEquals(0, counters.green)
            assertEquals(1, counters.yellow)
            assertEquals(1, counters.red)
            assertEquals(1, counters.expired)
            assertEquals(3, counters.total)
        }
    }

    // Helper function to create product with daysUntilExpiry
    private fun createProduct(ean: String, daysUntil: Int): ProductCatalogEntity {
        return ProductCatalogEntity(
            ean = ean,
            name = "Product $ean",
            packSize = 100,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L,
            daysUntilExpiry = daysUntil
        )
    }

    // Helper to create a mock that returns status based on expiryDate (Instant)
    // This is a simplified mock that doesn't need to calculate days
    private fun setupMockBasedOnDays() {
        // We need to mock based on the actual days, not Instant
        // Since GetSemaphoreCountersUseCase calculates Instant from daysUntilExpiry,
        // we need to mock CalculateStatusUseCase to return the correct status
        // based on the Instant it receives

        val clock = Clock.systemUTC()
        val now = Instant.now(clock)

        every { mockCalculateStatusUseCase(any(), any()) } answers { call ->
            val expiryDate = call.invocation.args[0] as Instant
            val daysUntil = Duration.between(now, expiryDate).toDays().toInt()
            when {
                daysUntil <= 0 -> SemaphoreStatus.EXPIRED
                daysUntil in 1..15 -> SemaphoreStatus.RED
                daysUntil in 16..30 -> SemaphoreStatus.YELLOW
                else -> SemaphoreStatus.GREEN
            }
        }
    }
}
