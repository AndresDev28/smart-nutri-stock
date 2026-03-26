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

    // TEST 3: YELLOW period (1 day - tomorrow)
    @Test
    fun `calculateStatus with 1 day should return YELLOW status`() = runTest {
        val expiryDate = instantDaysFromNow(1)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.YELLOW, result)
    }

    // TEST 4: YELLOW period (7 days - boundary)
    @Test
    fun `calculateStatus with 7 days should return YELLOW status`() = runTest {
        val expiryDate = instantDaysFromNow(7)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.YELLOW, result)
    }

    // TEST 5: GREEN period (8 days - boundary)
    @Test
    fun `calculateStatus with 8 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(8)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 6: GREEN period (30 days)
    @Test
    fun `calculateStatus with 30 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(30)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 7: GREEN period (45 days)
    @Test
    fun `calculateStatus with 45 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(45)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 8: Edge case - very large positive number
    @Test
    fun `calculateStatus with 365 days should return GREEN status`() = runTest {
        val expiryDate = instantDaysFromNow(365)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.GREEN, result)
    }

    // TEST 9: Edge case - very large negative number
    @Test
    fun `calculateStatus with -365 days should return EXPIRED status`() = runTest {
        val expiryDate = instantDaysFromNow(-365)

        val result = useCase(expiryDate, fixedClock)

        assertEquals(SemaphoreStatus.EXPIRED, result)
    }

    // TEST 10: Verify new thresholds (0, 1-7, 8+)
    @Test
    fun `verify new thresholds EXPIRED 0 YELLOW 7 GREEN 8`() = runTest {
        // New thresholds:
        // - EXPIRED: <= 0 days
        // - YELLOW: 1-7 days
        // - GREEN: >7 days

        assertEquals(SemaphoreStatus.EXPIRED, useCase(instantDaysFromNow(-1), fixedClock))
        assertEquals(SemaphoreStatus.EXPIRED, useCase(instantDaysFromNow(0), fixedClock))
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(1), fixedClock))
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(5), fixedClock))
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(7), fixedClock))
        assertEquals(SemaphoreStatus.GREEN, useCase(instantDaysFromNow(8), fixedClock))
        assertEquals(SemaphoreStatus.GREEN, useCase(instantDaysFromNow(45), fixedClock))
    }

    // TEST 11: Precision bug - Instant with time components causes incorrect status
    @Test
    fun `precision bug - expiry at midnight today, current time late at night should not be EXPIRED`() = runTest {
        // Current time: Jan 1, 2024 at 23:59:59.999Z (almost midnight)
        val now = Instant.parse("2024-01-01T23:59:59.999Z")
        val clock = Clock.fixed(now, Clock.systemUTC().zone)

        // Expiry date: Jan 2, 2024 at 00:00:00Z (midnight next day)
        // This should be "tomorrow" (YELLOW), not EXPIRED
        val expiryDate = Instant.parse("2024-01-02T00:00:00Z")

        // Duration.between(23:59:59.999, 00:00:00.000).toDays() returns 0 (less than 24 hours)
        // This incorrectly marks it as EXPIRED when it's actually tomorrow
        val result = useCase(expiryDate, clock)

        // After fixing with LocalDate, this should return YELLOW (within 7 days)
        assertEquals(SemaphoreStatus.YELLOW, result)
    }

    // TEST 12: Precision bug - same day different times
    @Test
    fun `precision bug - expiry in evening, current time in morning same day should be EXPIRED`() = runTest {
        // Current time: Jan 1, 2024 at 09:00:00Z (morning)
        val now = Instant.parse("2024-01-01T09:00:00Z")
        val clock = Clock.fixed(now, Clock.systemUTC().zone)

        // Expiry date: Jan 1, 2024 at 20:00:00Z (evening same day)
        // Both are same day (Jan 1), so it should be EXPIRED
        val expiryDate = Instant.parse("2024-01-01T20:00:00Z")

        val result = useCase(expiryDate, clock)

        // After fixing with LocalDate, same day = EXPIRED
        assertEquals(SemaphoreStatus.EXPIRED, result)
    }

    // TEST 13: Boundary tests for new thresholds (Yesterday, Today, Tomorrow, In 7 Days, In 8 Days)
    @Test
    fun `boundary tests - Yesterday, Today, Tomorrow, In 7 Days, In 8 Days`() = runTest {
        // Yesterday -> EXPIRED
        assertEquals(SemaphoreStatus.EXPIRED, useCase(instantDaysFromNow(-1), fixedClock))

        // Today -> EXPIRED
        assertEquals(SemaphoreStatus.EXPIRED, useCase(instantDaysFromNow(0), fixedClock))

        // Tomorrow -> YELLOW (within 7 days)
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(1), fixedClock))

        // In 7 days -> YELLOW (boundary, within 7 days)
        assertEquals(SemaphoreStatus.YELLOW, useCase(instantDaysFromNow(7), fixedClock))

        // In 8 days -> GREEN (beyond 7 days)
        assertEquals(SemaphoreStatus.GREEN, useCase(instantDaysFromNow(8), fixedClock))
    }
}
