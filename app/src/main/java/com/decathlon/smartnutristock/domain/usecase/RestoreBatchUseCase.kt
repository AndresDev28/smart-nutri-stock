package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.StockRepository
import javax.inject.Inject

/**
 * Use case for restoring a soft-deleted batch.
 *
 * This use case wraps the repository's restoreBatch method,
 * allowing the UI to undo a soft delete by setting deletedAt to NULL,
 * making the batch visible again in normal queries.
 *
 * This is part of the undo/redo mechanism for batch deletion,
 * preserving data integrity while allowing recovery from accidental deletions.
 *
 * @property stockRepository The repository for batch data operations
 */
class RestoreBatchUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    /**
     * Restore a soft-deleted batch by setting deletedAt to NULL.
     *
     * @param batchId The ID of the batch to restore
     * @return Number of rows affected (1 if restored, 0 if not found)
     */
    suspend operator fun invoke(batchId: String): Int {
        return stockRepository.restoreBatch(batchId)
    }
}
