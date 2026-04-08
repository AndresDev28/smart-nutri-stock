package com.decathlon.smartnutristock.data.entity

import java.time.Instant

/**
 * DTO representing a batch joined with its product catalog information.
 *
 * This class is used for Room queries that JOIN active_stocks with product_catalog
 * to get complete batch information including product name and pack size.
 *
 * @property id Unique identifier for this batch
 * @property ean 13-digit EAN code of product
 * @property quantity Number of units in this batch
 * @property expiryDate Expiry date of this batch (UTC)
 * @property createdAt Timestamp when this batch was first created
 * @property updatedAt Timestamp when this batch was last modified
 * @property productName Product name (nullable - may not exist in catalog)
 * @property packSize Pack size in grams (nullable - may not exist in catalog)
 * @property actionTaken Workflow action taken on this batch (PENDING, DISCOUNTED, REMOVED)
 */
data class BatchWithProductInfo(
    val id: String,
    val ean: String,
    val quantity: Int,
    val expiryDate: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val productName: String?,
    val packSize: Int?,
    val actionTaken: String = "PENDING"
) {
    /**
     * Convert to Batch domain model with enriched product information.
     */
    fun toDomainModel(
        status: com.decathlon.smartnutristock.domain.model.SemaphoreStatus
    ): com.decathlon.smartnutristock.domain.model.Batch {
        return com.decathlon.smartnutristock.domain.model.Batch(
            id = id,
            ean = ean,
            quantity = quantity,
            expiryDate = expiryDate,
            status = status,
            name = productName,
            packSize = packSize,
            deletedAt = null, // Active batches (from findAllWithProductInfo) have deletedAt = NULL
            actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.valueOf(actionTaken)
        )
    }
}
