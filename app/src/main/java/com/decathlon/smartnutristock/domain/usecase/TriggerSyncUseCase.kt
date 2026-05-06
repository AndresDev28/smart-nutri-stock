package com.decathlon.smartnutristock.domain.usecase

import android.content.Context
import com.decathlon.smartnutristock.data.worker.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for triggering an immediate sync after user login.
 *
 * This use case triggers a one-time sync request to WorkManager immediately
 * after successful authentication, ensuring the Dashboard has data without
 * waiting for the next periodic sync (15 min interval).
 *
 * IMPORTANT: This should be called AFTER successful login and AFTER
 * orphan records have been claimed to ensure the session is fully initialized.
 *
 * Example usage in LoginViewModel:
 * ```
 * // After successful login
 * triggerSyncUseCase(context, storeId)
 * ```
 *
 * @property context Application context for WorkManager (injected as @ApplicationContext)
 */
class TriggerSyncUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Execute immediate sync trigger.
     *
     * Enqueues a one-time work request to WorkManager that will sync
     * products from Supabase immediately (subject to network constraints).
     *
     * @param storeId Store ID for sync operations (default "1620")
     */
    suspend operator fun invoke(storeId: String = "1620") {
        Timber.i("TriggerSync: Scheduling immediate sync for storeId=$storeId")
        SyncScheduler.triggerImmediateSync(context, storeId)
        Timber.i("TriggerSync: Immediate sync scheduled successfully")
    }
}
