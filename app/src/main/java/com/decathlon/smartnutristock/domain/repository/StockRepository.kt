package com.decathlon.smartnutristock.domain.repository

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for batch stock management.
 *
 * Provides CRUD operations with upsert logic that handles
 * batch creation, updates, and automatic deletion of depleted batches.
 */
interface StockRepository {
    /**
     * Upsert a batch into database.
     *
     * This is the core operation for batch management:
     * - Inserts new batch if not exists (ean + expiryDate unique key)
     * - Updates existing batch if exists
     * - Implements "Golden Rule": Deletes batch if quantity <= 0
     *
     * @param batch The batch to upsert
     * @return UpsertBatchResult indicating Success/Deleted/Error
     */
    suspend fun upsert(batch: Batch): UpsertBatchResult

    /**
     * Find all batches for a given EAN code.
     *
     * @param ean The EAN code
     * @return Flow of all batches with this EAN
     */
    suspend fun findByEan(ean: String): Flow<Batch>

    /**
     * Find a specific batch by EAN and expiry date.
     *
     * @param ean The EAN code
     * @param expiryDate The expiry date
     * @return The batch if found, null otherwise
     */
    suspend fun findByEanAndExpiryDate(ean: String, expiryDate: java.time.Instant): Batch?

    /**
     * Retrieve all batches from database.
     *
     * @return Flow of all batches
     */
    suspend fun findAll(): Flow<Batch>

    /**
     * Count batches by semaphore status.
     *
     * @return Flow of SemaphoreCounters with counts for each status
     */
    suspend fun getSemaphoreCounters(): Flow<SemaphoreCounters>

    /**
     * Delete a batch by EAN and expiry date.
     *
     * @param ean The EAN code
     * @param expiryDate The expiry date
     * @return Number of rows deleted
     */
    suspend fun deleteByEanAndExpiryDate(ean: String, expiryDate: java.time.Instant): Int

    /**
     * Delete all batches for a given EAN code.
     *
     * @param ean The EAN code
     * @return Number of rows deleted
     */
    suspend fun deleteByEan(ean: String): Int

    /**
     * Retrieve all batches with product catalog information.
     *
     * This method returns batches enriched with product name and pack size
     * from the product_catalog table.
     *
     * @return Flow of all batches with product info
     */
    suspend fun findAllWithProductInfo(): Flow<Batch>

    /**
     * Update an existing batch.
     *
     * @param batch The batch to update
     * @return Number of rows affected (1 if updated, 0 if not found)
     */
    suspend fun updateBatch(batch: Batch): Int

    /**
     * Soft delete a batch by setting deletedAt timestamp.
     *
     * @param id The batch ID
     * @param timestamp The timestamp when the batch was deleted
     * @return Number of rows affected (1 if deleted, 0 if not found)
     */
    suspend fun softDeleteBatch(id: String, timestamp: java.time.Instant): Int

    /**
     * Restore a soft-deleted batch by setting deletedAt to NULL.
     *
     * @param id The batch ID
     * @return Number of rows affected (1 if restored, 0 if not found)
     */
    suspend fun restoreBatch(id: String): Int

    /**
     * Update product name in the product catalog.
     *
     * @param ean The EAN code of the product
     * @param name The new product name
     * @return Number of rows affected (1 if updated, 0 if not found)
     */
    suspend fun updateProductName(ean: String, name: String): Int
}
