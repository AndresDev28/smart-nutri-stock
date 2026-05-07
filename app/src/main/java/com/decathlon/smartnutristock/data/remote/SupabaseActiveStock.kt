package com.decathlon.smartnutristock.data.remote

import kotlinx.serialization.Serializable

/**
 * Supabase row format for sync operations.
 *
 * Matches the active_stocks table schema in Supabase exactly.
 * Lives here (not in the repository) because it's part of the remote data contract.
 *
 * @property id Unique identifier for this batch (uuid)
 * @property ean 13-digit EAN code of product
 * @property product_name Name of the product from the catalog
 * @property quantity Number of units in this batch
 * @property expiry_date Expiry date of this batch (ISO-8601 date string, e.g., "2026-04-23")
 * @property status Semaphore status (GREEN, YELLOW, EXPIRED)
 * @property action_taken Workflow action taken on this batch (PENDING, DISCOUNTED, REMOVED)
 * @property user_id User ID who created/modified this batch (uuid)
 * @property store_id Store ID for multitenancy (default "1620" for Decathlon Gandía)
 * @property version Optimistic lock version for conflict resolution (default 1)
 * @property is_dirty Flag indicating if this batch has unsynced changes (0 = synced, 1 = dirty)
 * @property synced_at Timestamp when this batch was last synced with Supabase (ISO-8601 string)
 * @property created_at Timestamp when this batch was first created (ISO-8601 string)
 */
@Serializable
data class SupabaseActiveStock(
    val id: String,
    val ean: String,
    val product_name: String,
    val quantity: Int,
    val expiry_date: String,
    val status: String,
    val action_taken: String = "PENDING",
    val user_id: String? = null,
    val store_id: String = "1620",
    val version: Int = 1,
    val is_dirty: Int = 0,
    val synced_at: String? = null,
    val created_at: String? = null
)
