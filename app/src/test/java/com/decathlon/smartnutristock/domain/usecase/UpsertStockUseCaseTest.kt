package com.decathlon.smartnutristock.domain.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.data.repository.RegisterResult
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidPackSize
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidEan
import com.decathlon.smartnutristock.data.repository.RegisterResult.Success
import java.time.Instant

class UpsertStockUseCaseTest {

    private lateinit var useCase: UpsertStockUseCase
    private lateinit var mockRepository: ProductRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = UpsertStockUseCase(mockRepository)
    }

    // TEST 1: Create new stock entry with valid data
    @Test
    fun `upsertStock with valid data should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 10
        val product = ProductCatalogEntity(
            ean = ean,
            name = "Barra de Proteína",
            packSize = 20,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L
        )

        coEvery { mockRepository.findByEan(ean) } returns product

        val result = useCase(ean, expiryDate, quantity)

        assert(result is Success)
        val returnedProduct = (result as Success).product
        assert(returnedProduct.ean == ean)
        assert(returnedProduct.name == "Barra de Proteína")
    }

    // TEST 2: Update existing stock entry
    @Test
    fun `upsertStock should update existing entry`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 15
        val product = ProductCatalogEntity(
            ean = ean,
            name = "Barra de Proteína",
            packSize = 20,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L
        )

        coEvery { mockRepository.findByEan(ean) } returns product

        val result = useCase(ean, expiryDate, quantity)

        assert(result is Success)
        val returnedProduct = (result as Success).product
        assert(returnedProduct.ean == ean)
    }

    // TEST 3: Upsert with valid quantity
    @Test
    fun `upsertStock with positive quantity should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 100
        val product = ProductCatalogEntity(
            ean = ean,
            name = "Arroz Blanco",
            packSize = 1000,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L
        )

        coEvery { mockRepository.findByEan(ean) } returns product

        val result = useCase(ean, expiryDate, quantity)

        assert(result is Success)
    }

    // TEST 4: Invalid quantity (zero)
    @Test
    fun `upsertStock with zero quantity should return InvalidPackSize`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 0

        val result = useCase(ean, expiryDate, quantity)

        assert(result is InvalidPackSize)
        assert((result as InvalidPackSize).message == "La cantidad debe ser positiva")
    }

    // TEST 5: Invalid quantity (negative)
    @Test
    fun `upsertStock with negative quantity should return InvalidPackSize`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = -5

        val result = useCase(ean, expiryDate, quantity)

        assert(result is InvalidPackSize)
        assert((result as InvalidPackSize).message == "La cantidad debe ser positiva")
    }

    // TEST 6: Null product (product not found)
    @Test
    fun `upsertStock with non-existent product should return InvalidEan`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 10

        coEvery { mockRepository.findByEan(ean) } returns null

        val result = useCase(ean, expiryDate, quantity)

        assert(result is InvalidEan)
        assert((result as InvalidEan).message == "El producto no existe en el catálogo")
    }

    // TEST 7: Edge case - quantity equals 1 (boundary)
    @Test
    fun `upsertStock with quantity equals 1 should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 1
        val product = ProductCatalogEntity(
            ean = ean,
            name = "Barra de Proteína",
            packSize = 20,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L
        )

        coEvery { mockRepository.findByEan(ean) } returns product

        val result = useCase(ean, expiryDate, quantity)

        assert(result is Success)
    }

    // TEST 8: Null expiry date (should still work)
    @Test
    fun `upsertStock with null expiry date should return Success`() = runTest {
        val ean = "8435489901234"
        val expiryDate: Instant? = null
        val quantity = 10
        val product = ProductCatalogEntity(
            ean = ean,
            name = "Barra de Proteína",
            packSize = 20,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L
        )

        coEvery { mockRepository.findByEan(ean) } returns product

        val result = useCase(ean, expiryDate, quantity)

        assert(result is Success)
    }

    // TEST 9: Verify repository is NOT called for invalid quantity
    @Test
    fun `upsertStock should NOT call repository when quantity is zero`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 0

        val result = useCase(ean, expiryDate, quantity)

        coVerify(exactly = 0) { mockRepository.findByEan(any()) }
        assert(result is InvalidPackSize)
    }

    // TEST 10: Verify repository is called for valid data
    @Test
    fun `upsertStock should call repository for valid data`() = runTest {
        val ean = "8435489901234"
        val expiryDate = Instant.now()
        val quantity = 10
        val product = ProductCatalogEntity(
            ean = ean,
            name = "Barra de Proteína",
            packSize = 20,
            createdAt = System.currentTimeMillis() / 1000,
            createdBy = 1L
        )

        coEvery { mockRepository.findByEan(ean) } returns product

        useCase(ean, expiryDate, quantity)

        coVerify { mockRepository.findByEan(ean) }
    }
}
