package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.data.repository.RegisterResult
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.DatabaseError
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.DuplicateEan
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidEan
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidName
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidPackSize
import com.decathlon.smartnutristock.data.repository.RegisterResult.Success
import javax.inject.Inject

/**
 * Use case for registering new products dynamically.
 *
 * Business Logic:
 * 1. Validates EAN format (13 digits with regex: ^\d{13}$)
 * 2. Validates product name length (3-100 characters)
 * 3. Validates pack size (positive integer > 0)
 * 4. Checks for duplicate EAN before registration
 * 5. Returns RegisterResult with appropriate Success or Failure type
 *
 * Error Messages (Spanish):
 * - InvalidEan: "El código debe tener 13 dígitos"
 * - InvalidName: "El nombre debe tener entre 3 y 100 caracteres"
 * - InvalidPackSize: "El pack size debe ser positivo"
 * - DuplicateEan: "El producto '{existingName}' ya existe con este código"
 * - DatabaseError: "Error al guardar en la base de datos"
 */
class RegisterProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {

    /**
     * Validates EAN format (13 digits).
     * Uses regex: ^\d{13}$ (GS1 standard)
     */
    private fun validateEanFormat(ean: String): Boolean {
        return ean.matches(Regex("^\\d{13}$"))
    }

    /**
     * Validates product name length (3-100 characters).
     */
    private fun validateNameLength(name: String): Boolean {
        return name.length in 3..100
    }

    /**
     * Validates pack size (positive integer > 0).
     */
    private fun validatePackSize(packSize: Int): Boolean {
        return packSize > 0
    }

    /**
     * Invokes the use case.
     *
     * @param ean 13-digit EAN code
     * @param productName Product name (3-100 chars)
     * @param packSize Pack size in grams (positive)
     * @param userId User ID who is registering
     * @return RegisterResult.Success with ProductEntity, or RegisterResult.Failure with error
     */
    suspend operator fun invoke(
        ean: String,
        productName: String,
        packSize: Int,
        userId: Long
    ): RegisterResult {
        // Validation 1: EAN must be 13 digits
        if (!validateEanFormat(ean)) {
            return InvalidEan("El código debe tener 13 dígitos")
        }

        // Validation 2: Name must be 3-100 characters
        if (!validateNameLength(productName)) {
            return InvalidName("El nombre debe tener entre 3 y 100 caracteres")
        }

        // Validation 3: Pack size must be positive
        if (!validatePackSize(packSize)) {
            return InvalidPackSize("El pack size debe ser positivo")
        }

        // Check for duplicate EAN before creating product
        val existingProduct = productRepository.findByEan(ean)
        if (existingProduct != null) {
            return DuplicateEan(
                message = "El producto '${existingProduct.name}' ya existe con este código",
                existingProduct = existingProduct
            )
        }

        // Create product entity
        val product = ProductCatalogEntity(
            ean = ean,
            name = productName,
            packSize = packSize,
            createdAt = System.currentTimeMillis() / 1000,  // Room expects seconds
            createdBy = userId
        )

        // Register product (handles database insertion)
        return try {
            productRepository.registerProduct(product)
        } catch (e: Exception) {
            DatabaseError("Error al guardar en la base de datos: ${e.message}")
        }
    }
}
