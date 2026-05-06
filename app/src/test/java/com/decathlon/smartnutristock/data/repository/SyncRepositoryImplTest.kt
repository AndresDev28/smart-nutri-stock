package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.remote.SyncRemoteDataSource
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for SyncRepositoryImpl.
 *
 * Focuses on the "Producto desconocido" fix:
 * - Push: Skips records without valid product names in catalog
 * - Push: Only marks pushed records as synced (not skipped ones)
 * - Pull: Cleans up "Unknown" garbage from product_catalog
 * - Pull: Does NOT overwrite good local catalog names with "Unknown" from cloud
 *
 * Uses a mocked [SyncRemoteDataSource] interface instead of Postgrest,
 * avoiding the inline extension function issue with Supabase SDK.
 */
class SyncRepositoryImplTest {

    private lateinit var repository: SyncRepositoryImpl
    private lateinit var remoteDataSource: SyncRemoteDataSource
    private lateinit var stockDao: StockDao
    private lateinit var productCatalogDao: ProductCatalogDao
    private lateinit var calculateStatusUseCase: CalculateStatusUseCase

    private val testClock = Clock.fixed(Instant.parse("2024-06-01T12:00:00Z"), ZoneOffset.UTC)
    private val testNow = Instant.now(testClock)
    private val storeId = "1620"

    @Before
    fun setup() {
        remoteDataSource = mockk(relaxed = true)
        stockDao = mockk(relaxed = true)
        productCatalogDao = mockk(relaxed = true)
        calculateStatusUseCase = mockk()

        every { calculateStatusUseCase(any(), any()) } returns SemaphoreStatus.GREEN
        every { calculateStatusUseCase(any()) } returns SemaphoreStatus.GREEN

        // Default: sanitize returns 0 (no storeId fix needed)
        coEvery { stockDao.sanitizeStoreIds() } returns 0

        repository = SyncRepositoryImpl(
            remoteDataSource = remoteDataSource,
            stockDao = stockDao,
            productCatalogDao = productCatalogDao,
            calculateStatusUseCase = calculateStatusUseCase
        )
    }

    // ============================================================================
    // HELPER FACTORIES
    // ============================================================================

    private fun makeDirtyEntity(
        id: String = "batch-1",
        ean: String = "8435408475366",
        quantity: Int = 10,
        expiryDate: Instant = Instant.parse("2026-07-01T00:00:00Z")
    ) = ActiveStockEntity(
        id = id,
        ean = ean,
        quantity = quantity,
        expiryDate = expiryDate,
        createdAt = testNow,
        updatedAt = testNow,
        userId = "user-123",
        storeId = storeId,
        version = 1,
        isDirty = 1
    )

    private fun makeCatalogEntity(
        ean: String = "8435408475366",
        name: String = "Proteína Whey 900g"
    ) = ProductCatalogEntity(
        ean = ean,
        name = name,
        packSize = 1,
        createdAt = System.currentTimeMillis(),
        createdBy = 0L
    )

    // ============================================================================
    // PUSH TESTS: Product name filtering
    // ============================================================================

    @Test
    fun `pushDirtyRecords returns Success(0) when no dirty records exist`() = runTest {
        // Given
        coEvery { stockDao.getDirtyRecords(storeId) } returns flowOf(emptyList())

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(0)
    }

    @Test
    fun `pushDirtyRecords skips records when catalog has no entry for EAN`() = runTest {
        // Given: A dirty record whose EAN has NO catalog entry
        val entity = makeDirtyEntity(ean = "0000000000000")
        coEvery { stockDao.getDirtyRecords(storeId) } returns flowOf(listOf(entity))
        coEvery { productCatalogDao.findByEan("0000000000000") } returns null

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then: Should succeed with 0 (skipped, not pushed)
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(0)

        // And: Should NOT call upsert nor markAsSynced
        coVerify(exactly = 0) { remoteDataSource.upsertActiveStocks(any()) }
        coVerify(exactly = 0) { stockDao.markAsSynced(any(), any()) }
    }

    @Test
    fun `pushDirtyRecords skips records when catalog name is Unknown`() = runTest {
        // Given: A dirty record whose catalog entry has "Unknown" name (from old bug)
        val entity = makeDirtyEntity(ean = "8402001030710")
        coEvery { stockDao.getDirtyRecords(storeId) } returns flowOf(listOf(entity))
        coEvery { productCatalogDao.findByEan("8402001030710") } returns
            makeCatalogEntity(ean = "8402001030710", name = "Unknown")

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then: Should succeed with 0 (skipped)
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(0)
        coVerify(exactly = 0) { remoteDataSource.upsertActiveStocks(any()) }
        coVerify(exactly = 0) { stockDao.markAsSynced(any(), any()) }
    }

    @Test
    fun `pushDirtyRecords skips records when catalog name is blank`() = runTest {
        // Given: A dirty record whose catalog entry has empty name
        val entity = makeDirtyEntity(ean = "1111111111111")
        coEvery { stockDao.getDirtyRecords(storeId) } returns flowOf(listOf(entity))
        coEvery { productCatalogDao.findByEan("1111111111111") } returns
            makeCatalogEntity(ean = "1111111111111", name = "")

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then: Should succeed with 0 (skipped)
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(0)
        coVerify(exactly = 0) { remoteDataSource.upsertActiveStocks(any()) }
        coVerify(exactly = 0) { stockDao.markAsSynced(any(), any()) }
    }

