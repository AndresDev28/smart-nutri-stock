package com.decathlon.smartnutristock.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_catalog",
    indices = [
        Index(value = ["ean"]),  // Index for fast lookups (O(log n) instead of O(n))
        Index(value = ["name"]),  // Index for search by name
    ]
)
data class ProductCatalogEntity(
    @PrimaryKey
    val ean: String,  // 13-digit EAN code (GS1 standard)

    val name: String,  // Product name (3-100 characters)

    val packSize: Int,  // Pack size in grams (positive integer)

    val createdAt: Long,  // Timestamp when product was registered
    val createdBy: Long,  // User ID who registered this product

    val daysUntilExpiry: Int = 0  // Days until expiry (negative if expired)
)
