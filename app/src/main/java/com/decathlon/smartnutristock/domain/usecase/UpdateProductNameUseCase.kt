package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.StockRepository
import javax.inject.Inject

/**
 * Use case for updating a product name in the product catalog.
 *
 * This use case wraps the repository's updateProductName method,
 * allowing the UI to update product information while maintaining
 * clean architecture separation of concerns.
 *
 * @property stockRepository The repository for batch data operations
 */
class UpdateProductNameUseCase @Inject constructor(
    private val stockRepository: StockRepository
) {
    /**
     * Update a product name.
     *
     * @param ean The EAN code of the product
     * @param name The new product name
     * @return Number of rows affected (1 if updated, 0 if not found)
     */
    suspend operator fun invoke(ean: String, name: String): Int {
        return stockRepository.updateProductName(ean, name)
    }
}
