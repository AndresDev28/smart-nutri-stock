package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.repository.StockRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for calculating semaphore counters by status.
 *
 * Counter Logic:
 * - Groups batches by expiry status (green/yellow/red/expired)
 * - Returns count for each status
 * - Total count = green + yellow + red + expired
 *
 * Status Definitions:
 * - Green: >30 days until expiry
 * - Yellow: 16-30 days until expiry
 * - Red: 1-15 days until expiry
 * - Expired: <=0 days until expiry
 *
 * @return Flow of SemaphoreCounters (reactive updates)
 */
class GetSemaphoreCountersUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {

    /**
     * Calculate semaphore counters from all batches.
     *
     * Delegates to StockRepository which calculates counters using CalculateStatusUseCase.
     *
     * @return Flow of SemaphoreCounters (reactive updates)
     */
    suspend operator fun invoke(): Flow<SemaphoreCounters> {
        return stockRepository.getSemaphoreCounters()
    }
}
