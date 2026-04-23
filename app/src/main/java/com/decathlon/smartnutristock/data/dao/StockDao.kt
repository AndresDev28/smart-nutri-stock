package com.decathlon.smartnutristock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.BatchWithProductInfo
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data Access Object for ActiveStockEntity.
 *
 * Provides CRUD operations for batch management with lookup by composite key (ean, expiryDate).
 */
@Dao
interface StockDao {

    /**
     * Insert a new batch into database.
     *
     * @param activeStock The batch to insert
     * @return The row ID of inserted batch
     * @throws SQLiteConstraintException if a batch with same (ean, expiryDate) already exists
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(activeStock: ActiveStockEntity): Long

    /**
     * Update an existing batch.
     *
     * @param activeStock The batch to update (matched by primary key id)
     * @return Number of rows affected (1 if updated, 0 if not found)
     */
    @Update
    suspend fun update(activeStock: ActiveStockEntity): Int

    /**
     * Find a batch by its composite key (EAN + expiry date).
     *
     * @param ean The EAN code
     * @param expiryDate The expiry date
     * @return The batch if found, null otherwise
     */
    @Query("SELECT * FROM active_stocks WHERE ean = :ean AND expiryDate = :expiryDate AND deletedAt IS NULL LIMIT 1")
    suspend fun findByEanAndExpiryDate(ean: String, expiryDate: Instant): ActiveStockEntity?

    /**
     * Find all batches for a given EAN code.
     *
     * @param ean The EAN code
     * @return List of all batches with this EAN, ordered by expiry date
     */
    @Query("SELECT * FROM active_stocks WHERE ean = :ean AND deletedAt IS NULL ORDER BY expiryDate ASC")
    suspend fun findByEan(ean: String): List<ActiveStockEntity>

    /**
     * Retrieve all batches in database.
     *
     * @return List of all batches
     */
    @Query("SELECT * FROM active_stocks WHERE deletedAt IS NULL ORDER BY expiryDate ASC")
    suspend fun findAll(): List<ActiveStockEntity>

    /**
     * Delete a batch by its composite key (EAN + expiry date).
     *
     * @param ean The EAN code
     * @param expiryDate The expiry date
     * @return Number of rows deleted (1 if deleted, 0 if not found)
     */
    @Query("DELETE FROM active_stocks WHERE ean = :ean AND expiryDate = :expiryDate")
    suspend fun deleteByEanAndExpiryDate(ean: String, expiryDate: Instant): Int

    /**
     * Delete all batches for a given EAN code.
     *
     * @param ean The EAN code
     * @return Number of rows deleted
     */
    @Query("DELETE FROM active_stocks WHERE ean = :ean")
    suspend fun deleteByEan(ean: String): Int

    /**
     * Delete all batches from database.
     *
     * @return Number of rows deleted
     */
    @Query("DELETE FROM active_stocks")
    suspend fun deleteAll(): Int

    /**
     * Count total number of batches in database.
     *
     * @return The count of batches
     */
    @Query("SELECT COUNT(*) FROM active_stocks WHERE deletedAt IS NULL")
    suspend fun count(): Int

    /**
     * Retrieve all batches with product catalog information.
     *
     * This query JOINs active_stocks with product_catalog to get complete batch information
     * including product name and pack size.
     *
     * @return List of batches with product info, ordered by expiry date
     */
    @Query("""
        SELECT
            a.id,
            a.ean,
            a.quantity,
            a.expiryDate,
            a.createdAt,
            a.updatedAt,
            p.name as productName,
            p.packSize,
            a.actionTaken
        FROM active_stocks a
        LEFT JOIN product_catalog p ON a.ean = p.ean
        WHERE a.deletedAt IS NULL
        ORDER BY a.expiryDate ASC
    """)
    suspend fun findAllWithProductInfo(): List<BatchWithProductInfo>

