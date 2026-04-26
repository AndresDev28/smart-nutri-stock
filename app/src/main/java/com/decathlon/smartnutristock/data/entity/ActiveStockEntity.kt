package com.decathlon.smartnutristock.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity representing an active stock batch.
 *
 * Stores batches with their EAN code, quantity, expiry date, timestamps, and workflow action.
 * Each batch is uniquely identified by a UUID (id) and has a composite unique index
 * on (ean, expiryDate) to ensure no duplicate batches for same product and expiry.
 *
 * Sync metadata (userId, storeId, syncedAt, version, isDirty) added in v6 for
 * multi-device synchronization with Supabase.
 *
 * @property id Unique UUID identifier for this batch (PRIMARY KEY)
 * @property ean 13-digit EAN code of product
 * @property quantity Number of units in this batch
 * @property expiryDate Expiry date of this batch (UTC)
 * @property createdAt Timestamp when this batch was first created
 * @property updatedAt Timestamp when this batch was last modified
 * @property deletedAt Timestamp when batch was soft-deleted (null if active)
 * @property actionTaken Workflow action taken on this batch (PENDING, DISCOUNTED, REMOVED)
 * @property userId User ID who created/modified this batch (nullable for orphan cleanup)
 * @property storeId Store ID for multitenancy (default "1620" for Decathlon Gandía)
 * @property syncedAt Timestamp when this batch was last synced with Supabase (null if never synced)
 * @property version Optimistic lock version for conflict resolution (default 1)
 * @property deviceId Device ID that created/modified this batch
 * @property isDirty Flag indicating if this batch has unsynced changes (0 = synced, 1 = dirty)
 */
@Entity(
    tableName = "active_stocks",
    indices = [
        Index(
            value = ["ean", "expiryDate"],
            unique = true
        ),
        Index(value = ["deletedAt"]),
        Index(value = ["userId"]),
        Index(value = ["storeId"]),
        Index(value = ["isDirty"])
    ]
)
data class ActiveStockEntity(
    @PrimaryKey
    val id: String,

    val ean: String,
    val quantity: Int,
    val expiryDate: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val actionTaken: String = "PENDING",
    val userId: String? = null,
    val storeId: String = "1620",
    val syncedAt: Instant? = null,
    val version: Int = 1,
    val deviceId: String? = null,
    val isDirty: Int = 1  // Default to 1 so existing records are marked for sync
)
