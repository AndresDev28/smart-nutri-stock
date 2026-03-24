package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class CalculateStatusUseCaseTest {

    private lateinit var useCase: CalculateStatusUseCase
    private lateinit var fixedClock: Clock

    @Before
    fun setup() {
        useCase = CalculateStatusUseCase()
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), Clock.systemUTC().zone)
    }

    /**
     * Helper to create an Instant that is [days] days in the future or past from the fixed clock time.
     */
    private fun instantDaysFromNow(days: Int): Instant {
        return fixedClock.instant().plus(Duration.ofDays(days.toLong()))
    }

    // TEST 1: Expired (days <= 0) -> EXPIRED
    @Test
    fun `calculateStatus with zero days should return EXPIRED status`() = runTest {
        val expiryDate = instantDaysFromNow(0)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.EXPIRED, result)
    }

    // TEST 2: Expired (negative days) -> EXPIRED
    @Test
    fun `calculateStatus with negative days should return EXPIRED status`() = runTest {
        val expiryDate = instantDaysFromNow(-5)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.EXPIRED, result)
    }

    // TEST 3: RED period (1-15 days)
    @Test
    fun `calculateStatus with 1 day should return RED status`() = runTest {
        val expiryDate = instantDaysFromNow(1)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.RED, result)
    }

    // TEST 4: RED period (10 days)
    @Test
    fun `calculateStatus with 10 days should return RED status`() = runTest {
        val expiryDate = instantDaysFromNow(10)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.RED, result)
    }

    // TEST 5: RED period (15 days) - boundary
    @Test
    fun `calculateStatus with 15 days should return RED status`() = runTest {
        val expiryDate = instantDaysFromNow(15)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.RED, result)
    }

    // TEST 6: YELLOW period (16 days)
    @Test
    fun `calculateStatus with 16 days should return YELLOW status`() = runTest {
        val expiryDate = instantDaysFromNow(16)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.YELLOW, result)
    }

    // TEST 7: YELLOW period (20 days)
    @Test
    fun `calculateStatus with 20 days should return YELLOW status`() = runTest {
        val expiryDate = instantDaysFromNow(20)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.YELLOW, result)
    }

    // TEST 8: YELLOW period (30 days) - boundary
    @Test
    fun `calculateStatus with 30 days should return YELLOW status`() = runTest {
        val expiryDate = instantDaysFromNow(30)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.YELLOW, result)
    }

    // TEST 9: GREEN period (31 days) - boundary
    @Test
    fun `calculateStatus with 31 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(31)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 10: GREEN period (45 days)
    @Test
    fun `calculateStatus with 45 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(45)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 11: Edge case - very large positive number
    @Test
    fun `calculateStatus with 365 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(365)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 12: Edge case - very large negative number
    @Test
    fun `calculateStatus with -365 days should return EXPIRED status`() = runTest {
        val expiryDate = instantDaysFromNow(-365)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.EXPIRED, result)
    }

    // TEST 13: Verify new thresholds (correct vs old)
    @Test
    fun `verify new thresholds RED 15 YELLOW 30 GREEN 31`() = runTest {
        // Old thresholds: 1-7 (warning), 8+ (safe)
        // New thresholds: 1-15 (RED), 16-30 (YELLOW), 31+ (GREEN)

        assertEquals(SemaphoreStatus.EXPIRED, useCase(instantDaysFromNow(0), fixedClock))
        assertEquals(SemaphoreStatus.RED, useCase(instantDaysFromNow(15), fixedClock))
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(16), fixedClock))
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(30), fixedClock))
        assertEquals(SemaphoreStatus.GREEN, useCase(instantDaysFromNow(31), fixedClock))
        assertEquals(SemaphoreStatus.GREEN, useCase(instantDaysFromNow(45), fixedClock))
    }
}
