package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for fetching all products from the catalog.
 *
 * This use case returns a Flow of all products, allowing the UI to
 * reactively observe changes in the product catalog.
 */
class GetAllProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    /**
     * Get all products from the catalog.
     *
     * @return Flow of all products (reactive updates)
     */
    operator fun invoke(): Flow<List<ProductCatalogEntity>> {
        return productRepository.getAllProducts()
    }
}
