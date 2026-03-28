package com.decathlon.smartnutristock.domain.model

/**
 * Semaphore status for product batches based on days until expiry.
 *
 * Thresholds (using LocalDate comparison to avoid precision issues):
 * - EXPIRED: expiryDate <= Today (already past or same day)
 * - YELLOW: Tomorrow to Today + 7 days (inclusive) - urgent attention needed
 * - GREEN: > Today + 7 days - safe
 */
enum class SemaphoreStatus {
    EXPIRED,
    YELLOW,
    GREEN
}
