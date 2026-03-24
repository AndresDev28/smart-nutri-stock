package com.decathlon.smartnutristock.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing an active stock batch.
 *
 * Stores batches with their EAN code, quantity, expiry date, and timestamps.
 * Each batch is uniquely identified by a UUID (id) and has a composite unique index
 * on (ean, expiryDate) to ensure no duplicate batches for same product and expiry.
 *
 * @property id Unique UUID identifier for this batch (PRIMARY KEY)
 * @property ean 13-digit EAN code of product
 * @property quantity Number of units in this batch
 * @property expiryDate Expiry date of this batch (UTC)
 * @property createdAt Timestamp when this batch was first created
 * @property updatedAt Timestamp when this batch was last modified
 */
@Entity(
    tableName = "active_stocks",
    indices = [
        Index(
            value = ["ean", "expiryDate"],
            unique = true
        )
    ]
)
data class ActiveStockEntity(
    @PrimaryKey
    val id: String,

    val ean: String,
    val quantity: Int,
    val expiryDate: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
