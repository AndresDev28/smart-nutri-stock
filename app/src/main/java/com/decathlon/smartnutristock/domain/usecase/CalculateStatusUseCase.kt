package com.decathlon.smartnutristock.domain.usecase

import javax.inject.Inject

/**
 * Use case for calculating expiry status semaphore.
 *
 * Semaphore Logic:
 * - 🔴 Red: Expiration date has passed (days <= 0)
 * - 🟡 Yellow: Within warning period (1-7 days until expiry)
 * - 🟢 Green: Safe (8+ days until expiry)
 *
 * @return SemaphoreStatus with status code and days remaining
 */
class CalculateStatusUseCase @Inject constructor() {

    /**
     * Calculate status based on days until expiry.
     *
     * @param daysUntilExpiry Days until expiration (can be negative)
     * @return SemaphoreStatus with appropriate status
     */
    operator fun invoke(daysUntilExpiry: Int): SemaphoreStatus {
        return when {
            // RED: Expiration date has passed
            daysUntilExpiry <= 0 -> SemaphoreStatus.Expired(
                status = "expired",
                color = "#FF4444",  // Red
                daysUntil = daysUntilExpiry
            )

            // YELLOW: Warning period (1-7 days)
            daysUntilExpiry in 1..7 -> SemaphoreStatus.Warning(
                status = "warning",
                color = "#FFC107",  // Yellow/Orange
                daysUntil = daysUntilExpiry
            )

            // GREEN: Safe period (8+ days)
            else -> SemaphoreStatus.Safe(
                status = "safe",
                color = "#4CAF50",  // Green
                daysUntil = daysUntilExpiry
            )
        }
    }
}

/**
 * Sealed class for semaphore status.
 * Enables exhaustive `when()` expressions in Compose UI.
 */
sealed class SemaphoreStatus {
    data class Expired(val status: String, val color: String, val daysUntil: Int) : SemaphoreStatus()
    data class Warning(val status: String, val color: String, val daysUntil: Int) : SemaphoreStatus()
    data class Safe(val status: String, val color: String, val daysUntil: Int) : SemaphoreStatus()
}
