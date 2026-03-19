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
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.DuplicateEan
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidEan
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidName
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidPackSize
import com.decathlon.smartnutristock.data.repository.RegisterResult.Success
import java.time.Instant

class RegisterProductUseCaseTest {

    private lateinit var useCase: RegisterProductUseCase
    private lateinit var mockRepository: ProductRepository

    @Before
    fun setup() {
        mockRepository = mockk()
        useCase = RegisterProductUseCase(mockRepository)
    }

    // TEST 1: Successful registration with valid data
    @Test
    fun `registerProduct with valid EAN, name, and packSize should return Success`() = runTest {
        // Given: Valid data
        val ean = "1234567890123"  // 13 digits
        val productName = "Arroz Blanco La Campaña"
        val packSize = 1000
        val userId = 1L
        val product = ProductCatalogEntity(
            ean = ean,
            name = productName,
            packSize = packSize,
            createdAt = Instant.now().epochSecond,
            createdBy = userId
        )

        // When: Repository succeeds
        coEvery { mockRepository.findByEan(ean) } returns null  // No duplicate
        coEvery { mockRepository.registerProduct(any()) } returns Success(product)

        // Then: UseCase should return Success
        val result = useCase(ean, productName, packSize, userId)

        assert(result is Success)
        assert((result as Success).product.ean == ean)
    }

    // TEST 2: Invalid EAN (12 digits instead of 13)
    @Test
    fun `registerProduct with 12-digit EAN should return InvalidEan error`() = runTest {
        // Given: Invalid EAN (12 digits)
        val ean = "123456789012"  // 12 digits
        val productName = "Arroz Blanco La Campaña"
        val packSize = 1000
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidEan error
        assert(result is InvalidEan)
        assert((result as InvalidEan).message == "El código debe tener 13 dígitos")
    }

    // TEST 3: Invalid name (2 characters, less than 3)
    @Test
    fun `registerProduct with 2-character name should return InvalidName error`() = runTest {
        // Given: Invalid name (2 chars)
        val ean = "1234567890123"  // Valid
        val productName = "RZ"  // Only 2 chars
        val packSize = 1000
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidName error
        assert(result is InvalidName)
        assert((result as InvalidName).message == "El nombre debe tener entre 3 y 100 caracteres")
    }

    // TEST 4: Invalid packSize (negative number)
    @Test
    fun `registerProduct with negative packSize should return InvalidPackSize error`() = runTest {
        // Given: Invalid packSize (negative)
        val ean = "1234567890123"  // Valid
        val productName = "Arroz Blanco La Campaña"
        val packSize = -100  // Negative
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidPackSize error
        assert(result is InvalidPackSize)
        assert((result as InvalidPackSize).message == "El pack size debe ser positivo")
    }

    // TEST 5: Duplicate EAN (product already exists)
    @Test
    fun `registerProduct with duplicate EAN should return DuplicateEan error`() = runTest {
        // Given: Duplicate EAN
        val ean = "1234567890123"  // 13 digits
        val productName = "Arroz Blanco La Campaña"
        val packSize = 1000
        val userId = 1L
        val existingProduct = ProductCatalogEntity(
            ean = ean,
            name = "Arroz ya existente",
            packSize = 500,
            createdAt = Instant.now().epochSecond,
            createdBy = 2L
        )

        // When: Repository finds duplicate
        coEvery { mockRepository.findByEan(ean) } returns existingProduct

        // Then: Should return DuplicateEan error with existing product name
        val result = useCase(ean, productName, packSize, userId)

        assert(result is DuplicateEan)
        assert((result as DuplicateEan).message.contains("ya existe"))
        assert((result as DuplicateEan).existingProduct.name == "Arroz ya existente")
    }

    // TEST 6: Boundary test - name exactly 3 characters
    @Test
    fun `registerProduct with 3-character name should be valid`() = runTest {
        // Given: Boundary condition (exactly 3 chars)
        val ean = "1234567890123"  // Valid
        val productName = "ABC"  // Exactly 3 chars (valid)
        val packSize = 1000
        val userId = 1L
        val product = ProductCatalogEntity(
            ean = ean,
            name = productName,
            packSize = packSize,
            createdAt = Instant.now().epochSecond,
            createdBy = userId
        )

        // When: Repository succeeds
        coEvery { mockRepository.findByEan(ean) } returns null
        coEvery { mockRepository.registerProduct(any()) } returns Success(product)

        // Then: UseCase should return Success
        val result = useCase(ean, productName, packSize, userId)

        assert(result is Success)
        assert((result as Success).product.name == "ABC")
    }

