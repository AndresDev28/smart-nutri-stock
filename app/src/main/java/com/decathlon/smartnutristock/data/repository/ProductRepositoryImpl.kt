package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProductRepository using Room database.
 * All database operations run on Dispatchers.IO for async, non-blocking behavior.
 */
@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val productCatalogDao: ProductCatalogDao
) : ProductRepository {

    override suspend fun findByEan(ean: String): ProductCatalogEntity? {
        return productCatalogDao.findByEan(ean)
    }

    override suspend fun registerProduct(product: ProductCatalogEntity): RegisterResult {
        // Validation: EAN must be 13 digits
        if (!eanMatchesEanFormat(product.ean)) {
            return RegisterResult.Failure.InvalidEan("El código debe tener 13 dígitos")
        }

        // Validation: Name must be 3-100 characters
        if (!isValidProductName(product.name)) {
            return RegisterResult.Failure.InvalidName("El nombre debe tener entre 3 y 100 caracteres")
        }

        // Validation: Pack size must be positive
        if (!isValidPackSize(product.packSize)) {
            return RegisterResult.Failure.InvalidPackSize("El pack size debe ser positivo")
        }

        // Check for duplicate EAN
        val existing = productCatalogDao.findByEan(product.ean)
        if (existing != null) {
            return RegisterResult.Failure.DuplicateEan(
                "El producto '${existing.name}' ya existe con este código",
                existing
            )
        }

        // Insert product
        try {
            productCatalogDao.insertOrReplace(product)
            return RegisterResult.Success(product)
        } catch (e: Exception) {
            return RegisterResult.Failure.DatabaseError(
                "Error al guardar: ${e.message}"
            )
        }
    }

    override fun getAllProducts(): kotlinx.coroutines.flow.Flow<List<ProductCatalogEntity>> {
        return productCatalogDao.getAllProducts()
    }

    override suspend fun deleteByEan(ean: String): Int {
        return productCatalogDao.deleteByEan(ean)
    }

    // Validation helper methods
    private fun eanMatchesEanFormat(ean: String): Boolean {
        return ean.matches(Regex("^\\d{13}$"))
    }

    private fun isValidProductName(name: String): Boolean {
        return name.length in 3..100
    }

    private fun isValidPackSize(packSize: Int): Boolean {
        return packSize > 0
    }
}
