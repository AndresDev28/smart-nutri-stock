package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for calculating semaphore counters by status.
 *
 * Counter Logic:
 * - Groups products by expiry status (green/yellow/red/expired)
 * - Returns count for each status
 * - Total count = green + yellow + red + expired
 *
 * Status Definitions (NEW):
 * - Green: >30 days until expiry
 * - Yellow: 16-30 days until expiry
 * - Red: 1-15 days until expiry
 * - Expired: <=0 days until expiry
 *
 * @return Flow of SemaphoreCounters (reactive updates)
 */
class GetSemaphoreCountersUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val calculateStatusUseCase: CalculateStatusUseCase
) {

    /**
     * Calculate semaphore counters from all products.
     *
     * @return Flow of SemaphoreCounters (reactive updates)
     */
    operator fun invoke(): Flow<SemaphoreCounters> {
        return productRepository.getAllProducts()
            .map { products ->
                calculateCounters(products)
            }
    }

    /**
     * Calculate counters from product list.
     * Uses CalculateStatusUseCase to determine status with NEW thresholds.
     *
     * TODO: Replace ProductRepository with StockRepository in Fase 2.3 (Hilt Implementation)
     * TODO: Remove daysUntilExpiry -> Instant conversion when StockRepository is used
     */
    private fun calculateCounters(products: List<ProductCatalogEntity>): SemaphoreCounters {
        val clock = Clock.systemUTC()
        val now = Instant.now(clock)

        // Helper to convert daysUntilExpiry (Int) to Instant for CalculateStatusUseCase
        fun toInstant(daysUntil: Int): Instant {
            return now.plus(Duration.ofDays(daysUntil.toLong()))
        }

        val greenCount = products.count { calculateStatusUseCase(toInstant(it.daysUntilExpiry)) == SemaphoreStatus.GREEN }
        val yellowCount = products.count { calculateStatusUseCase(toInstant(it.daysUntilExpiry)) == SemaphoreStatus.YELLOW }
        val redCount = products.count { calculateStatusUseCase(toInstant(it.daysUntilExpiry)) == SemaphoreStatus.RED }
        val expiredCount = products.count { calculateStatusUseCase(toInstant(it.daysUntilExpiry)) == SemaphoreStatus.EXPIRED }

        return SemaphoreCounters(
            red = redCount,
            yellow = yellowCount,
            green = greenCount,
            expired = expiredCount
        )
    }
}
