package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for calculating expiry status semaphore based on expiry date.
 *
 * Semaphore Logic (NEW - correct thresholds):
 * - 🔴 EXPIRED: Already past expiry date (≤0 days)
 * - 🔴 RED: ≤15 days until expiry
 * - 🟡 YELLOW: 16-30 days until expiry
 * - 🟢 GREEN: >30 days until expiry
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
        val now = Instant.now(clock)
        val daysUntilExpiry = Duration.between(now, expiryDate).toDays().toInt()

        return when {
            // EXPIRED: Already past expiry date
            daysUntilExpiry <= 0 -> SemaphoreStatus.EXPIRED

            // RED: High priority - ≤15 days until expiry
            daysUntilExpiry in 1..15 -> SemaphoreStatus.RED

            // YELLOW: Medium priority - 16-30 days until expiry
            daysUntilExpiry in 16..30 -> SemaphoreStatus.YELLOW

            // GREEN: Low priority - >30 days until expiry
            else -> SemaphoreStatus.GREEN
        }
    }
}
