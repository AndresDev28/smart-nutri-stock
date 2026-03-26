package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import javax.inject.Inject

/**
 * Use case for fetching all batches with product information.
 *
 * This use case returns a Flow of all batches enriched with product name and pack size,
 * allowing the UI to reactively observe changes in the stock.
 */
class GetAllBatchesUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    /**
     * Get all batches with product information.
     *
     * @return Flow of all batches with product info (reactive updates)
     */
    operator fun invoke(): Flow<List<Batch>> {
        return flow {
            val batches = stockRepository.findAllWithProductInfo().toList()
            emit(batches)
        }
    }
}