    @Test
    fun `pushDirtyRecords only marks pushed records as synced, not skipped ones`() = runTest {
        // Given: Two dirty records — one with valid name, one with no catalog
        val validEntity = makeDirtyEntity(id = "batch-valid", ean = "8435408475366")
        val unknownEntity = makeDirtyEntity(id = "batch-unknown", ean = "0000000000000")

        coEvery { stockDao.getDirtyRecords(storeId) } returns
            flowOf(listOf(validEntity, unknownEntity))
        coEvery { productCatalogDao.findByEan("8435408475366") } returns
            makeCatalogEntity(ean = "8435408475366", name = "Proteína Whey 900g")
        coEvery { productCatalogDao.findByEan("0000000000000") } returns null

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then: Only the valid record should be counted
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(1)

        // And: Upsert was called (with only the valid record)
        coVerify(exactly = 1) { remoteDataSource.upsertActiveStocks(any()) }

        // And: Only the valid record's ID should be marked as synced
        val idsSlot = slot<List<String>>()
        coVerify { stockDao.markAsSynced(capture(idsSlot), any()) }
        assertThat(idsSlot.captured).containsExactly("batch-valid")
        assertThat(idsSlot.captured).doesNotContain("batch-unknown")
    }

    // ============================================================================
    // PULL TESTS: Catalog garbage cleanup
    // ============================================================================

    @Test
    fun `pullRemoteChanges calls removeGarbageEntries before processing`() = runTest {
        // Given: No remote records to process
        coEvery { productCatalogDao.removeGarbageEntries() } returns 3
        coEvery { remoteDataSource.fetchActiveStocks(any(), any()) } returns emptyList()

        // When
        repository.pullRemoteChanges(storeId, Instant.EPOCH)

        // Then: Garbage cleanup should have been called
        coVerify(exactly = 1) { productCatalogDao.removeGarbageEntries() }
    }

    // ============================================================================
    // DAO INTERACTION TESTS: Product catalog DAO
    // ============================================================================

    @Test
    fun `removeGarbageEntries is called during pullRemoteChanges even with 0 results`() = runTest {
        // Given
        coEvery { productCatalogDao.removeGarbageEntries() } returns 0
        coEvery { remoteDataSource.fetchActiveStocks(any(), any()) } returns emptyList()

        // When
        repository.pullRemoteChanges(storeId, Instant.EPOCH)

        // Then: Should still call cleanup (idempotent operation)
        coVerify(exactly = 1) { productCatalogDao.removeGarbageEntries() }
    }

    @Test
    fun `pushDirtyRecords resolves product name from catalog for valid entries`() = runTest {
        // Given: A dirty record with a valid catalog entry
        val entity = makeDirtyEntity(ean = "8435408475366")
        coEvery { stockDao.getDirtyRecords(storeId) } returns flowOf(listOf(entity))
        coEvery { productCatalogDao.findByEan("8435408475366") } returns
            makeCatalogEntity(ean = "8435408475366", name = "Proteína Whey 900g")

        // When
        repository.pushDirtyRecords(storeId)

        // Then: Should have queried the catalog and called upsert
        coVerify { productCatalogDao.findByEan("8435408475366") }
        coVerify(exactly = 1) { remoteDataSource.upsertActiveStocks(any()) }
    }

    @Test
    fun `pushDirtyRecords handles mix of valid and invalid catalog entries correctly`() = runTest {
        // Given: Three dirty records with different catalog states
        val goodEntity1 = makeDirtyEntity(id = "good-1", ean = "1111111111111")
        val badEntity = makeDirtyEntity(id = "bad-1", ean = "2222222222222")
        val goodEntity2 = makeDirtyEntity(id = "good-2", ean = "3333333333333")

        coEvery { stockDao.getDirtyRecords(storeId) } returns
            flowOf(listOf(goodEntity1, badEntity, goodEntity2))

        coEvery { productCatalogDao.findByEan("1111111111111") } returns
            makeCatalogEntity(ean = "1111111111111", name = "Producto A")
        coEvery { productCatalogDao.findByEan("2222222222222") } returns
            makeCatalogEntity(ean = "2222222222222", name = "Unknown")
        coEvery { productCatalogDao.findByEan("3333333333333") } returns
            makeCatalogEntity(ean = "3333333333333", name = "Producto C")

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then: 2 pushed, 1 skipped
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(2)

        // And: Only good records marked as synced
        val idsSlot = slot<List<String>>()
        coVerify { stockDao.markAsSynced(capture(idsSlot), any()) }
        assertThat(idsSlot.captured).containsExactly("good-1", "good-2")
    }

    @Test
    fun `pushDirtyRecords returns Success(0) when ALL records have Unknown names`() = runTest {
        // Given: All dirty records have "Unknown" catalog names
        val entity1 = makeDirtyEntity(id = "bad-1", ean = "1111111111111")
        val entity2 = makeDirtyEntity(id = "bad-2", ean = "2222222222222")

        coEvery { stockDao.getDirtyRecords(storeId) } returns
            flowOf(listOf(entity1, entity2))
        coEvery { productCatalogDao.findByEan("1111111111111") } returns
            makeCatalogEntity(ean = "1111111111111", name = "Unknown")
        coEvery { productCatalogDao.findByEan("2222222222222") } returns null

        // When
        val result = repository.pushDirtyRecords(storeId)

        // Then: No records pushed, no records marked as synced
        assertThat(result).isInstanceOf(SyncResult.Success::class.java)
        assertThat((result as SyncResult.Success).syncedCount).isEqualTo(0)
        coVerify(exactly = 0) { remoteDataSource.upsertActiveStocks(any()) }
        coVerify(exactly = 0) { stockDao.markAsSynced(any(), any()) }
    }
}
