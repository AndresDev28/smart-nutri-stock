package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.StockRepository
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for soft-deleting a batch.
 *
 * This use case wraps the repository's softDeleteBatch method,
 * allowing the UI to mark a batch as deleted while maintaining
 * clean architecture separation of concerns.
 *
 * The soft delete sets the deletedAt timestamp, making the batch
 * invisible to normal queries while preserving it for potential undo.
 *
 * @property stockRepository The repository for batch data operations
 */
class SoftDeleteBatchUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    /**
     * Soft delete a batch by setting deletedAt timestamp.
     *
     * @param batchId The ID of the batch to soft delete
     * @param timestamp The timestamp when the batch was deleted
     * @return Number of rows affected (1 if deleted, 0 if not found)
     */
    suspend operator fun invoke(batchId: String, timestamp: Instant = Instant.now(Clock.systemUTC())): Int {
        return stockRepository.softDeleteBatch(batchId, timestamp)
    }
}
