package com.decathlon.smartnutristock.domain.model

/**
 * Semaphore status for product batches based on days until expiry.
 *
 * Thresholds:
 * - EXPIRED: Already past expiry date (≤0 days)
 * - RED: ≤15 days until expiry
 * - YELLOW: 16-30 days until expiry
 * - GREEN: >30 days until expiry
 */
enum class SemaphoreStatus {
    EXPIRED,
    RED,
    YELLOW,
    GREEN
}
