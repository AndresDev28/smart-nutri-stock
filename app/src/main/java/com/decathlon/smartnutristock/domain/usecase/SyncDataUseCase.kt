package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.model.SyncResult
import com.decathlon.smartnutristock.domain.repository.SyncRepository
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Use case for synchronizing data between local database and remote Supabase.
 *
 * This use case orchestrates the full sync cycle:
 * 1. Push dirty records (local → cloud)
 * 2. Pull remote changes (cloud → local)
 * 3. Return SyncResult with counts
 *
 * Error handling:
 * - If push fails, don't pull (fail fast)
 * - If pull fails, report partial success (push was successful)
 * - Log all sync operations with Timber
 *
 * @property syncRepository Repository for sync operations
 * @property clock Clock for timestamp operations (injectable for testing)
 */
class SyncDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    /**
     * Execute full sync cycle for a specific store.
     *
     * Process:
     * 1. Get last sync time
     * 2. Push dirty records to cloud
     * 3. If push succeeds, pull remote changes from cloud
     * 4. Return combined sync result
     *
     * @param storeId Store identifier to scope the sync (default "1620")
     * @return SyncResult indicating overall success/failure and counts
     */
    suspend operator fun invoke(storeId: String = "1620"): SyncResult {
        Timber.d("Sync: Starting sync cycle for storeId=$storeId")

        // 1. Get last sync time
        val lastSyncTime = syncRepository.getLastSyncTime(storeId)
        Timber.d("Sync: Last sync time = $lastSyncTime")

        // 2. Push dirty records (local → cloud)
        val pushResult = syncRepository.pushDirtyRecords(storeId)

        when (pushResult) {
            is SyncResult.Success -> {
                Timber.d("Sync: Push successful - ${pushResult.syncedCount} records")
            }
            is SyncResult.PartialSuccess -> {
                Timber.w("Sync: Push partial success - ${pushResult.syncedCount} synced, ${pushResult.failedCount} failed")
                // Don't pull if push had failures - might cause data inconsistency
                return SyncResult.PartialSuccess(
                    syncedCount = pushResult.syncedCount,
                    failedCount = pushResult.failedCount,
                    error = pushResult.error
                )
            }
            is SyncResult.Error -> {
                Timber.e(pushResult.cause, "Sync: Push failed - ${pushResult.message}")
                // Don't pull if push failed completely
                return SyncResult.Error(
                    message = pushResult.message,
                    cause = pushResult.cause
                )
            }
        }

        // 3. Pull remote changes (cloud → local)
        // Use the time BEFORE push as baseline to pull changes that happened before our push
        val pullBaseline = lastSyncTime ?: Instant.EPOCH
        val pullResult = syncRepository.pullRemoteChanges(storeId, pullBaseline)

        when (pullResult) {
            is SyncResult.Success -> {
                Timber.d("Sync: Pull successful - ${pullResult.syncedCount} records")
                return SyncResult.Success(
                    syncedCount = pushResult.syncedCount + pullResult.syncedCount
                )
            }
            is SyncResult.PartialSuccess -> {
                Timber.w("Sync: Pull partial success - ${pullResult.syncedCount} synced, ${pullResult.failedCount} failed")
                return SyncResult.PartialSuccess(
                    syncedCount = pushResult.syncedCount + pullResult.syncedCount,
                    failedCount = pullResult.failedCount,
                    error = pullResult.error
                )
            }
            is SyncResult.Error -> {
                Timber.e(pullResult.cause, "Sync: Pull failed - ${pullResult.message}")
                // Push succeeded, but pull failed - report partial success
                return SyncResult.PartialSuccess(
                    syncedCount = pushResult.syncedCount,
                    failedCount = 0,
                    error = "Push succeeded, but pull failed: ${pullResult.message}"
                )
            }
        }
    }
}
