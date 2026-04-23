package com.decathlon.smartnutristock.domain.model

/**
 * Synchronization result sealed class.
 *
 * Represents the outcome of a synchronization operation.
 * Used to communicate sync status to the UI layer.
 *
 * Pure Kotlin sealed class - NO Room or Supabase annotations.
 */
sealed class SyncResult {
    /**
     * Synchronization completed successfully.
     *
     * @property syncedCount Number of records successfully synced
     */
    data class Success(val syncedCount: Int) : SyncResult()

    /**
     * Partial synchronization success.
     *
     * Some records synced, some failed.
     *
     * @property syncedCount Number of records successfully synced
     * @property failedCount Number of records that failed to sync
     * @property error Optional error message describing failures
     */
    data class PartialSuccess(
        val syncedCount: Int,
        val failedCount: Int,
        val error: String? = null
    ) : SyncResult()

    /**
     * Synchronization failed completely.
     *
     * @property message Error message describing what went wrong
     * @property cause Optional underlying exception
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : SyncResult()
}