    // TEST 7: Boundary test - name exactly 100 characters
    @Test
    fun `registerProduct with 100-character name should be valid`() = runTest {
        // Given: Boundary condition (exactly 100 chars)
        val ean = "1234567890123"  // Valid
        val productName = "A".repeat(100)  // Exactly 100 chars (valid)
        val packSize = 1000
        val userId = 1L
        val product = ProductCatalogEntity(
            ean = ean,
            name = productName,
            packSize = packSize,
            createdAt = Instant.now().epochSecond,
            createdBy = userId
        )

        // When: Repository succeeds
        coEvery { mockRepository.findByEan(ean) } returns null
        coEvery { mockRepository.registerProduct(any()) } returns Success(product)

        // Then: UseCase should return Success
        val result = useCase(ean, productName, packSize, userId)

        assert(result is Success)
        assert((result as Success).product.name.length == 100)
    }

    // TEST 8: Invalid EAN with non-numeric characters
    @Test
    fun `registerProduct with non-numeric EAN should return InvalidEan error`() = runTest {
        // Given: Invalid EAN (contains letters)
        val ean = "123456789012A"  // Contains letter 'A'
        val productName = "Arroz Blanco La Campaña"
        val packSize = 1000
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidEan error
        assert(result is InvalidEan)
        assert((result as InvalidEan).message == "El código debe tener 13 dígitos")
    }

    // TEST 9: Invalid packSize (zero)
    @Test
    fun `registerProduct with zero packSize should return InvalidPackSize error`() = runTest {
        // Given: Invalid packSize (zero)
        val ean = "1234567890123"  // Valid
        val productName = "Arroz Blanco La Campaña"
        val packSize = 0  // Zero
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidPackSize error
        assert(result is InvalidPackSize)
        assert((result as InvalidPackSize).message == "El pack size debe ser positivo")
    }

    // TEST 10: Invalid name (empty string)
    @Test
    fun `registerProduct with empty name should return InvalidName error`() = runTest {
        // Given: Invalid name (empty)
        val ean = "1234567890123"  // Valid
        val productName = ""  // Empty
        val packSize = 1000
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidName error
        assert(result is InvalidName)
        assert((result as InvalidName).message == "El nombre debe tener entre 3 y 100 caracteres")
    }

    // TEST 11: Invalid name (101 characters)
    @Test
    fun `registerProduct with 101-character name should return InvalidName error`() = runTest {
        // Given: Invalid name (101 chars)
        val ean = "1234567890123"  // Valid
        val productName = "A".repeat(101)  // 101 chars (too long)
        val packSize = 1000
        val userId = 1L

        // When: Called
        val result = useCase(ean, productName, packSize, userId)

        // Then: Should return InvalidName error
        assert(result is InvalidName)
        assert((result as InvalidName).message == "El nombre debe tener entre 3 y 100 caracteres")
    }

    // TEST 12: Verify repository.registerProduct is NOT called when validation fails
    @Test
    fun `registerProduct should NOT call repository when EAN is invalid`() = runTest {
        // Given: Invalid EAN
        val ean = "123456789012"  // 12 digits
        val productName = "Arroz Blanco La Campaña"
        val packSize = 1000
        val userId = 1L

        // When: Called
        useCase(ean, productName, packSize, userId)

        // Then: Repository should NOT be called
        coVerify(exactly = 0) { mockRepository.registerProduct(any()) }
    }

    // TEST 13: Verify repository.findByEan is called for valid data
    @Test
    fun `registerProduct should call findByEan when data is valid`() = runTest {
        // Given: Valid data
        val ean = "1234567890123"
        val productName = "Arroz Blanco La Campaña"
        val packSize = 1000
        val userId = 1L

        coEvery { mockRepository.findByEan(ean) } returns null
        coEvery { mockRepository.registerProduct(any()) } returns Success(mockk())

        // When: Called
        useCase(ean, productName, packSize, userId)

        // Then: findByEan should be called
        coVerify { mockRepository.findByEan(ean) }
    }
}
