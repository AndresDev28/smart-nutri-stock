package com.decathlon.smartnutristock.presentation.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for NotificationPermissionHandler.
 *
 * Tests verify:
 * - Permission check logic for granted/denied states
 * - Backward compatibility (API < 33 → auto-granted)
 * - Rationale dialog logic (shouldShowRationale)
 * - Permanent denial detection
 * - Settings intent creation
 */
@RunWith(AndroidJUnit4::class)
class NotificationPermissionHandlerTest {

    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity

    @Before
    fun setup() {
        mockContext = mockk()
        mockActivity = mockk()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ===== isPermissionRequired Tests =====

    @Test
    fun isPermissionRequired_returns_false_on_API_32() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 32

        // When
        val result = NotificationPermissionHandler.isPermissionRequired()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun isPermissionRequired_returns_false_on_API_31() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 31

        // When
        val result = NotificationPermissionHandler.isPermissionRequired()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun isPermissionRequired_returns_true_on_API_33() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33

        // When
        val result = NotificationPermissionHandler.isPermissionRequired()

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun isPermissionRequired_returns_true_on_API_34() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 34

        // When
        val result = NotificationPermissionHandler.isPermissionRequired()

        // Then
        assertThat(result).isTrue()
    }

    // ===== checkPermission Tests =====

    @Test
    fun checkPermission_returns_true_on_API_32_auto_granted() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 32

        // When
        val result = NotificationPermissionHandler.checkPermission(mockContext)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun checkPermission_returns_true_on_API_31_auto_granted() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 31

        // When
        val result = NotificationPermissionHandler.checkPermission(mockContext)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun checkPermission_returns_true_when_permission_is_granted_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        mockkStatic("androidx.core.content.ContextCompat")
        every {
            mockContext.checkSelfPermission(any())
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = NotificationPermissionHandler.checkPermission(mockContext)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun checkPermission_returns_false_when_permission_is_denied_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        mockkStatic("androidx.core.content.ContextCompat")
        every {
            mockContext.checkSelfPermission(any())
        } returns PackageManager.PERMISSION_DENIED

        // When
        val result = NotificationPermissionHandler.checkPermission(mockContext)

        // Then
        assertThat(result).isFalse()
    }

    // ===== shouldShowRationale Tests =====

    @Test
    fun shouldShowRationale_returns_false_on_API_32() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 32

        // When
        val result = NotificationPermissionHandler.shouldShowRationale(mockActivity)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun shouldShowRationale_returns_false_on_API_31() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 31

        // When
        val result = NotificationPermissionHandler.shouldShowRationale(mockActivity)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun shouldShowRationale_returns_true_when_activity_returns_true_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        every {
            mockActivity.shouldShowRequestPermissionRationale(any())
        } returns true

        // When
        val result = NotificationPermissionHandler.shouldShowRationale(mockActivity)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun shouldShowRationale_returns_false_when_activity_returns_false_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        every {
            mockActivity.shouldShowRequestPermissionRationale(any())
        } returns false

        // When
        val result = NotificationPermissionHandler.shouldShowRationale(mockActivity)

        // Then
        assertThat(result).isFalse()
    }

    // ===== isPermanentlyDenied Tests =====

    @Test
    fun isPermanentlyDenied_returns_false_on_API_32() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 32

        // When
        val result = NotificationPermissionHandler.isPermanentlyDenied(mockActivity)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun isPermanentlyDenied_returns_false_when_permission_is_granted_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        mockkStatic("androidx.core.content.ContextCompat")
        every {
            mockActivity.checkSelfPermission(any())
        } returns PackageManager.PERMISSION_GRANTED

        // When
        val result = NotificationPermissionHandler.isPermanentlyDenied(mockActivity)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun isPermanentlyDenied_returns_false_when_user_denied_but_can_ask_again_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        mockkStatic("androidx.core.content.ContextCompat")
        every {
            mockActivity.checkSelfPermission(any())
        } returns PackageManager.PERMISSION_DENIED
        every {
            mockActivity.shouldShowRequestPermissionRationale(any())
        } returns true

        // When
        val result = NotificationPermissionHandler.isPermanentlyDenied(mockActivity)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun isPermanentlyDenied_returns_true_when_user_denied_permanently_on_API_33_plus() {
        // Given
        mockkStatic(Build.VERSION::class)
        every { Build.VERSION.SDK_INT } returns 33
        mockkStatic("androidx.core.content.ContextCompat")
        every {
            mockActivity.checkSelfPermission(any())
        } returns PackageManager.PERMISSION_DENIED
        every {
            mockActivity.shouldShowRequestPermissionRationale(any())
        } returns false

        // When
        val result = NotificationPermissionHandler.isPermanentlyDenied(mockActivity)

        // Then
        assertThat(result).isTrue()
    }

    // ===== createSettingsIntent Tests =====

    @Test
    fun createSettingsIntent_creates_intent_with_correct_action_and_URI() {
        // Given
        every { mockContext.packageName } returns "com.decathlon.smartnutristock"

        // When
        val intent = NotificationPermissionHandler.createSettingsIntent(mockContext)

        // Then
        assertThat(intent.action).isEqualTo("android.settings.APPLICATION_DETAILS_SETTINGS")
        assertThat(intent.data.toString()).isEqualTo("package:com.decathlon.smartnutristock")
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0)
    }

    @Test
    fun createSettingsIntent_uses_correct_package_name() {
        // Given
        val packageName = "com.example.testapp"
        every { mockContext.packageName } returns packageName

        // When
        val intent = NotificationPermissionHandler.createSettingsIntent(mockContext)

        // Then
        assertThat(intent.data.toString()).isEqualTo("package:$packageName")
    }
}
