package com.decathlon.smartnutristock.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.BatchWithProductInfo
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.local.SmartNutriStockDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Integration tests for StockDao using an in-memory Room database.
 *
 * Tests verify CRUD operations and JOIN queries with product catalog.
 * Note: ActiveStockEntity does not have a status field - status is calculated
 * dynamically in the domain layer, not stored in the database.
 */
@RunWith(AndroidJUnit4::class)
class StockDaoTest {

    private lateinit var database: SmartNutriStockDatabase
    private lateinit var stockDao: StockDao
    private lateinit var productCatalogDao: ProductCatalogDao

    private val testNow = Instant.parse("2024-01-01T00:00:00Z")

    @Before
    fun setup() {
        // Create an in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SmartNutriStockDatabase::class.java
        ).build()
        stockDao = database.stockDao()
        productCatalogDao = database.productCatalogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Test insert creates new record.
     */
    @Test
    fun insert_creates_new_record() = runTest {
        // Given
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )

        // When
        val rowId = stockDao.insert(stock)

        // Then
        assertThat(rowId).isNotEqualTo(-1L)

        val found = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(found).isNotNull()
        assertThat(found!!.id).isEqualTo("batch-1")
        assertThat(found.quantity).isEqualTo(10)
    }

    /**
     * Test insert enforces unique constraint on (ean, expiryDate).
     */
    @Test
    fun insert_enforces_unique_constraint_on_ean_and_expiryDate() = runTest {
        // Given
        val stock1 = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock1)

        // When - try to insert duplicate (same ean and expiryDate)
        val stock2 = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475366",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow.plusSeconds(1),
            updatedAt = testNow.plusSeconds(1)
        )

        // Then - should throw SQLiteConstraintException
        var exceptionThrown = false
        try {
            stockDao.insert(stock2)
        } catch (e: Exception) {
            exceptionThrown = true
            assertThat(e.message).contains("UNIQUE constraint failed")
        }
        assertThat(exceptionThrown).isTrue()

        // Verify only one record exists
        val all = stockDao.findAll()
        assertThat(all).hasSize(1)
    }

    /**
     * Test insert allows different EAN with same expiryDate.
     */
    @Test
    fun insert_allows_different_EAN_with_same_expiryDate() = runTest {
        // Given
        val stock1 = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock1)

        // When - insert different EAN with same expiryDate
        val stock2 = ActiveStockEntity(
            id = "batch-2",
            ean = "1234567890123",
            quantity = 20,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock2)

        // Then - both should exist
        val all = stockDao.findAll()
        assertThat(all).hasSize(2)
    }

    /**
     * Test insert allows same EAN with different expiryDate.
     */
    @Test
    fun insert_allows_same_EAN_with_different_expiryDate() = runTest {
        // Given
        val stock1 = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock1)

        // When - insert same EAN with different expiryDate
        val stock2 = ActiveStockEntity(
            id = "batch-2",
            ean = "8435408475366",
            quantity = 20,
            expiryDate = testNow.plusSeconds(86400), // 1 day later
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock2)

        // Then - both should exist
        val all = stockDao.findAll()
        assertThat(all).hasSize(2)
    }

    /**
     * Test update modifies existing record by id.
     */
    @Test
    fun update_modifies_existing_record_by_id() = runTest {
        // Given
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock)

        // When
        val updatedStock = stock.copy(
            quantity = 20,
            updatedAt = testNow.plusSeconds(3600)
        )
        val rowsAffected = stockDao.update(updatedStock)

        // Then
        assertThat(rowsAffected).isEqualTo(1)

        val found = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(found).isNotNull()
        assertThat(found!!.quantity).isEqualTo(20)
        assertThat(found.updatedAt).isEqualTo(testNow.plusSeconds(3600))
    }

    /**
     * Test update returns 0 when record not found.
     */
    @Test
    fun update_returns_0_when_record_not_found() = runTest {
        // Given - empty database

        // When
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        val rowsAffected = stockDao.update(stock)

        // Then
        assertThat(rowsAffected).isEqualTo(0)
    }

    /**
     * Test findByEanAndExpiryDate returns batch when exists.
     */
    @Test
    fun findByEanAndExpiryDate_returns_batch_when_exists() = runTest {
        // Given
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock)

        // When
        val result = stockDao.findByEanAndExpiryDate("8435408475366", testNow)

        // Then
        assertThat(result).isNotNull()
        assertThat(result!!.ean).isEqualTo("8435408475366")
        assertThat(result.quantity).isEqualTo(10)
        assertThat(result.expiryDate).isEqualTo(testNow)
    }

    /**
     * Test findByEanAndExpiryDate returns null when not exists.
     */
    @Test
    fun findByEanAndExpiryDate_returns_null_when_not_exists() = runTest {
        // Given - empty database

        // When
        val result = stockDao.findByEanAndExpiryDate("8435408475366", testNow)

        // Then
        assertThat(result).isNull()
    }

    /**
     * Test findByEan returns batches ordered by expiryDate ASC.
     */
    @Test
    fun findByEan_returns_batches_ordered_by_expiryDate_ASC() = runTest {
        // Given
        val expiry1 = testNow
        val expiry2 = testNow.plusSeconds(86400) // 1 day later
        val expiry3 = testNow.plusSeconds(172800) // 2 days later

        stockDao.insert(ActiveStockEntity("batch-1", "8435408475366", 10, expiry2, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-2", "8435408475366", 20, expiry3, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-3", "8435408475366", 15, expiry1, testNow, testNow))

        // When
        val result = stockDao.findByEan("8435408475366")

        // Then - should be ordered by expiryDate ASC
        assertThat(result).hasSize(3)
        assertThat(result[0].expiryDate).isEqualTo(expiry1)
        assertThat(result[1].expiryDate).isEqualTo(expiry2)
        assertThat(result[2].expiryDate).isEqualTo(expiry3)
    }

    /**
     * Test findAll returns all batches ordered by expiryDate ASC.
     */
    @Test
    fun findAll_returns_all_batches_ordered_by_expiryDate_ASC() = runTest {
        // Given
        val expiry1 = testNow
        val expiry2 = testNow.plusSeconds(86400)
        val expiry3 = testNow.plusSeconds(172800)

        stockDao.insert(ActiveStockEntity("batch-2", "ean2", 20, expiry2, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-3", "ean3", 30, expiry3, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-1", "ean1", 10, expiry1, testNow, testNow))

        // When
        val result = stockDao.findAll()

        // Then - should be ordered by expiryDate ASC
        assertThat(result).hasSize(3)
        assertThat(result[0].expiryDate).isEqualTo(expiry1)
        assertThat(result[1].expiryDate).isEqualTo(expiry2)
        assertThat(result[2].expiryDate).isEqualTo(expiry3)
    }

    /**
     * Test deleteByEanAndExpiryDate deletes batch.
     */
    @Test
    fun deleteByEanAndExpiryDate_deletes_batch() = runTest {
        // Given
        val stock = ActiveStockEntity(
            id = "batch-1",
            ean = "8435408475366",
            quantity = 10,
            expiryDate = testNow,
            createdAt = testNow,
            updatedAt = testNow
        )
        stockDao.insert(stock)

        // When
        val count = stockDao.deleteByEanAndExpiryDate("8435408475366", testNow)

        // Then
        assertThat(count).isEqualTo(1)

        val found = stockDao.findByEanAndExpiryDate("8435408475366", testNow)
        assertThat(found).isNull()
    }

    /**
     * Test deleteByEan deletes all batches for EAN.
     */
    @Test
    fun deleteByEan_deletes_all_batches_for_EAN() = runTest {
        // Given
        stockDao.insert(ActiveStockEntity("batch-1", "8435408475366", 10, testNow, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-2", "8435408475366", 20, testNow.plusSeconds(86400), testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-3", "1234567890123", 30, testNow, testNow, testNow))

        // When
        val count = stockDao.deleteByEan("8435408475366")

        // Then
        assertThat(count).isEqualTo(2)

        val remaining = stockDao.findAll()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].ean).isEqualTo("1234567890123")
    }

    /**
     * Test deleteAll deletes all batches.
     */
    @Test
    fun deleteAll_deletes_all_batches() = runTest {
        // Given
        stockDao.insert(ActiveStockEntity("batch-1", "ean1", 10, testNow, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-2", "ean2", 20, testNow, testNow, testNow))

        // When
        val count = stockDao.deleteAll()

        // Then
        assertThat(count).isEqualTo(2)

        val all = stockDao.findAll()
        assertThat(all).isEmpty()
    }

    /**
     * Test count returns correct number of batches.
     */
    @Test
    fun count_returns_correct_number_of_batches() = runTest {
        // Given - empty database
        assertThat(stockDao.count()).isEqualTo(0)

        // When
        stockDao.insert(ActiveStockEntity("batch-1", "ean1", 10, testNow, testNow, testNow))
        assertThat(stockDao.count()).isEqualTo(1)

        stockDao.insert(ActiveStockEntity("batch-2", "ean2", 20, testNow, testNow, testNow))
        assertThat(stockDao.count()).isEqualTo(2)
    }

    /**
     * Test findAllWithProductInfo returns correct JOIN between active_stocks and product_catalog,
     * ordered by expiryDate ASC.
     */
    @Test
    fun findAllWithProductInfo_returns_correct_JOIN_between_active_stocks_and_product_catalog_ordered_by_expiryDate_ASC() = runTest {
        // Given
        val expiry1 = testNow
        val expiry2 = testNow.plusSeconds(86400)
        val expiry3 = testNow.plusSeconds(172800)

        // Insert products
        val product1 = ProductCatalogEntity(
            ean = "8435408475366",
            name = "Protein Whey 500g",
            packSize = 500,
            createdAt = System.currentTimeMillis(),
            createdBy = 1L
        )
        productCatalogDao.insertOrReplace(product1)

        val product2 = ProductCatalogEntity(
            ean = "1234567890123",
            name = "Creatine 300g",
            packSize = 300,
            createdAt = System.currentTimeMillis(),
            createdBy = 1L
        )
        productCatalogDao.insertOrReplace(product2)

        // Insert batches (note: inserted in different order to test sorting)
        stockDao.insert(ActiveStockEntity("batch-2", "1234567890123", 20, expiry2, testNow, testNow))
        stockDao.insert(ActiveStockEntity("batch-3", "9999999999999", 30, expiry3, testNow, testNow)) // No product in catalog
        stockDao.insert(ActiveStockEntity("batch-1", "8435408475366", 10, expiry1, testNow, testNow))

        // When
        val result = stockDao.findAllWithProductInfo()

        // Then - should be ordered by expiryDate ASC
        assertThat(result).hasSize(3)

        // First batch (expiry1) - with product info
        val batch1 = result[0]
        assertThat(batch1.id).isEqualTo("batch-1")
        assertThat(batch1.ean).isEqualTo("8435408475366")
        assertThat(batch1.quantity).isEqualTo(10)
        assertThat(batch1.expiryDate).isEqualTo(expiry1)
        assertThat(batch1.productName).isEqualTo("Protein Whey 500g")
        assertThat(batch1.packSize).isEqualTo(500)

        // Second batch (expiry2) - with product info
        val batch2 = result[1]
        assertThat(batch2.id).isEqualTo("batch-2")
        assertThat(batch2.ean).isEqualTo("1234567890123")
        assertThat(batch2.quantity).isEqualTo(20)
        assertThat(batch2.expiryDate).isEqualTo(expiry2)
        assertThat(batch2.productName).isEqualTo("Creatine 300g")
        assertThat(batch2.packSize).isEqualTo(300)

        // Third batch (expiry3) - without product info (LEFT JOIN)
        val batch3 = result[2]
        assertThat(batch3.id).isEqualTo("batch-3")
        assertThat(batch3.ean).isEqualTo("9999999999999")
        assertThat(batch3.quantity).isEqualTo(30)
        assertThat(batch3.expiryDate).isEqualTo(expiry3)
        assertThat(batch3.productName).isNull() // No product in catalog
        assertThat(batch3.packSize).isNull()
    }

    /**
     * Test findAllWithProductInfo returns empty list when no batches exist.
     */
    @Test
    fun findAllWithProductInfo_returns_empty_list_when_no_batches_exist() = runTest {
        // Given - empty database (but maybe products exist)
        productCatalogDao.insertOrReplace(
            ProductCatalogEntity(
                ean = "8435408475366",
                name = "Protein Whey 500g",
                packSize = 500,
                createdAt = System.currentTimeMillis(),
                createdBy = 1L
            )
        )

        // When
        val result = stockDao.findAllWithProductInfo()

        // Then
        assertThat(result).isEmpty()
    }

    /**
     * Test findAllWithProductInfo returns batches with null product info when product not in catalog.
     */
    @Test
    fun findAllWithProductInfo_returns_batches_with_null_product_info_when_product_not_in_catalog() = runTest {
        // Given
        stockDao.insert(ActiveStockEntity("batch-1", "9999999999999", 10, testNow, testNow, testNow))

        // When
        val result = stockDao.findAllWithProductInfo()

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].ean).isEqualTo("9999999999999")
        assertThat(result[0].productName).isNull()
        assertThat(result[0].packSize).isNull()
    }
}
