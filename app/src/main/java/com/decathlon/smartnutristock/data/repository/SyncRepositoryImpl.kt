package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.remote.SupabaseActiveStock
import com.decathlon.smartnutristock.data.remote.SyncRemoteDataSource
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.repository.SyncRepository
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton



/**
 * Implementation of SyncRepository using Supabase Postgrest and Room.
 *
 * This repository:
 * - Pushes dirty records to Supabase using Postgrest upsert
 * - Pulls remote changes from Supabase with storeId filtering
 * - Applies conflict resolution using version number
 * - Marks records as synced in Room after successful sync
 *
 * @property remoteDataSource Remote data source for Supabase operations
 * @property stockDao Room StockDao for local database operations
 * @property productCatalogDao Room ProductCatalogDao for product name lookups
 * @property calculateStatusUseCase Use case to calculate semaphore status
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val remoteDataSource: SyncRemoteDataSource,
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
            // IMPORTANT: Skip records without a valid product name in the catalog.
            // Pushing "Unknown" pollutes Supabase and causes "Producto desconocido" on pull.
            val supabaseRecords = dirtyEntities.mapNotNull { entity ->
                val catalogEntry = productCatalogDao.findByEan(entity.ean)
                val productName = catalogEntry?.name

                if (productName.isNullOrBlank() || productName == "Unknown") {
                    Timber.w("Sync: Skipping push for EAN ${entity.ean} — no valid product name in catalog")
                    return@mapNotNull null
                }

                val status = calculateStatusUseCase(entity.expiryDate, Clock.systemUTC())
                entity.toSupabaseFormat(productName, status.name)
            }

            if (supabaseRecords.isEmpty()) {
                Timber.i("Sync: All ${dirtyEntities.size} dirty records skipped (no valid product names)")
                return SyncResult.Success(0)
            }

            // 3. Upsert to Supabase
            remoteDataSource.upsertActiveStocks(supabaseRecords)

            // 4. Mark ONLY pushed records as synced in Room
            // Records skipped (no catalog name) stay dirty for retry on next sync cycle
            val pushedEans = supabaseRecords.map { it.ean }.toSet()
            val pushedIds = dirtyEntities
                .filter { it.ean in pushedEans }
                .map { it.id }
            val now = Instant.now(Clock.systemUTC())
            stockDao.markAsSynced(pushedIds, now)

            val skippedCount = dirtyEntities.size - supabaseRecords.size
            if (skippedCount > 0) {
                Timber.w("Sync: Pushed ${supabaseRecords.size}, skipped $skippedCount (no catalog name)")
            }

            SyncResult.Success(supabaseRecords.size)
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
            // 0. One-shot cleanup: Remove "Unknown" garbage from product_catalog
            //    (caused by the old push bug that sent "Unknown" as product_name)
            val cleaned = productCatalogDao.removeGarbageEntries()
            if (cleaned > 0) {
                Timber.w("Sync: Cleaned $cleaned garbage entries from product_catalog")
            }

            // 1. Query Supabase for changes since last sync
            val lastSyncedAtStr = lastSyncedAt.toString()

            val supabaseRecords = remoteDataSource.fetchActiveStocks(storeId, lastSyncedAtStr)

            if (supabaseRecords.isEmpty()) {
                return SyncResult.Success(0)
            }

            // 2. Apply changes to Room with conflict resolution
            var syncedCount = 0

            for (supabaseRecord in supabaseRecords) {
                try {
                    // Skip expired records — no point loading dead products into fresh install
                    val expiryDate = parseFlexibleDate(supabaseRecord.expiry_date)
                    val isExpired = expiryDate.isBefore(Instant.now(Clock.systemUTC()))
                    if (supabaseRecord.status == "EXPIRED" && isExpired) {
                        Timber.w("Sync: Skipping expired record ${supabaseRecord.id} (EAN: ${supabaseRecord.ean})")
                        continue
                    }

                    // Check if local record exists
                    val localRecord = stockDao.findById(supabaseRecord.id)

                    // UPSERT product catalog to ensure UI can display the product name.
                    // Use EAN as fallback when name is "Unknown" or blank.
                    // Never overwrite a good local name with a worse one.
                    val incomingName = supabaseRecord.product_name
                    val isValidName = incomingName.isNotBlank() && incomingName != "Unknown"
                    val displayName = if (isValidName) incomingName else "EAN: ${supabaseRecord.ean}"

                    val existingCatalog = productCatalogDao.findByEan(supabaseRecord.ean)
                    val shouldUpsert = existingCatalog == null ||
                        existingCatalog.name.isBlank() ||
                        existingCatalog.name == "Unknown" ||
                        existingCatalog.name.startsWith("EAN:")

                    if (shouldUpsert) {
                        productCatalogDao.insertOrReplace(
                            ProductCatalogEntity(
                                ean = supabaseRecord.ean,
                                name = displayName,
                                packSize = existingCatalog?.packSize ?: 1,
                                createdAt = existingCatalog?.createdAt ?: System.currentTimeMillis(),
                                createdBy = existingCatalog?.createdBy ?: 0L
                            )
                        )
                    }

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
