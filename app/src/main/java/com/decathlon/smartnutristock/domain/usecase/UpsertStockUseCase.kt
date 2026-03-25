package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import com.decathlon.smartnutristock.domain.repository.StockRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for upserting product stock (batches).
 *
 * This use case handles batch creation/updates with automatic
 * status calculation and cleanup of depleted batches.
 *
 * Replaces old ProductCatalogEntity-based stock management.
 */
class UpsertStockUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {

    /**
     * Upsert a batch into stock.
     *
     * This operation:
     * - Calculates semaphore status automatically
     * - Inserts or updates batch based on composite key (ean + expiryDate)
     * - Automatically deletes batches when quantity <= 0 (Golden Rule)
     *
     * @param batch The batch to upsert
     * @return UpsertBatchResult indicating success/deleted/error
     */
    suspend fun upsert(batch: Batch): UpsertBatchResult {
        return stockRepository.upsert(batch)
    }
}
