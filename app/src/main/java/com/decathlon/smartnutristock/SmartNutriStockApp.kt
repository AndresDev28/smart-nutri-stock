package com.decathlon.smartnutristock

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.decathlon.smartnutristock.data.worker.StatusCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main application class for Smart Nutri Stock.
 *
 * This class initializes:
 * - Hilt for dependency injection
 * - WorkManager for background tasks
 * - Periodic worker for daily status checks
 *
 * Implements Configuration.Provider to enable Hilt Worker factory
 * for dependency injection in workers.
 */
@HiltAndroidApp
class SmartNutriStockApp : Application(), androidx.work.Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager and enqueue StatusCheckWorker
        initializeStatusCheckWorker()

        // Log database state for debugging
        logDatabaseState()
    }

    /**
     * Initialize the StatusCheckWorker for daily background checks.
     *
     * Worker is scheduled to run every 24 hours at 06:00 AM.
     * Uses smart enqueue policy: REPLACE only if existing work is FAILED,
     * otherwise KEEP to prevent duplicate workers.
     * Uses EXPONENTIAL backoff for reliability.
     */
    private fun initializeStatusCheckWorker() {
        val workManager = WorkManager.getInstance(this)

        // Calculate initial delay to run at 06:00 AM
        val now = System.currentTimeMillis()
        val sixAmToday = getSixAmToday()
        val initialDelay = if (now < sixAmToday) {
            sixAmToday - now
        } else {
            sixAmToday + (24 * 60 * 60 * 1000) - now
        }

        val statusCheckRequest = PeriodicWorkRequestBuilder<StatusCheckWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30, TimeUnit.SECONDS
            )
            .build()

        // Smart enqueue: REPLACE only if existing work is FAILED, otherwise KEEP
        try {
            val workInfos = workManager.getWorkInfosForUniqueWork(STATUS_CHECK_WORK_NAME).get()

            val shouldReplace = workInfos.isEmpty() ||
                workInfos.all { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }

            val policy = if (shouldReplace) {
                ExistingPeriodicWorkPolicy.REPLACE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            workManager.enqueueUniquePeriodicWork(
                STATUS_CHECK_WORK_NAME,
                policy,
                statusCheckRequest
            )
        } catch (e: Exception) {
            // Fallback: if we can't check status, use REPLACE to ensure fresh schedule
            workManager.enqueueUniquePeriodicWork(
                STATUS_CHECK_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                statusCheckRequest
            )
        }
    }

    /**
     * Get the timestamp for 06:00 AM today.
     */
    private fun getSixAmToday(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 6)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Log database state for debugging purposes.
     *
     * Uses a background thread/coroutine to query database without blocking main thread.
     * Since this is Application.onCreate(), use CoroutineScope(Dispatchers.IO).
     */
    private fun logDatabaseState() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("INIT_DEBUG", "App initialized - checking database state")
                Log.d("INIT_DEBUG", "WorkManager scheduled - check Background Task Inspector")
            } catch (e: Exception) {
                Log.e("INIT_DEBUG", "Failed to query database state", e)
            }
        }
    }

    companion object {
        private const val STATUS_CHECK_WORK_NAME = "status_check_worker"
    }
}
