package com.decathlon.smartnutristock.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity

@Dao
interface ProductCatalogDao {
    // Find product by EAN (used for duplicate check)
    @Query("SELECT * FROM product_catalog WHERE ean = :ean LIMIT 1")
    suspend fun findByEan(ean: String): ProductCatalogEntity?

    // Insert or replace product (upsert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(product: ProductCatalogEntity)

    // Get all products (for debugging/admin)
    @Query("SELECT * FROM product_catalog ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductCatalogEntity>>

    // Delete product by EAN
    @Query("DELETE FROM product_catalog WHERE ean = :ean")
    suspend fun deleteByEan(ean: String): Int

    // Update product name by EAN
    @Query("UPDATE product_catalog SET name = :name WHERE ean = :ean")
    suspend fun updateProductName(ean: String, name: String): Int
}
