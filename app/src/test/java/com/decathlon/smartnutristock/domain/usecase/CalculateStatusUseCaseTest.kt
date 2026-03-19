package com.decathlon.smartnutristock.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CalculateStatusUseCaseTest {

    private lateinit var useCase: CalculateStatusUseCase

    @Before
    fun setup() {
        useCase = CalculateStatusUseCase()
    }

    // TEST 1: Expired (days <= 0) -> Red semaphore
    @Test
    fun `calculateStatus with zero days should return Expired status`() = runTest {
        val daysUntilExpiry = 0

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Expired)
        val expired = result as SemaphoreStatus.Expired
        assert(expired.status == "expired")
        assert(expired.color == "#FF4444")
        assert(expired.daysUntil == 0)
    }

    // TEST 2: Expired (negative days) -> Red semaphore
    @Test
    fun `calculateStatus with negative days should return Expired status`() = runTest {
        val daysUntilExpiry = -5

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Expired)
        val expired = result as SemaphoreStatus.Expired
        assert(expired.status == "expired")
        assert(expired.color == "#FF4444")
        assert(expired.daysUntil == -5)
    }

    // TEST 3: Warning period (1-7 days) -> Yellow semaphore
    @Test
    fun `calculateStatus with 1 day should return Warning status`() = runTest {
        val daysUntilExpiry = 1

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Warning)
        val warning = result as SemaphoreStatus.Warning
        assert(warning.status == "warning")
        assert(warning.color == "#FFC107")
        assert(warning.daysUntil == 1)
    }

    // TEST 4: Warning period (3 days)
    @Test
    fun `calculateStatus with 3 days should return Warning status`() = runTest {
        val daysUntilExpiry = 3

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Warning)
        val warning = result as SemaphoreStatus.Warning
        assert(warning.status == "warning")
        assert(warning.color == "#FFC107")
        assert(warning.daysUntil == 3)
    }

    // TEST 5: Boundary condition (exactly 7 days) -> Yellow
    @Test
    fun `calculateStatus with 7 days should return Warning status`() = runTest {
        val daysUntilExpiry = 7

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Warning)
        val warning = result as SemaphoreStatus.Warning
        assert(warning.status == "warning")
        assert(warning.color == "#FFC107")
        assert(warning.daysUntil == 7)
    }

    // TEST 6: Safe (8+ days) -> Green semaphore
    @Test
    fun `calculateStatus with 8 days should return Safe status`() = runTest {
        val daysUntilExpiry = 8

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Safe)
        val safe = result as SemaphoreStatus.Safe
        assert(safe.status == "safe")
        assert(safe.color == "#4CAF50")
        assert(safe.daysUntil == 8)
    }

    // TEST 7: Safe (10 days)
    @Test
    fun `calculateStatus with 10 days should return Safe status`() = runTest {
        val daysUntilExpiry = 10

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Safe)
        val safe = result as SemaphoreStatus.Safe
        assert(safe.status == "safe")
        assert(safe.color == "#4CAF50")
        assert(safe.daysUntil == 10)
    }

    // TEST 8: Safe (30 days - far in the future)
    @Test
    fun `calculateStatus with 30 days should return Safe status`() = runTest {
        val daysUntilExpiry = 30

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Safe)
        val safe = result as SemaphoreStatus.Safe
        assert(safe.status == "safe")
        assert(safe.color == "#4CAF50")
        assert(safe.daysUntil == 30)
    }

    // TEST 9: Boundary condition (exactly 8 days) -> Green
    @Test
    fun `calculateStatus with exactly 8 days boundary should return Safe status`() = runTest {
        val daysUntilExpiry = 8

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Safe)
        val safe = result as SemaphoreStatus.Safe
        assert(safe.status == "safe")
        assert(safe.color == "#4CAF50")
        assert(safe.daysUntil == 8)
    }

    // TEST 10: Edge case - very large positive number
    @Test
    fun `calculateStatus with 365 days should return Safe status`() = runTest {
        val daysUntilExpiry = 365

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Safe)
        val safe = result as SemaphoreStatus.Safe
        assert(safe.status == "safe")
        assert(safe.color == "#4CAF50")
        assert(safe.daysUntil == 365)
    }

    // TEST 11: Edge case - very large negative number
    @Test
    fun `calculateStatus with -365 days should return Expired status`() = runTest {
        val daysUntilExpiry = -365

        val result = useCase(daysUntilExpiry)

        assert(result is SemaphoreStatus.Expired)
        val expired = result as SemaphoreStatus.Expired
        assert(expired.status == "expired")
        assert(expired.color == "#FF4444")
        assert(expired.daysUntil == -365)
    }

    // TEST 12: Verify status codes are correct
    @Test
    fun `calculateStatus should return correct status codes`() = runTest {
        val expired = useCase(-1) as SemaphoreStatus.Expired
        val warning = useCase(5) as SemaphoreStatus.Warning
        val safe = useCase(15) as SemaphoreStatus.Safe

        assert(expired.status == "expired")
        assert(warning.status == "warning")
        assert(safe.status == "safe")
    }

    // TEST 13: Verify color codes are correct
    @Test
    fun `calculateStatus should return correct color codes`() = runTest {
        val expired = useCase(-1) as SemaphoreStatus.Expired
        val warning = useCase(5) as SemaphoreStatus.Warning
        val safe = useCase(15) as SemaphoreStatus.Safe

        assert(expired.color == "#FF4444")  // Red
        assert(warning.color == "#FFC107")  // Yellow/Orange
        assert(safe.color == "#4CAF50")   // Green
    }
}
