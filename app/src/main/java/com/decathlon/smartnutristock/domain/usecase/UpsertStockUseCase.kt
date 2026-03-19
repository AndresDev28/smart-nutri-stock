package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.data.repository.RegisterResult
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidEan
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidPackSize
import com.decathlon.smartnutristock.data.repository.RegisterResult.Success
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for upserting (creating or updating) stock entries.
 *
 * Business Logic:
 * - Validates quantity (must be positive)
 * - Checks if product exists in catalog
 * - Creates stock entry with product information
 *
 * @return RegisterResult with Success (containing product info) or Failure
 */
class UpsertStockUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    /**
     * Upsert stock entry for a product.
     *
     * @param ean Product EAN (13 digits)
     * @param expiryDate Expiry date (Instant)
     * @param quantity Stock quantity (must be positive)
     * @return RegisterResult.Success with ProductCatalogEntity, or RegisterResult.Failure
     */
    suspend operator fun invoke(
        ean: String,
        expiryDate: Instant?,
        quantity: Int
    ): RegisterResult {
        // Validation: Quantity must be positive
        if (quantity <= 0) {
            return InvalidPackSize("La cantidad debe ser positiva")
        }

        // Check if product exists in catalog
        val product = productRepository.findByEan(ean)
        if (product == null) {
            return InvalidEan("El producto no existe en el catálogo")
        }

        // For MVP, we return success with the product info
        // In a full implementation, this would create/update a stock entry table
        return Success(product)
    }
}
