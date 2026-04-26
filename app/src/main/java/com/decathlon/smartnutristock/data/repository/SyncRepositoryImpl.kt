package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.repository.SyncRepository
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase row format for sync operations.
 *
 * Matches the active_stocks table schema in Supabase exactly.
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
private data class SupabaseActiveStock(
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

/**
 * Implementation of SyncRepository using Supabase Postgrest and Room.
 *
 * This repository:
 * - Pushes dirty records to Supabase using Postgrest upsert
 * - Pulls remote changes from Supabase with storeId filtering
 * - Applies conflict resolution using version number
 * - Marks records as synced in Room after successful sync
 *
 * @property postgrest Supabase Postgrest client
 * @property stockDao Room StockDao for local database operations
 * @property productCatalogDao Room ProductCatalogDao for product name lookups
 * @property calculateStatusUseCase Use case to calculate semaphore status
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val stockDao: StockDao,
    private val productCatalogDao: ProductCatalogDao,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : SyncRepository {

    /**
     * Get all records with pending sync (dirty records).
     *
     * Converts Room entities to domain Batch models.
     *
     * @param storeId Store identifier to scope the query (filter is applied in Room query)
     * @return List of dirty records as Batch domain models
     */
    override suspend fun getDirtyRecords(storeId: String): List<Batch> {
        val dirtyEntities = stockDao.getDirtyRecords(storeId).first()
        return dirtyEntities.map { it.toBatch(calculateStatusUseCase) }
    }

    /**
     * Push local changes to remote Supabase.
     *
     * Process:
     * 1. Get dirty records from Room
     * 2. Map to Supabase format
     * 3. Upsert to Supabase Postgrest (insert or update if exists)
     * 4. Mark records as synced in Room
     *
     * Uses Postgrest .upsert() which will INSERT new records or UPDATE existing ones
     * based on the primary key (id) or unique constraints (ean + expiry_date).
     *
     * @param storeId Store identifier to scope the sync
     * @return SyncResult indicating success/failure and counts
     */
    override suspend fun pushDirtyRecords(storeId: String): SyncResult {
        return try {
            // 0. Sanitize storeIds with literal JSON quotes (one-shot fix for toString() bug)
            val sanitized = stockDao.sanitizeStoreIds()
            if (sanitized > 0) {
                Timber.w("Sync: Sanitized $sanitized records with quoted storeId values")
            }

            // 1. Get dirty records
            val dirtyEntities = stockDao.getDirtyRecords(storeId).first()

            if (dirtyEntities.isEmpty()) {
                return SyncResult.Success(0)
            }

            // 2. Map to Supabase format (resolve product names from catalog)
            val supabaseRecords = dirtyEntities.map { entity ->
                val productName = productCatalogDao.findByEan(entity.ean)?.name ?: "Unknown"
                val status = calculateStatusUseCase(entity.expiryDate, Clock.systemUTC())
                entity.toSupabaseFormat(productName, status.name)
            }

            // 3. Upsert to Supabase
            postgrest["active_stocks"].upsert(supabaseRecords)

            // 4. Mark as synced in Room
            val recordIds = dirtyEntities.map { entity -> entity.id }
            val now = Instant.now(Clock.systemUTC())
            stockDao.markAsSynced(recordIds, now)

            SyncResult.Success(dirtyEntities.size)
        } catch (e: Exception) {
            Timber.e(e, "Sync: Failed to push dirty records")
            Timber.tag("SYNC_ERROR").e(e, "Error subiendo datos: ${e.message}")
            SyncResult.Error(
                message = "Failed to push records to cloud",
                cause = e
            )
        }
    }

    /**
     * Pull remote changes from Supabase.
     *
     * Process:
     * 1. Query Supabase for records with synced_at > lastSyncedAt and matching storeId
     * 2. Map to Room entities
     * 3. Upsert to Room with conflict resolution (last-write-wins via updatedAt)
     *
     * @param storeId Store identifier to scope the sync
     * @param lastSyncedAt Timestamp of last successful sync
     * @return SyncResult indicating success/failure and counts
     */
    override suspend fun pullRemoteChanges(storeId: String, lastSyncedAt: Instant): SyncResult {
        return try {
            // 1. Query Supabase for changes since last sync
            val lastSyncedAtStr = lastSyncedAt.toString()

            val response = postgrest["active_stocks"]
                .select {
                    filter {
                        eq("store_id", storeId)
                        gt("synced_at", lastSyncedAtStr)
                    }
                }

            val supabaseRecords = response.decodeList<SupabaseActiveStock>()

            if (supabaseRecords.isEmpty()) {
                return SyncResult.Success(0)
            }

            // 2. Apply changes to Room with conflict resolution
            var syncedCount = 0

            for (supabaseRecord in supabaseRecords) {
                try {
                    // Check if local record exists
                    val localRecord = stockDao.findById(supabaseRecord.id)

                    if (localRecord != null) {
                        // Conflict resolution: higher version wins
                        if (supabaseRecord.version > localRecord.version) {
                            // Remote is newer - apply update
                            val updatedEntity = supabaseRecord.toEntity()
                            stockDao.update(updatedEntity)
                            syncedCount++
                        }
                        // Local is newer or equal - skip (do nothing)
                    } else {
                        // No local record - insert new
                        val newEntity = supabaseRecord.toEntity()
                        stockDao.insert(newEntity)
                        syncedCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Sync: Failed to apply remote change for batch ${supabaseRecord.id}")
                }
            }

            SyncResult.Success(syncedCount)
        } catch (e: Exception) {
            Timber.e(e, "Sync: Failed to pull remote changes")
            Timber.tag("SYNC_ERROR").e(e, "Error bajando datos: ${e.message}")
            SyncResult.Error(
                message = "Failed to pull changes from cloud",
                cause = e
            )
        }
    }

    /**
     * Mark records as synced.
     *
     * Clears dirty flag and updates synced timestamp for specified records.
     *
     * @param recordIds IDs of records to mark as synced
     * @param syncedAt Timestamp when sync occurred
     * @return Result indicating success or failure
     */
    override suspend fun markAsSynced(recordIds: List<String>, syncedAt: Instant): Result<Unit> {
        return try {
            stockDao.markAsSynced(recordIds, syncedAt)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Sync: Failed to mark records as synced")
            Result.failure(e)
        }
    }

    /**
     * Claim orphan records for the current user.
     *
     * Assigns userId and storeId to records with null or empty userId.
     * Called after successful login to associate orphan records with the user.
     *
     * @param userId User ID to assign to orphan records
     * @param storeId Store ID to assign (typically "1620")
     * @return Result containing count of updated records
     */
    override suspend fun claimOrphanRecords(userId: String, storeId: String): Result<Int> {
        return try {
            Timber.i("Orphan cleanup: Claiming records for userId=$userId, storeId=$storeId")
            val updatedCount = stockDao.claimOrphanRecords(userId, storeId)
            Timber.i("Orphan cleanup: Updated $updatedCount orphan records")
            Result.success(updatedCount)
        } catch (e: Exception) {
            Timber.e(e, "Orphan cleanup: Failed to claim orphan records")
            Result.failure(e)
        }
    }

    /**
     * Get sync metadata for a store.
     *
     * Returns information about the last sync time by finding the most recent
     * syncedAt timestamp for the given store.
     *
     * @param storeId Store identifier
     * @return Last successful sync timestamp, or null if never synced
     */
    override suspend fun getLastSyncTime(storeId: String): Instant? {
        return try {
            val entities = stockDao.getAllByStore(storeId).first()
            if (entities.isEmpty()) {
                null
            } else {
                // Find the most recent syncedAt timestamp
                entities.mapNotNull { entity -> entity.syncedAt }.maxOrNull()
            }
        } catch (e: Exception) {
            Timber.e(e, "Sync: Failed to get last sync time")
            null
        }
    }

    // ============================================================================
    // Extension functions for mapping between Room, Domain, and Supabase formats
    // ============================================================================

    /**
     * Convert ActiveStockEntity to SupabaseActiveStock format.
     *
     * @param productName Product name resolved from the catalog
     * @param status Calculated semaphore status (GREEN, YELLOW, EXPIRED)
     */
    private fun ActiveStockEntity.toSupabaseFormat(
        productName: String,
        status: String
    ): SupabaseActiveStock {
        return SupabaseActiveStock(
            id = this.id,
            ean = this.ean,
            product_name = productName,
            quantity = this.quantity,
            expiry_date = this.expiryDate.atZone(ZoneId.of("UTC")).toLocalDate().toString(),
            status = status,
            action_taken = this.actionTaken,
            user_id = this.userId,
            store_id = this.storeId,
            version = this.version,
            is_dirty = this.isDirty,
            synced_at = this.syncedAt?.toString(),
            created_at = this.createdAt.toString()
        )
    }

    private fun parseFlexibleDate(value: String): Instant {
        return try {
            Instant.parse(value)
        } catch (_: Exception) {
            LocalDate.parse(value).atStartOfDay(ZoneId.of("UTC")).toInstant()
        }
    }

    /**
     * Convert SupabaseActiveStock to ActiveStockEntity format.
     */
    private fun SupabaseActiveStock.toEntity(): ActiveStockEntity {
        val now = Instant.now(Clock.systemUTC())
        return ActiveStockEntity(
            id = this.id,
            ean = this.ean,
            quantity = this.quantity,
            expiryDate = parseFlexibleDate(this.expiry_date),
            createdAt = this.created_at?.let { parseFlexibleDate(it) } ?: now,
            updatedAt = now,
            deletedAt = null,
            actionTaken = this.action_taken,
            userId = this.user_id,
            storeId = this.store_id,
            syncedAt = this.synced_at?.let { parseFlexibleDate(it) },
            version = this.version,
            deviceId = null,
            isDirty = this.is_dirty
        )
    }

    /**
     * Convert ActiveStockEntity to Batch domain model.
     */
    private fun ActiveStockEntity.toBatch(calculateStatusUseCase: CalculateStatusUseCase): Batch {
        val clock = Clock.systemUTC()
        return Batch(
            id = this.id,
            ean = this.ean,
            quantity = this.quantity,
            expiryDate = this.expiryDate,
            status = calculateStatusUseCase(this.expiryDate, clock),
            deletedAt = this.deletedAt,
            actionTaken = com.decathlon.smartnutristock.domain.model.WorkflowAction.valueOf(this.actionTaken)
        )
    }
}
