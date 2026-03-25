package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetSemaphoreCountersUseCaseTest {

    private lateinit var useCase: GetSemaphoreCountersUseCase
    private lateinit var mockStockRepository: StockRepository

    @Before
    fun setup() {
        mockStockRepository = mockk()
        useCase = GetSemaphoreCountersUseCase(mockStockRepository)
    }

    // TEST 1: Mixed statuses
    @Test
    fun `getSemaphoreCounters with mixed statuses should return correct counts`() = runTest {
        val counters = SemaphoreCounters(
            red = 2,
            yellow = 3,
            green = 5,
            expired = 1
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(5, received.green)
            assertEquals(3, received.yellow)
            assertEquals(2, received.red)
            assertEquals(1, received.expired)
            assertEquals(11, received.total)
        }
    }

    // TEST 2: Only red products
    @Test
    fun `getSemaphoreCounters with only red products should return correct counts`() = runTest {
        val counters = SemaphoreCounters(
            red = 4,
            yellow = 0,
            green = 0,
            expired = 0
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(0, received.green)
            assertEquals(0, received.yellow)
            assertEquals(4, received.red)
            assertEquals(0, received.expired)
            assertEquals(4, received.total)
        }
    }

    // TEST 3: Only yellow products
    @Test
    fun `getSemaphoreCounters with only yellow products should return correct counts`() = runTest {
        val counters = SemaphoreCounters(
            red = 0,
            yellow = 3,
            green = 0,
            expired = 0
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(0, received.green)
            assertEquals(3, received.yellow)
            assertEquals(0, received.red)
            assertEquals(0, received.expired)
            assertEquals(3, received.total)
        }
    }

    // TEST 4: Only green products
    @Test
    fun `getSemaphoreCounters with only green products should return correct counts`() = runTest {
        val counters = SemaphoreCounters(
            red = 0,
            yellow = 0,
            green = 5,
            expired = 0
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(5, received.green)
            assertEquals(0, received.yellow)
            assertEquals(0, received.red)
            assertEquals(0, received.expired)
            assertEquals(5, received.total)
        }
    }

    // TEST 5: Empty catalog
    @Test
    fun `getSemaphoreCounters with empty catalog should return zero counts`() = runTest {
        val counters = SemaphoreCounters(
            red = 0,
            yellow = 0,
            green = 0,
            expired = 0
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(0, received.green)
            assertEquals(0, received.yellow)
            assertEquals(0, received.red)
            assertEquals(0, received.expired)
            assertEquals(0, received.total)
        }
    }

    // TEST 6: Expired products
    @Test
    fun `getSemaphoreCounters with expired products should return correct counts`() = runTest {
        val counters = SemaphoreCounters(
            red = 0,
            yellow = 0,
            green = 0,
            expired = 3
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(0, received.green)
            assertEquals(0, received.yellow)
            assertEquals(0, received.red)
            assertEquals(3, received.expired)
            assertEquals(3, received.total)
        }
    }

    // TEST 7: All categories with values
    @Test
    fun `getSemaphoreCounters with all categories should return correct total`() = runTest {
        val counters = SemaphoreCounters(
            red = 5,
            yellow = 10,
            green = 20,
            expired = 2
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        val result = useCase()

        result.collect { received ->
            assertEquals(20, received.green)
            assertEquals(10, received.yellow)
            assertEquals(5, received.red)
            assertEquals(2, received.expired)
            assertEquals(37, received.total)
        }
    }

    // TEST 8: Verify repository is called
    @Test
    fun `getSemaphoreCounters should call stock repository`() = runTest {
        val counters = SemaphoreCounters(
            red = 1,
            yellow = 1,
            green = 1,
            expired = 1
        )

        coEvery { mockStockRepository.getSemaphoreCounters() } returns flowOf(counters)

        useCase()

        // If no exception, repository was called successfully
    }
}
