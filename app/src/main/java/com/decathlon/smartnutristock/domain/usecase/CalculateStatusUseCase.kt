package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Use case for calculating expiry status semaphore based on expiry date.
 *
 * Semaphore Logic (NEW - correct thresholds):
 * - 🔴 EXPIRED: expiryDate <= Today (already past or same day)
 * - 🟡 YELLOW: Tomorrow to Today + 7 days (inclusive) - urgent attention needed
 * - 🟢 GREEN: > Today + 7 days - safe
 *
 * IMPORTANT: Uses LocalDate comparison to avoid precision issues with Instant.
 * Comparing dates (ignoring time) ensures consistent behavior regardless of time of day.
 *
 * @return SemaphoreStatus with appropriate status based on expiry date
 */
class CalculateStatusUseCase @Inject constructor() {

    /**
     * Calculate status based on expiry date.
     *
     * @param expiryDate Expiration date and time (UTC)
     * @param clock Clock to use for time calculation (default: UTC system clock)
     * @return SemaphoreStatus with appropriate status
     */
    operator fun invoke(expiryDate: Instant, clock: Clock = Clock.systemUTC()): SemaphoreStatus {
        val zoneId = ZoneId.of("UTC")
        val now = Instant.now(clock)

        // Convert both to LocalDate (date only, no time) to avoid precision issues
        val expiryLocalDate = expiryDate.atZone(zoneId).toLocalDate()
        val todayLocalDate = now.atZone(zoneId).toLocalDate()

        // Calculate days between dates (can be negative, zero, or positive)
        val daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(todayLocalDate, expiryLocalDate).toInt()

        return when {
            // EXPIRED: Today or in the past (<= 0 days)
            daysUntilExpiry <= 0 -> SemaphoreStatus.EXPIRED

            // YELLOW: Tomorrow to Today + 7 days (1-7 days) - urgent attention needed
            daysUntilExpiry in 1..7 -> SemaphoreStatus.YELLOW

            // GREEN: Beyond 7 days (>7 days) - safe
            else -> SemaphoreStatus.GREEN
        }
    }
}
