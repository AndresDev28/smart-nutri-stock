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

    /**
     * Remove catalog entries with garbage names caused by the old sync push bug.
     * These entries have name = 'Unknown' because the push used to fallback to "Unknown"
     * when the catalog didn't have the product, and the pull then persisted that garbage.
     *
     * @return Number of entries removed
     */
    @Query("DELETE FROM product_catalog WHERE name = 'Unknown' OR name = ''")
    suspend fun removeGarbageEntries(): Int
}
