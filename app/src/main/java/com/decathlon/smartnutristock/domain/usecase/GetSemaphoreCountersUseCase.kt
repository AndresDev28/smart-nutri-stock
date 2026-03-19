package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for calculating semaphore counters by status.
 *
 * Counter Logic:
 * - Groups products by expiry status (green/yellow/red)
 * - Returns count for each status
 * - Total count = green + yellow + red
 *
 * Status Definitions:
 * - Green: 8+ days until expiry (safe)
 * - Yellow: 1-7 days until expiry (warning)
 * - Red: <=0 days until expiry (expired)
 *
 * @return Flow of SemaphoreCounters (reactive updates)
 */
class GetSemaphoreCountersUseCase @Inject constructor(
    private val productRepository: ProductRepository
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
     * Uses CalculateStatusUseCase logic to determine status.
     */
    private fun calculateCounters(products: List<ProductCatalogEntity>): SemaphoreCounters {
        val greenCount = products.count { it.daysUntilExpiry >= 8 }
        val yellowCount = products.count { it.daysUntilExpiry in 1..7 }
        val redCount = products.count { it.daysUntilExpiry <= 0 }

        return SemaphoreCounters(
            green = greenCount,
            yellow = yellowCount,
            red = redCount,
            total = products.size
        )
    }
}

/**
 * Domain model for semaphore counters.
 * Aggregated statistics for dashboard display.
 */
data class SemaphoreCounters(
    val green: Int,
    val yellow: Int,
    val red: Int,
    val total: Int
) {
    /**
     * Calculate percentage of expired products.
     */
    fun expiredPercentage(): Float = if (total > 0) {
        (red.toFloat() / total) * 100
    } else {
        0f
    }

    /**
     * Calculate percentage of warning products.
     */
    fun warningPercentage(): Float = if (total > 0) {
        (yellow.toFloat() / total) * 100
    } else {
        0f
    }

    /**
     * Calculate percentage of safe products.
     */
    fun safePercentage(): Float = if (total > 0) {
        (green.toFloat() / total) * 100
    } else {
        0f
    }
}
