package com.decathlon.smartnutristock.domain.model

/**
 * Semaphore status for product batches based on days until expiry.
 *
 * Thresholds (using LocalDate comparison to avoid precision issues):
 * - EXPIRED: expiryDate <= Today (already past or same day)
 * - YELLOW: Tomorrow to Today + 7 days (inclusive) - urgent attention needed
 * - GREEN: > Today + 7 days - safe
 *
 * Note: RED is deprecated, use EXPIRED for expired/urgent items.
 */
enum class SemaphoreStatus {
    EXPIRED,
    @Deprecated("Use EXPIRED instead - same UI color")
    RED,
    YELLOW,
    GREEN
}
