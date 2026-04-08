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
 * @property name Product name (nullable - from product_catalog)
 * @property packSize Pack size in grams (nullable - from product_catalog)
 * @property deletedAt Timestamp when batch was soft-deleted (null if active)
 * @property actionTaken Workflow action taken on this batch (default: PENDING)
 */
data class Batch(
    val id: String,
    val ean: String,
    val quantity: Int,
    val expiryDate: Instant,
    val status: SemaphoreStatus,
    val name: String? = null,
    val packSize: Int? = null,
    val deletedAt: Instant? = null,
    val actionTaken: WorkflowAction = WorkflowAction.PENDING
)
