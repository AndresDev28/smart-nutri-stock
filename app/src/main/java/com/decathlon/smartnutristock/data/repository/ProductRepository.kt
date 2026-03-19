package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity

/**
 * Repository interface for product catalog operations.
 * Abstraction over data sources to enable testing and architecture flexibility.
 */
interface ProductRepository {
    /**
     * Find a product by EAN code.
     * @param ean 13-digit EAN code
     * @return Product if found, null otherwise
     */
    suspend fun findByEan(ean: String): ProductCatalogEntity?

    /**
     * Register a new product in the catalog.
     * Uses insertOrReplace to handle duplicates gracefully.
     * @param product Product to register
     * @return RegisterResult with Success or Failure
     */
    suspend fun registerProduct(product: ProductCatalogEntity): RegisterResult

    /**
     * Get all products in the catalog (for debugging).
     * @return Flow of all products
     */
    fun getAllProducts(): kotlinx.coroutines.flow.Flow<List<ProductCatalogEntity>>

    /**
     * Delete a product by EAN code.
     * @param ean 13-digit EAN code
     */
    suspend fun deleteByEan(ean: String): Int
}

/**
 * Sealed class for product registration results.
 * Enables exhaustive `when()` expressions in Compose UI.
 */
sealed class RegisterResult {
    data class Success(val product: ProductCatalogEntity) : RegisterResult()

    sealed class Failure : RegisterResult() {
        data class InvalidEan(val message: String) : Failure()
        data class InvalidName(val message: String) : Failure()
        data class InvalidPackSize(val message: String) : Failure()
        data class DuplicateEan(val message: String, val existingProduct: ProductCatalogEntity) : Failure()
        data class DatabaseError(val message: String) : Failure()
    }
}
