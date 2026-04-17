package com.decathlon.smartnutristock.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.decathlon.smartnutristock.data.notification.NotificationHelper
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.repository.StockRepository
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList

/**
 * Background worker that checks all stock batches for expiry status.
 *
 * This worker runs daily (scheduled at 06:00 AM) to:
 * 1. Fetch all batches from the database
 * 2. Calculate status for each batch using CalculateStatusUseCase
 * 3. Group batches by YELLOW and EXPIRED status
 * 4. Send grouped notifications for each status that has batches
 *
 * Notifications are grouped by status to reduce notification spam:
 * - YELLOW: "Tienes X lotes por caducar pronto" (1-7 days remaining)
 * - EXPIRED: "Tienes X lotes caducados" (<= 0 days remaining)
 *
 * GREEN status batches do NOT generate notifications (safe to ignore).
 */
@HiltWorker
class StatusCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val stockRepository: StockRepository,
    private val notificationHelper: NotificationHelper,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            // 0. Ensure notification channel exists before any notification logic
            notificationHelper.createNotificationChannel()

            // 1. Fetch all batches with product information
            val allBatches = stockRepository.findAllWithProductInfo().toList()

            // 2. Calculate status for each batch and group by YELLOW/EXPIRED
            val yellowBatches = mutableListOf<String>()
            val expiredBatches = mutableListOf<String>()

            for (batch in allBatches) {
                val status = calculateStatusUseCase(batch.expiryDate)

                when (status) {
                    SemaphoreStatus.YELLOW -> {
                        // Use product name if available, otherwise use EAN
                        batch.name?.let { yellowBatches.add(it) }
                    }

                    SemaphoreStatus.EXPIRED -> {
                        // Use product name if available, otherwise use EAN
                        batch.name?.let { expiredBatches.add(it) }
                    }

                    SemaphoreStatus.GREEN -> {
                        // Don't send notifications for GREEN status (safe to ignore)
                    }
                }
            }

            // 3. Send grouped notifications if there are batches in each status
            if (yellowBatches.isNotEmpty()) {
                notificationHelper.sendGroupedNotification(
                    status = SemaphoreStatus.YELLOW,
                    count = yellowBatches.size,
                    batchNames = yellowBatches
                )
            }

            if (expiredBatches.isNotEmpty()) {
                notificationHelper.sendGroupedNotification(
                    status = SemaphoreStatus.EXPIRED,
                    count = expiredBatches.size,
                    batchNames = expiredBatches
                )
            }

            // 4. Return success
            return Result.success()

        } catch (hacerloe: Exception) {
            // Log error and return failure (WorkManager will retry with backoff)
            // TODO: Add proper logging when error tracking is implemented
            return Result.failure()
        }
    }
}
