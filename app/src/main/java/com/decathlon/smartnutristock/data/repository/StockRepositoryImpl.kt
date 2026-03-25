package com.decathlon.smartnutristock.data.repository

import com.decathlon.smartnutristock.data.dao.StockDao
import com.decathlon.smartnutristock.data.entity.ActiveStockEntity
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import com.decathlon.smartnutristock.domain.repository.StockRepository
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
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
    private val calculateStatusUseCase: CalculateStatusUseCase
) : StockRepository {

    override suspend fun upsert(batch: Batch): UpsertBatchResult {
        return try {
            // 1. Calculate status based on expiry date
            val status = calculateStatusUseCase(batch.expiryDate)

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
                // Update existing batch
                val updatedEntity = batch.toEntity()
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
        var red = 0
        var yellow = 0
        var green = 0
        var expired = 0

        entities.forEach { entity ->
            when (calculateStatusUseCase(entity.expiryDate)) {
                SemaphoreStatus.EXPIRED -> expired++
                SemaphoreStatus.RED -> red++
                SemaphoreStatus.YELLOW -> yellow++
                SemaphoreStatus.GREEN -> green++
            }
        }

        emit(SemaphoreCounters(red = red, yellow = yellow, green = green, expired = expired))
    }

    override suspend fun deleteByEanAndExpiryDate(ean: String, expiryDate: Instant): Int {
        return stockDao.deleteByEanAndExpiryDate(ean, expiryDate)
    }

    override suspend fun deleteByEan(ean: String): Int {
        return stockDao.deleteByEan(ean)
    }

    /**
     * Extension function to convert ActiveStockEntity to Batch domain model.
     */
    private fun ActiveStockEntity.toDomainModel(): Batch {
        return Batch(
            id = this.id,
            ean = this.ean,
            quantity = this.quantity,
            expiryDate = this.expiryDate,
            status = calculateStatusUseCase(this.expiryDate)
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
            updatedAt = now
        )
    }
}
