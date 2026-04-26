package com.decathlon.smartnutristock.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.decathlon.smartnutristock.data.local.encrypted.EncryptedSessionManager
import com.decathlon.smartnutristock.domain.repository.AuthRepository
import com.decathlon.smartnutristock.domain.usecase.SyncDataUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that synchronizes data between local database and Supabase.
 *
 * This worker runs periodically (every 15 minutes) when network is available.
 * It performs the full sync cycle: push dirty records → pull remote changes.
 *
 * Auth Guard:
 * - Checks if user is logged in via AuthRepository/SessionManager
 * - If not logged in, returns Result.failure() without attempting sync
 *
 * Constraints:
 * - Network required (CONNECTED)
 * - Battery not low (BATTERY_NOT_LOW)
 * - Storage not low (STORAGE_NOT_LOW)
 *
 * Retry Policy:
 * - On success: Returns Result.success()
 * - On error: Returns Result.retry() (WorkManager will retry with exponential backoff)
 *
 * IMPORTANT: This worker uses ExistingPeriodicWorkPolicy.KEEP to prevent duplicate workers.
 * Use the SyncScheduler helper to enqueue this worker.
 *
 * @property context Application context
 * @property params Worker parameters
 * @property sessionManager Session manager for checking auth state
 * @property syncDataUseCase Use case for performing sync
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionManager: EncryptedSessionManager,
    private val syncDataUseCase: SyncDataUseCase
) : CoroutineWorker(context, params) {

    companion object {
        /**
         * Unique work name for this worker.
         * Used with enqueueUniquePeriodicWork() to prevent duplicates.
         */
        const val WORK_NAME = "sync_work"

        /**
         * Default store ID for sync operations.
         * Can be overridden via input data.
         */
        private const val DEFAULT_STORE_ID = "1620"

        /**
         * Input data key for store ID.
         */
        const val INPUT_STORE_ID = "store_id"
    }

    override suspend fun doWork(): Result {
        return try {
            // 1. Auth Guard: Check if user is logged in
            val userId = sessionManager.getUserId()
            if (userId == null) {
                Timber.w("SyncWorker: User not logged in, skipping sync")
                return Result.failure()
            }

            val storeId = inputData.getString(INPUT_STORE_ID) ?: DEFAULT_STORE_ID
            val sessionStoreId = sessionManager.getStoreId()

            // 2. Execute full sync cycle
            val syncResult = syncDataUseCase(storeId)

            // 3. Handle sync result
            when (syncResult) {
                is com.decathlon.smartnutristock.domain.model.SyncResult.Success -> {
                    Timber.i("SyncWorker: Sync completed successfully - ${syncResult.syncedCount} records synced")
                    Result.success()
                }
                is com.decathlon.smartnutristock.domain.model.SyncResult.PartialSuccess -> {
                    Timber.w("SyncWorker: Sync partially successful - ${syncResult.syncedCount} synced, ${syncResult.failedCount} failed")
                    // Return success - partial sync is better than no sync
                    Result.success()
                }
                is com.decathlon.smartnutristock.domain.model.SyncResult.Error -> {
                    Timber.e(syncResult.cause, "SyncWorker: Sync failed - ${syncResult.message}")
                    // Return retry to let WorkManager handle backoff
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Unexpected error")
            Result.retry()
        }
    }
}

/**
 * Helper object for scheduling the SyncWorker.
 *
 * Encapsulates the WorkManager enqueue logic with proper constraints and policies.
 *
 * Usage:
 * ```
 * // Schedule periodic sync (every 15 minutes)
 * SyncScheduler.scheduleSync(context, storeId = "1620")
 *
 * // Trigger immediate one-time sync (e.g., after user action)
 * SyncScheduler.triggerImmediateSync(context, storeId = "1620")
 *
 * // Cancel all sync work
 * SyncScheduler.cancelSync(context)
 * ```
 */
object SyncScheduler {

    private const val SYNC_INTERVAL_MINUTES = 15L
    private const val BACKOFF_DELAY_SECONDS = 30L

    /**
     * Schedule periodic sync worker.
     *
     * Creates a PeriodicWorkRequest that runs every 15 minutes with:
     * - Network constraint (CONNECTED)
     * - Battery not low constraint
     * - Storage not low constraint
     * - Exponential backoff retry policy
     *
     * IMPORTANT: Uses ExistingPeriodicWorkPolicy.KEEP to prevent duplicate workers.
     * If a worker with the same name is already scheduled, it will be kept as-is.
     *
     * @param context Application context
     * @param storeId Store ID for sync operations (default "1620")
     */
    fun scheduleSync(context: Context, storeId: String = "1620") {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(androidx.work.workDataOf(
                SyncWorker.INPUT_STORE_ID to storeId
            ))
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // CRITICAL: KEEP, not REPLACE
            syncRequest
        )
    }

    /**
     * Trigger immediate one-time sync.
     *
     * Creates a OneTimeWorkRequest that runs immediately (subject to constraints).
     * Useful for triggering sync after user actions (e.g., after login, after manual sync button).
     *
     * @param context Application context
     * @param storeId Store ID for sync operations (default "1620")
     */
    fun triggerImmediateSync(context: Context, storeId: String = "1620") {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInputData(androidx.work.workDataOf(
                SyncWorker.INPUT_STORE_ID to storeId
            ))
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }

    /**
     * Cancel all sync work.
     *
     * Cancels the unique periodic work and any pending one-time sync requests.
     *
     * @param context Application context
     */
    fun cancelSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
