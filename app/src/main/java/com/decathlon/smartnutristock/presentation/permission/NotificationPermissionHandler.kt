package com.decathlon.smartnutristock.presentation.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.decathlon.smartnutristock.R

/**
 * Permission handler for POST_NOTIFICATIONS (Android 13+).
 *
 * This class provides methods to check, request, and handle the notification permission
 * introduced in Android 13 (API level 33). On Android 12 and below, the permission
 * is auto-granted and does not need to be requested.
 *
 * Backward Compatibility:
 * - API 33+ (Android 13+): POST_NOTIFICATIONS permission is required
 * - API 32 and below (Android 12 and earlier): Permission is auto-granted
 */
class NotificationPermissionHandler {

    companion object {
        /**
         * Check if POST_NOTIFICATIONS permission is required for this Android version.
         *
         * @return true if running on Android 13+ (API 33+), false otherwise
         */
        fun isPermissionRequired(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }

        /**
         * Check if POST_NOTIFICATIONS permission is granted.
         *
         * On Android 12 and below, this always returns true (auto-granted).
         *
         * @param context The application context
         * @return true if permission is granted, false otherwise
         */
        fun checkPermission(context: Context): Boolean {
            // On Android 12 and below, permission is auto-granted
            if (!isPermissionRequired()) {
                return true
            }

            // On Android 13+, check if permission is granted
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        /**
         * Check if we should show a rationale dialog for the permission request.
         *
         * This should be called when the user has previously denied the permission.
         * The rationale dialog explains WHY the permission is needed before requesting again.
         *
         * @param activity The activity requesting the permission
         * @return true if we should show rationale, false otherwise
         */
        fun shouldShowRationale(activity: Activity): Boolean {
            // On Android 12 and below, never show rationale (no permission needed)
            if (!isPermissionRequired()) {
                return false
            }

            // On Android 13+, check if user previously denied and "Don't ask again" was NOT checked
            return activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        }

        /**
         * Check if the user has permanently denied the permission.
         *
         * This is true when the user denied and checked "Don't ask again".
         * In this case, we should direct them to app settings.
         *
         * @param activity The activity that requested the permission
         * @return true if permanently denied, false otherwise
         */
        fun isPermanentlyDenied(activity: Activity): Boolean {
            // On Android 12 and below, never permanently denied (no permission needed)
            if (!isPermissionRequired()) {
                return false
            }

            // On Android 13+, permanently denied if:
            // 1. Permission is not granted AND
            // 2. shouldShowRequestPermissionRationale returns false
            return !checkPermission(activity) &&
                    !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        }

        /**
         * Create an intent to open the app's notification settings.
         *
         * This should be used when the user has permanently denied the permission.
         *
         * @param context The context to create the intent from
         * @return Intent to open app settings
         */
        fun createSettingsIntent(context: Context): Intent {
            return Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Rationale dialog state.
     *
     * This sealed class represents the different states of the rationale dialog:
     * - NotShowing: Dialog is not visible
     * - Showing: Dialog is visible with callbacks for confirm/dismiss actions
     */
    sealed class RationaleDialogState {
        data object NotShowing : RationaleDialogState()
        data class Showing(
            val onConfirm: () -> Unit,
            val onDismiss: () -> Unit
        ) : RationaleDialogState()
    }
}

/**
 * Composable rationale dialog for notification permission.
 *
 * This dialog explains why notifications are needed for the app to function.
 * It's shown when the user has previously denied the permission request.
 *
 * @param onConfirm Callback when user accepts and wants to grant permission
 * @param onDismiss Callback when user dismisses the dialog without granting
 */
@Composable
fun NotificationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = stringResource(R.string.notification_rationale_title)
            )
        },
        text = {
            androidx.compose.material3.Text(
                text = stringResource(R.string.notification_rationale_message)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.notification_rationale_confirm)
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss
            ) {
                androidx.compose.material3.Text(
                    text = stringResource(R.string.notification_rationale_dismiss)
                )
            }
        }
    )
}
