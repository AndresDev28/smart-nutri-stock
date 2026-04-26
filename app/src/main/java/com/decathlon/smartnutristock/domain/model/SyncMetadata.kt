package com.decathlon.smartnutristock.domain.model

/**
 * Synchronization metadata for tracking sync state.
 *
 * Contains information about when a record was last synced,
 * its version, and whether it has local changes pending sync.
 *
 * Pure Kotlin data class - NO Room or Supabase annotations.
 *
 * @property syncedAt Timestamp of last successful sync (null if never synced)
 * @property version Record version number (increments on each sync)
 * @property isDirty Flag indicating if there are local changes pending sync
 * @property deviceId Device identifier that last modified the record
 */
data class SyncMetadata(
    val syncedAt: Long? = null,
    val version: Int = 1,
    val isDirty: Boolean = false,
    val deviceId: String? = null
)
