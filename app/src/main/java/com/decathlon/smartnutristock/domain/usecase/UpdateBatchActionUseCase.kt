package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.WorkflowAction
import com.decathlon.smartnutristock.domain.repository.StockRepository
import javax.inject.Inject

/**
 * Use case to update the workflow action taken on a batch.
 *
 * @property repository The stock repository for batch operations
 */
class UpdateBatchActionUseCase @Inject constructor(
    private val repository: StockRepository
) {
    /**
     * Update the workflow action for a specific batch.
     *
     * @param batchId The unique identifier of the batch
     * @param action The workflow action to apply (PENDING, DISCOUNTED, REMOVED)
     */
    suspend operator fun invoke(batchId: String, action: WorkflowAction) {
        repository.updateBatchAction(batchId, action)
    }
}
