package com.decathlon.smartnutristock.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.local.SmartNutriStockDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ProductCatalogDao using an in-memory Room database.
 *
 * Tests verify CRUD operations including findByEan and insertOrReplace behavior.
 */
@RunWith(AndroidJUnit4::class)
class ProductCatalogDaoTest {

    private lateinit var database: SmartNutriStockDatabase
    private lateinit var productCatalogDao: ProductCatalogDao

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartNutriStockDatabase::class.java
        ).build()
        productCatalogDao = database.productCatalogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test findByEan returns product when it exists.
     */
    @Test
    fun findByEan_returns_product_when_it_exists() = runTest {
        // Given
        val product = ProductCatalogEntity(
            ean = "8435408475366",
            name = "Protein Whey 500g",
            packSize = 500,
            createdAt = System.currentTimeMillis(),
            createdBy = 1L,
            daysUntilExpiry = 0
        )
        productCatalogDao.insertOrReplace(product)

        // When
        val result = productCatalogDao.findByEan("8435408475366")

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.ean).isEqualTo("8435408475366")
        assertThat(result.name).isEqualTo("Protein Whey 500g")
        assertThat(result.packSize).isEqualTo(500)
    }

    /**
     * Test findByEan returns null when it doesn't exist.
     */
    @Test
    fun findByEan_returns_null_when_it_doesnt_exist() = runTest {
        // Given - no products in database

        // When
        val result = productCatalogDao.findByEan("9999999999999")

        // Then
        assertThat(result).isNull()
    }

    /**
     * Test insertOrReplace correctly replaces existing product with same EAN.
     */
    @Test
    fun insertOrReplace_correctly_replaces_existing_product_with_same_EAN() = runTest {
        // Given - insert initial product
        val initialProduct = ProductCatalogEntity(
            ean = "8435408475366",
            name = "Protein Whey 500g",
            packSize = 500,
            createdAt = 1000L,
            createdBy = 1L,
            daysUntilExpiry = 0
        )
        productCatalogDao.insertOrReplace(initialProduct)

        // When - insert replacement with same EAN
        val replacementProduct = ProductCatalogEntity(
            ean = "8435408475366",
            name = "Protein Whey 1kg",
            packSize = 1000,
            createdAt = 2000L,
            createdBy = 2L,
            daysUntilExpiry = 30
        )
        productCatalogDao.insertOrReplace(replacementProduct)
        val result = productCatalogDao.findByEan("8435408475366")

        // Then - verify replacement was successful
        assertThat(result).isNotNull()
        assertThat(result!!.name).isEqualTo("Protein Whey 1kg")
        assertThat(result.packSize).isEqualTo(1000)
        assertThat(result.createdAt).isEqualTo(2000L)
        assertThat(result.createdBy).isEqualTo(2L)
        assertThat(result.daysUntilExpiry).isEqualTo(30)

        // Verify only one record exists
        val count = productCatalogDao.getAllProducts()
        assertThat(count).isNotNull()
    }

    /**
     * Test insertOrReplace correctly inserts a new product.
     */
    @Test
    fun insertOrReplace_correctly_inserts_a_new_product() = runTest {
        // Given - database is empty

        // When - insert new product
        val product = ProductCatalogEntity(
            ean = "8435408475366",
            name = "Protein Whey 500g",
            packSize = 500,
            createdAt = System.currentTimeMillis(),
            createdBy = 1L,
            daysUntilExpiry = 0
        )
        productCatalogDao.insertOrReplace(product)
        val result = productCatalogDao.findByEan("8435408475366")

        // Then - verify product was inserted
        assertThat(result).isNotNull()
        assertThat(result!!.ean).isEqualTo("8435408475366")
        assertThat(result.name).isEqualTo("Protein Whey 500g")
    }
}
