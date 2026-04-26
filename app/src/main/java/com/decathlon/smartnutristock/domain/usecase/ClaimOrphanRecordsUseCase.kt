package com.decathlon.smartnutristock.domain.usecase

import com.decathlon.smartnutristock.domain.repository.SyncRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for claiming orphan records after first successful login.
 *
 * This use case assigns real userId and storeId to all records where userId IS NULL
 * or userId == ''. These are "orphan" records that were created before authentication
 * was implemented or while the app was offline without a user session.
 *
 * IMPORTANT: This should be executed ONCE after the first successful login.
 * Use a flag (SharedPreferences or DataStore) to track if cleanup has been done.
 * The flag name should be: "orphan_cleanup_done" (boolean).
 *
 * Example usage in LoginViewModel:
 * ```
 * val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
 * val orphanCleanupDone = prefs.getBoolean("orphan_cleanup_done", false)
 *
 * if (!orphanCleanupDone && user != null) {
 *     val result = claimOrphanRecordsUseCase(user.id, user.storeId)
 *     if (result.isSuccess) {
 *         prefs.edit().putBoolean("orphan_cleanup_done", true).apply()
 *     }
 * }
 * ```
 *
 * @property syncRepository Repository for sync operations
 */
@Singleton
class ClaimOrphanRecordsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {

    /**
     * Execute orphan records cleanup for a user and store.
     *
     * Assigns real userId and storeId to all records with null/empty userId.
     * The default storeId is "1620" (Decathlon Gandía).
     *
     * @param userId User ID to assign to orphan records
     * @param storeId Store ID to assign (default "1620")
     * @return Result containing count of updated records on success, or error on failure
     */
    suspend operator fun invoke(
        userId: String,
        storeId: String = "1620"
    ): Result<Int> {
        Timber.i("Orphan cleanup: Starting for userId=$userId, storeId=$storeId")

        return try {
            val result = syncRepository.claimOrphanRecords(userId, storeId)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                Timber.i("Orphan cleanup: Successfully updated $count records")
            } else {
                Timber.e("Orphan cleanup: Failed - ${result.exceptionOrNull()?.message}")
            }
            result
        } catch (e: Exception) {
            Timber.e(e, "Orphan cleanup: Unexpected error")
            Result.failure(e)
        }
    }
}
