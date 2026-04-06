package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.repository.StockRepository
import javax.inject.Inject

/**
 * Use case for updating an existing batch.
 *
 * This use case wraps the repository's updateBatch method,
 * allowing the UI to update batch information while maintaining
 * clean architecture separation of concerns.
 *
 * @property stockRepository The repository for batch data operations
 */
class UpdateBatchUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    /**
     * Update an existing batch.
     *
     * @param batch The batch with updated fields
     * @return Number of rows affected (1 if updated, 0 if not found)
     */
    suspend operator fun invoke(batch: Batch): Int {
        return stockRepository.updateBatch(batch)
    }
}
