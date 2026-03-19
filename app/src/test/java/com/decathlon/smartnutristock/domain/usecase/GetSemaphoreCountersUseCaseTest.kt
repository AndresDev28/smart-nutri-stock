package com.decathlon.smartnutristock.domain.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import java.time.Instant

class GetSemaphoreCountersUseCaseTest {

    private lateinit var useCase: GetSemaphoreCountersUseCase
    private lateinit var mockRepository: ProductRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = GetSemaphoreCountersUseCase(mockRepository)
    }

    // TEST 1: All products with all status types
    @Test
    fun `getSemaphoreCounters with mixed statuses should return correct counts`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = -5),  // Red (expired)
            createProduct(ean = "2", daysUntil = 3),   // Yellow (warning)
            createProduct(ean = "3", daysUntil = 10)   // Green (safe)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 1)
            assert(counters.yellow == 1)
            assert(counters.red == 1)
            assert(counters.total == 3)
        }
    }

    // TEST 2: Only red products
    @Test
    fun `getSemaphoreCounters with only red products should return correct counts`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = -10),
            createProduct(ean = "2", daysUntil = 0),
            createProduct(ean = "3", daysUntil = -5)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 0)
            assert(counters.yellow == 0)
            assert(counters.red == 3)
            assert(counters.total == 3)
        }
    }

    // TEST 3: Only yellow products
    @Test
    fun `getSemaphoreCounters with only yellow products should return correct counts`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = 1),
            createProduct(ean = "2", daysUntil = 5),
            createProduct(ean = "3", daysUntil = 7)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 0)
            assert(counters.yellow == 3)
            assert(counters.red == 0)
            assert(counters.total == 3)
        }
    }

    // TEST 4: Only green products
    @Test
    fun `getSemaphoreCounters with only green products should return correct counts`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = 8),
            createProduct(ean = "2", daysUntil = 15),
            createProduct(ean = "3", daysUntil = 30)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 3)
            assert(counters.yellow == 0)
            assert(counters.red == 0)
            assert(counters.total == 3)
        }
    }

    // TEST 5: Mixed statuses
    @Test
    fun `getSemaphoreCounters with many mixed products should return correct counts`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = -10), // Red
            createProduct(ean = "2", daysUntil = -1),  // Red
            createProduct(ean = "3", daysUntil = 0),   // Red
            createProduct(ean = "4", daysUntil = 1),   // Yellow
            createProduct(ean = "5", daysUntil = 3),   // Yellow
            createProduct(ean = "6", daysUntil = 5),   // Yellow
            createProduct(ean = "7", daysUntil = 7),   // Yellow
            createProduct(ean = "8", daysUntil = 8),   // Green
            createProduct(ean = "9", daysUntil = 15),  // Green
            createProduct(ean = "10", daysUntil = 30)  // Green
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 3)
            assert(counters.yellow == 4)
            assert(counters.red == 3)
            assert(counters.total == 10)
        }
    }

    // TEST 6: Empty catalog
    @Test
    fun `getSemaphoreCounters with empty catalog should return zero counts`() = runTest {
        val products = emptyList<ProductCatalogEntity>()

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 0)
            assert(counters.yellow == 0)
            assert(counters.red == 0)
            assert(counters.total == 0)
        }
    }

    // TEST 7: Boundary test - exactly 7 days (yellow)
    @Test
    fun `getSemaphoreCounters with exactly 7 days should count as yellow`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = 7)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 0)
            assert(counters.yellow == 1)
            assert(counters.red == 0)
        }
    }

    // TEST 8: Boundary test - exactly 8 days (green)
    @Test
    fun `getSemaphoreCounters with exactly 8 days should count as green`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = 8)
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 1)
            assert(counters.yellow == 0)
            assert(counters.red == 0)
        }
    }

    // TEST 9: Single product in each category
    @Test
    fun `getSemaphoreCounters with one in each category`() = runTest {
        val products = listOf(
            createProduct(ean = "1", daysUntil = -5), // Red
            createProduct(ean = "2", daysUntil = 4),  // Yellow
            createProduct(ean = "3", daysUntil = 12)  // Green
        )

        every { mockRepository.getAllProducts() } returns flowOf(products)

        val result = useCase()

        result.collect { counters ->
            assert(counters.green == 1)
            assert(counters.yellow == 1)
            assert(counters.red == 1)
            assert(counters.total == 3)
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
}
