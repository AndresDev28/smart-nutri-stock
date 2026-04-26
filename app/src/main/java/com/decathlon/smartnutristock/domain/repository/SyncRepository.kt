package com.decathlon.smartnutristock.domain.repository

import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.model.Batch
import java.time.Instant

/**
 * Repository interface for synchronization operations.
 *
 * This interface defines the contract for offline-first synchronization
 * between local Room database and remote Supabase backend.
 *
 * Key concepts:
 * - Dirty flag: Records marked isDirty=true have local changes pending sync
 * - Last-write-wins: Conflicts resolved using updatedAt timestamp
 * - Store-level isolation: All operations scoped by storeId
 */
interface SyncRepository {

    /**
     * Get all records with pending changes (dirty records).
     *
     * Retrieves records that have local changes not yet synced to the cloud.
     *
     * @param storeId Store identifier to scope the query
     * @return List of dirty records
     */
    suspend fun getDirtyRecords(storeId: String): List<Batch>

    /**
     * Push local changes to remote Supabase.
     *
     * Uploads all dirty records for the given store to Supabase.
     * Uses Postgrest client with RLS policies for security.
     *
     * @param storeId Store identifier to scope the sync
     * @return SyncResult indicating success/failure and counts
     */
    suspend fun pushDirtyRecords(storeId: String): SyncResult

    /**
     * Pull remote changes from Supabase.
     *
     * Fetches changes from Supabase since the last sync time.
     * Applies changes to local database with conflict resolution.
     *
     * @param storeId Store identifier to scope the sync
     * @param lastSyncedAt Timestamp of last successful sync
     * @return SyncResult indicating success/failure and counts
     */
    suspend fun pullRemoteChanges(storeId: String, lastSyncedAt: Instant): SyncResult

    /**
     * Mark records as synced.
     *
     * Clears dirty flag and updates synced timestamp for specified records.
     *
     * @param recordIds IDs of records to mark as synced
     * @param syncedAt Timestamp when sync occurred
     * @return Result indicating success or failure
     */
    suspend fun markAsSynced(recordIds: List<String>, syncedAt: Instant): Result<Unit>

    /**
     * Claim orphan records for the current user.
     *
     * Assigns userId and storeId to records with null/empty userId.
     * Called after successful login to associate orphan records with the user.
     *
     * @param userId User ID to assign to orphan records
     * @param storeId Store ID to assign (typically "1620")
     * @return Result containing count of updated records
     */
    suspend fun claimOrphanRecords(userId: String, storeId: String): Result<Int>

    /**
     * Get sync metadata for a store.
     *
     * Returns information about the last sync time, version, etc.
     *
     * @param storeId Store identifier
     * @return Last successful sync timestamp, or null if never synced
     */
    suspend fun getLastSyncTime(storeId: String): Instant?
}