    /**
     * Soft delete a batch by setting deletedAt timestamp.
     *
     * @param id The batch ID
     * @param timestamp The timestamp when the batch was deleted
     * @return Number of rows affected (1 if deleted, 0 if not found)
     */
    @Query("UPDATE active_stocks SET deletedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Instant): Int

    /**
     * Restore a soft-deleted batch by setting deletedAt to NULL.
     *
     * @param id The batch ID
     * @return Number of rows affected (1 if restored, 0 if not found)
     */
    @Query("UPDATE active_stocks SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreBatch(id: String): Int

    /**
     * Update the workflow action taken on a batch.
     *
     * @param batchId The batch ID
     * @param action The workflow action (PENDING, DISCOUNTED, REMOVED)
     * @return Number of rows affected (1 if updated, 0 if not found)
     */
    @Query("UPDATE active_stocks SET actionTaken = :action WHERE id = :batchId")
    suspend fun updateAction(batchId: String, action: String): Int

    @Query("SELECT * FROM active_stocks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ActiveStockEntity?

    @Query("DELETE FROM active_stocks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    // ============================================================================
    // Sync Queries (Added for multi-device synchronization)
    // ============================================================================

    /**
     * Get all records with pending sync (dirty records).
     *
     * Returns Flow for reactive observation of dirty records.
     * Filters by storeId for multitenancy support.
     *
     * @param storeId Store identifier to filter records
     * @return Flow of dirty ActiveStockEntity records
     */
    @Query("SELECT * FROM active_stocks WHERE isDirty = 1 AND storeId = :storeId AND deletedAt IS NULL")
    fun getDirtyRecords(storeId: String): Flow<List<ActiveStockEntity>>

    /**
     * Mark records as synced by clearing dirty flag and updating sync metadata.
     *
     * @param ids List of record IDs to mark as synced
     * @param syncedAt Timestamp when sync occurred
     * @return Number of records updated
     */
    @Query(
        """
        UPDATE active_stocks
        SET isDirty = 0,
            syncedAt = :syncedAt,
            version = version + 1
        WHERE id IN (:ids)
        """
    )
    suspend fun markAsSynced(ids: List<String>, syncedAt: Instant): Int

    /**
     * Claim orphan records for a specific user and store.
     *
     * Assigns userId and storeId to records with null or empty userId.
     * Called after successful login to associate orphan records with the user.
     *
     * @param userId User ID to assign to orphan records
     * @param storeId Store ID to assign (typically "1620")
     * @return Number of records updated
     */
    @Query(
        """
        UPDATE active_stocks
        SET userId = :userId,
            storeId = :storeId,
            isDirty = 1
        WHERE (userId IS NULL OR userId = '') AND deletedAt IS NULL
        """
    )
    suspend fun claimOrphanRecords(userId: String, storeId: String): Int

    /**
     * Get all records for a specific store.
     *
     * Returns Flow for reactive observation of store-specific inventory.
     *
     * @param storeId Store identifier to filter records
     * @return Flow of ActiveStockEntity records for the store
     */
    @Query(
        """
        SELECT * FROM active_stocks
        WHERE storeId = :storeId AND deletedAt IS NULL
        ORDER BY expiryDate ASC
        """
    )
    fun getAllByStore(storeId: String): Flow<List<ActiveStockEntity>>

    /**
     * Mark a record as dirty (has unsynced changes).
     *
     * Called when a record is modified locally and needs to be synced.
     *
     * @param id Record ID to mark as dirty
     * @return Number of records updated (1 if successful, 0 if not found)
     */
    @Query("UPDATE active_stocks SET isDirty = 1 WHERE id = :id")
    suspend fun markAsDirty(id: String): Int

    /**
     * Fix storeId values that were stored with literal JSON quotes.
     *
     * Bug fix: EncryptedSessionManager.saveSession() was using .toString() on a JsonElement,
     * which wraps string values in literal quotes (e.g., "\"1620\"" instead of "1620").
     * This query strips surrounding double-quotes from all storeId values.
     *
     * @return Number of records fixed
     */
    @Query("""
        UPDATE active_stocks
        SET storeId = REPLACE(storeId, '"', '')
        WHERE storeId LIKE '"%"'
    """)
    suspend fun sanitizeStoreIds(): Int
}
