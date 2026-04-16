package com.decathlon.smartnutristock.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.decathlon.smartnutristock.R
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Helper class for creating and sending grouped notifications.
 *
 * This class manages notification channels for Android O+ and builds
 * grouped notifications for YELLOW and EXPIRED status alerts.
 *
 * Notifications are grouped by status (YELLOW/EXPIRED) to reduce notification spam.
 * Each group shows a summary with the count of batches in that status.
 */
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Notification Channel
        const val CHANNEL_ID = "smartnutristock_alerts"

        // Notification Group Keys
        const val GROUP_KEY_YELLOW = "smartnutristock_group_yellow"
        const val GROUP_KEY_EXPIRED = "smartnutristock_group_expired"

        // Notification IDs (unique per notification type)
        private const val NOTIFICATION_ID_YELLOW = 1001
        private const val NOTIFICATION_ID_EXPIRED = 1002

        // Pending Intent Request Codes
        private const val REQUEST_CODE_YELLOW = 2001
        private const val REQUEST_CODE_EXPIRED = 2002
    }

    /**
     * Create notification channel for Android O+.
     *
     * This should be called once during app initialization (e.g., in Application.onCreate()).
     * On Android versions below O, this method does nothing.
     */
    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                importance
            )            .apply {
                description = context.getString(R.string.notification_channel_description)
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // USE NATIVE NotificationManager — NOT NotificationManagerCompat
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Send a grouped notification for a specific status.
     *
     * This method creates a summary notification with the count of batches
     * in the specified status. The notification is grouped using the group key
     * to allow for future expansion with individual batch notifications.
     *
     * @param status The semaphore status (YELLOW or EXPIRED)
     * @param count The number of batches in this status
     * @param batchNames List of batch names (product names) for this status
     */
    fun sendGroupedNotification(
        status: SemaphoreStatus,
        count: Int,
        batchNames: List<String>
    ) {
        // ALWAYS ensure channels exist first before building notifications
        createNotificationChannel()

        // Determine notification properties based on status
        val groupKey: String
        val notificationId: Int
        val requestCode: Int
        val title: String
        val smallIcon: Int

        when (status) {
            SemaphoreStatus.YELLOW -> {
                groupKey = GROUP_KEY_YELLOW
                notificationId = NOTIFICATION_ID_YELLOW
                requestCode = REQUEST_CODE_YELLOW
                title = context.getString(R.string.notification_yellow_title, count)
                smallIcon = R.drawable.ic_notification_leaf
            }

            SemaphoreStatus.EXPIRED -> {
                groupKey = GROUP_KEY_EXPIRED
                notificationId = NOTIFICATION_ID_EXPIRED
                requestCode = REQUEST_CODE_EXPIRED
                title = context.getString(R.string.notification_expired_title, count)
                smallIcon = R.drawable.ic_notification_leaf
            }

            SemaphoreStatus.GREEN -> {
                return // Don't send notifications for GREEN status
            }
        }

        val text = formatBatchList(batchNames)

        // Create pending intent for deep link navigation
        // When user taps notification, app navigates to History screen with status filter
        val statusParam = when (status) {
            SemaphoreStatus.YELLOW -> "YELLOW"
            SemaphoreStatus.EXPIRED -> "EXPIRED"
            SemaphoreStatus.GREEN -> return // Don't send notifications for GREEN status
        }

        val deepLinkUri = "smartnutristock://history?status=$statusParam"

        // Create intent with deep link URI
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(deepLinkUri)
            `package` = context.packageName
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            pendingIntentFlags
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        // Send notification
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Format batch list into a readable string.
     *
     * Shows up to 3 batch names, with a "and X more" suffix if there are more.
     */
    private fun formatBatchList(batchNames: List<String>): String {
        return when (batchNames.size) {
            0 -> ""
            1 -> batchNames[0]
            2 -> "${batchNames[0]} y ${batchNames[1]}"
            else -> {
                val shown = batchNames.take(3).joinToString(", ")
                val remaining = batchNames.size - 3
                if (remaining > 0) {
                    "$shown y $remaining más"
                } else {
                    shown
                }
            }
        }
    }
}
