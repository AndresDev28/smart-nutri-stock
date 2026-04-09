package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.dao.ProductCatalogDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import com.decathlon.smartnutristock.domain.model.WorkflowAction
import com.decathlon.smartnutristock.domain.repository.StockRepository
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Repository implementation for batch stock management.
 *
 * Features:
 * - Atomic upsert operations
 * - "Golden Rule": Automatically deletes batches when quantity <= 0
 * - Calculates semaphore status using CalculateStatusUseCase with UTC handling
 */
class StockRepositoryImpl @Inject constructor(
    private val stockDao: StockDao,
    private val productCatalogDao: ProductCatalogDao,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : StockRepository {

    @Transaction
    override suspend fun upsert(batch: Batch): UpsertBatchResult {
        return try {
            // 1. Calculate status based on expiry date
            val clock = Clock.systemUTC()
            val status = calculateStatusUseCase(batch.expiryDate, clock)

            // 2. Golden Rule: If quantity is 0 or less, delete the batch
            if (batch.quantity <= 0) {
                stockDao.deleteByEanAndExpiryDate(batch.ean, batch.expiryDate)
                return UpsertBatchResult.Deleted
            }

            // 3. Check if batch exists to decide between Insert or Update
            val existing = stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate)

            if (existing == null) {
                // Insert new batch
                val entity = batch.toEntity()
                val rowId = stockDao.insert(entity)
                if (rowId == -1L) {
                    UpsertBatchResult.Error("Failed to insert batch")
                } else {
                    UpsertBatchResult.Success(status)
                }
            } else {
                // Update existing batch — preserve existing id and createdAt, SUM quantities
                val updatedEntity = ActiveStockEntity(
                    id = existing.id,
                    ean = batch.ean,
                    quantity = existing.quantity + batch.quantity,
                    expiryDate = batch.expiryDate,
                    createdAt = existing.createdAt,
                    updatedAt = Instant.now(Clock.systemUTC()),
                    deletedAt = existing.deletedAt,
                    actionTaken = batch.actionTaken.name
                )
                stockDao.update(updatedEntity)
                UpsertBatchResult.Success(status)
            }
        } catch (e: Exception) {
            UpsertBatchResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun findByEan(ean: String): kotlinx.coroutines.flow.Flow<Batch> = kotlinx.coroutines.flow.flow {
        stockDao.findByEan(ean).forEach { entity ->
            emit(entity.toDomainModel())
        }
    }

    override suspend fun findByEanAndExpiryDate(ean: String, expiryDate: Instant): Batch? {
        return stockDao.findByEanAndExpiryDate(ean, expiryDate)?.toDomainModel()
    }

    override suspend fun findAll(): kotlinx.coroutines.flow.Flow<Batch> = kotlinx.coroutines.flow.flow {
        stockDao.findAll().forEach { entity ->
            emit(entity.toDomainModel())
        }
    }

    override suspend fun getSemaphoreCounters(): kotlinx.coroutines.flow.Flow<SemaphoreCounters> = kotlinx.coroutines.flow.flow {
        val entities = stockDao.findAll()
        var yellow = 0
        var green = 0
        var expired = 0

        val clock = Clock.systemUTC()

        entities.forEach { entity ->
            when (calculateStatusUseCase(entity.expiryDate, clock)) {
                SemaphoreStatus.EXPIRED -> expired++
                SemaphoreStatus.YELLOW -> yellow++
                SemaphoreStatus.GREEN -> green++
            }
        }

        emit(SemaphoreCounters(yellow = yellow, green = green, expired = expired))
    }

    override suspend fun deleteByEanAndExpiryDate(ean: String, expiryDate: Instant): Int {
        return stockDao.deleteByEanAndExpiryDate(ean, expiryDate)
    }

    override suspend fun deleteByEan(ean: String): Int {
        return stockDao.deleteByEan(ean)
    }

    override suspend fun findAllWithProductInfo(): kotlinx.coroutines.flow.Flow<Batch> = kotlinx.coroutines.flow.flow {
        val batchesWithInfo = stockDao.findAllWithProductInfo()
        val clock = Clock.systemUTC()

        batchesWithInfo.forEach { batchInfo ->
            val status = calculateStatusUseCase(batchInfo.expiryDate, clock)
            emit(batchInfo.toDomainModel(status))
        }
    }

    @Transaction
    override suspend fun updateBatch(batch: Batch): Int {
        return try {
            val clock = Clock.systemUTC()
            val status = calculateStatusUseCase(batch.expiryDate, clock)

            val existing = stockDao.findById(batch.id)
            if (existing == null) {
                return 0
            }

            if (existing.expiryDate == batch.expiryDate) {
                val updatedEntity = ActiveStockEntity(
                    id = existing.id,
                    ean = batch.ean,
                    quantity = batch.quantity,
                    expiryDate = batch.expiryDate,
                    createdAt = existing.createdAt,
                    updatedAt = Instant.now(clock),
                    deletedAt = existing.deletedAt,
                    actionTaken = batch.actionTaken.name
                )
                stockDao.update(updatedEntity)
            } else {
                stockDao.deleteById(existing.id)

                val targetExisting = stockDao.findByEanAndExpiryDate(batch.ean, batch.expiryDate)
                if (targetExisting != null) {
                    val mergedEntity = ActiveStockEntity(
                        id = targetExisting.id,
                        ean = batch.ean,
                        quantity = targetExisting.quantity + batch.quantity,
                        expiryDate = batch.expiryDate,
                        createdAt = targetExisting.createdAt,
                        updatedAt = Instant.now(clock),
                        deletedAt = targetExisting.deletedAt,
                        actionTaken = batch.actionTaken.name
                    )
                    stockDao.update(mergedEntity)
                } else {
                    val newEntity = ActiveStockEntity(
                        id = batch.id,
                        ean = batch.ean,
                        quantity = batch.quantity,
                        expiryDate = batch.expiryDate,
                        createdAt = existing.createdAt,
                        updatedAt = Instant.now(clock),
                        deletedAt = null,
                        actionTaken = batch.actionTaken.name
                    )
                    stockDao.insert(newEntity)
                }
            }
            1
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun softDeleteBatch(id: String, timestamp: Instant): Int {
        return stockDao.softDelete(id, timestamp)
    }

    override suspend fun restoreBatch(id: String): Int {
        return stockDao.restoreBatch(id)
    }

    override suspend fun updateProductName(ean: String, name: String): Int {
        return productCatalogDao.updateProductName(ean, name)
    }

    override suspend fun updateBatchAction(batchId: String, action: WorkflowAction): Int {
        return stockDao.updateAction(batchId, action.name)
    }

    /**
     * Extension function to convert ActiveStockEntity to Batch domain model.
     */
    private fun ActiveStockEntity.toDomainModel(): Batch {
        val clock = Clock.systemUTC()
        return Batch(
            id = this.id,
            ean = this.ean,
            quantity = this.quantity,
            expiryDate = this.expiryDate,
            status = calculateStatusUseCase(this.expiryDate, clock),
            deletedAt = this.deletedAt,
            actionTaken = WorkflowAction.valueOf(this.actionTaken)
        )
    }

    /**
     * Extension function to convert Batch to ActiveStockEntity.
     */
    private fun Batch.toEntity(): ActiveStockEntity {
        val now = Instant.now(Clock.systemUTC())
        return ActiveStockEntity(
            id = this.id,
            ean = this.ean,
            quantity = this.quantity,
            expiryDate = this.expiryDate,
            createdAt = now,
            updatedAt = now,
            deletedAt = this.deletedAt,
            actionTaken = this.actionTaken.name
        )
    }
}
