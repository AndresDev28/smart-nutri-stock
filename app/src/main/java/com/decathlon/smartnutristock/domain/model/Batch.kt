package com.decathlon.smartnutristock.domain.model

import java.time.Instant

/**
 * Domain model representing a product batch with its stock quantity and expiry status.
 *
 * @property id Unique identifier for this batch
 * @property ean 13-digit EAN code of product
 * @property quantity Number of units in this batch
 * @property expiryDate Expiry date of this batch (UTC)
 * @property status Calculated semaphore status based on expiry date
 */
data class Batch(
    val id: String,
    val ean: String,
    val quantity: Int,
    val expiryDate: Instant,
    val status: SemaphoreStatus
)
