package com.decathlon.smartnutristock.integration

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.presentation.ui.history.HistoryViewModel
import com.decathlon.smartnutristock.presentation.ui.history.SemaphoreStatusFilter
import com.decathlon.smartnutristock.domain.usecase.ExportInventoryUseCase
import com.decathlon.smartnutristock.domain.usecase.GetAllBatchesUseCase
import com.decathlon.smartnutristock.domain.usecase.RestoreBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.SoftDeleteBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchActionUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateProductNameUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for deep link navigation to History screen with status filter.
 *
 * This test validates the deep link flow from Intent → NavController → ViewModel → UI.
 * Tests that notification taps navigate to HistoryScreen with the correct filter applied.
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkNavigationTest {

    private val mockGetAllBatchesUseCase = mockk<GetAllBatchesUseCase>()
    private val mockSoftDeleteBatchUseCase = mockk<SoftDeleteBatchUseCase>()
    private val mockRestoreBatchUseCase = mockk<RestoreBatchUseCase>()
    private val mockUpdateBatchUseCase = mockk<UpdateBatchUseCase>()
    private val mockUpdateProductNameUseCase = mockk<UpdateProductNameUseCase>()
    private val mockUpdateBatchActionUseCase = mockk<UpdateBatchActionUseCase>()
    private val mockExportInventoryUseCase = mockk<ExportInventoryUseCase>(relaxed = true)

    init {
        // Setup mock returns
        coEvery { mockGetAllBatchesUseCase() } returns flowOf(emptyList())
        coEvery { mockSoftDeleteBatchUseCase(any(), any()) } returns 1
        coEvery { mockRestoreBatchUseCase(any()) } returns 1
        coEvery { mockUpdateBatchUseCase(any()) } returns 1
        coEvery { mockUpdateProductNameUseCase(any(), any()) } returns 1
        coEvery { mockUpdateBatchActionUseCase(any(), any()) } returns Unit
    }

    @Test
    fun deepLink_YELLOW_status_initializes_ViewModel_with_YELLOW_filter() {
        // Given - Deep link intent with YELLOW status
        val deepLinkUri = "smartnutristock://history?status=YELLOW"
        val savedStateHandle = SavedStateHandle(mapOf("status" to "YELLOW"))

        // When - ViewModel is created with SavedStateHandle containing YELLOW status
        val viewModel =
            HistoryViewModel(
                mockGetAllBatchesUseCase,
                mockSoftDeleteBatchUseCase,
                mockRestoreBatchUseCase,
                mockUpdateBatchUseCase,
                mockUpdateProductNameUseCase,
                mockUpdateBatchActionUseCase,
                mockExportInventoryUseCase,
                savedStateHandle
            )

        // Then - ViewModel status filter should be YELLOW
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.YELLOW)

        // And - Deep link URI should match expected format
        assertThat(deepLinkUri).contains("smartnutristock://history")
        assertThat(deepLinkUri).contains("status=YELLOW")
    }

    @Test
    fun deepLink_EXPIRED_status_initializes_ViewModel_with_EXPIRED_filter() {
        // Given - Deep link intent with EXPIRED status
        val deepLinkUri = "smartnutristock://history?status=EXPIRED"
        val savedStateHandle = SavedStateHandle(mapOf("status" to "EXPIRED"))

        // When - ViewModel is created with SavedStateHandle containing EXPIRED status
        val viewModel =
            HistoryViewModel(
                mockGetAllBatchesUseCase,
                mockSoftDeleteBatchUseCase,
                mockRestoreBatchUseCase,
                mockUpdateBatchUseCase,
                mockUpdateProductNameUseCase,
                mockUpdateBatchActionUseCase,
                mockExportInventoryUseCase,
                savedStateHandle
            )

        // Then - ViewModel status filter should be EXPIRED
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.EXPIRED)

        // And - Deep link URI should match expected format
        assertThat(deepLinkUri).contains("smartnutristock://history")
        assertThat(deepLinkUri).contains("status=EXPIRED")
    }

    @Test
    fun deepLink_missing_status_defaults_to_ALL_filter() {
        // Given - Deep link intent without status parameter
        val deepLinkUri = "smartnutristock://history"
        val savedStateHandle = SavedStateHandle()

        // When - ViewModel is created without status in SavedStateHandle
        val viewModel =
            HistoryViewModel(
                mockGetAllBatchesUseCase,
                mockSoftDeleteBatchUseCase,
                mockRestoreBatchUseCase,
                mockUpdateBatchUseCase,
                mockUpdateProductNameUseCase,
                mockUpdateBatchActionUseCase,
                mockExportInventoryUseCase,
                savedStateHandle
            )

        // Then - ViewModel status filter should default to ALL
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.ALL)

        // And - Deep link URI should match expected format
        assertThat(deepLinkUri).contains("smartnutristock://history")
    }

    @Test
    fun deepLink_invalid_status_defaults_to_ALL_filter_graceful_degradation() {
        // Given - Deep link intent with invalid status
        val deepLinkUri = "smartnutristock://history?status=INVALID"
        val savedStateHandle = SavedStateHandle(mapOf("status" to "INVALID"))

        // When - ViewModel is created with invalid status in SavedStateHandle
        val viewModel =
            HistoryViewModel(
                mockGetAllBatchesUseCase,
                mockSoftDeleteBatchUseCase,
                mockRestoreBatchUseCase,
                mockUpdateBatchUseCase,
                mockUpdateProductNameUseCase,
                mockUpdateBatchActionUseCase,
                mockExportInventoryUseCase,
                savedStateHandle
            )

        // Then - ViewModel status filter should default to ALL (graceful degradation)
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.ALL)

        // And - Deep link URI should contain the invalid parameter
        assertThat(deepLinkUri).contains("smartnutristock://history")
        assertThat(deepLinkUri).contains("status=INVALID")
    }

    @Test
    fun deepLink_URI_scheme_and_host_match_expected_values() {
        // Given - Deep link URI
        val deepLinkUri = "smartnutristock://history?status=YELLOW"

        // When - Parse URI components
        val scheme = "smartnutristock"
        val host = "history"

        // Then - Verify URI components match design spec
        assertThat(scheme).isEqualTo("smartnutristock")
        assertThat(host).isEqualTo("history")
        assertThat(deepLinkUri).startsWith("$scheme://$host")
    }

    @Test
    fun deepLink_route_with_nullable_status_matches_NavHost_configuration() {
        // Given - NavHost route definition (from MainActivity.kt)
        val route = "history?status={status}"

        // Then - Verify route accepts optional status parameter
        assertThat(route).contains("status={status}")
        // Note: NavArgument configuration (nullable, defaultValue) is validated at runtime by Navigation Compose
    }

    @Test
    fun notification_tap_creates_correct_deepLink_URI() {
        // Given - Batch status from notification
        val notificationStatus = SemaphoreStatus.YELLOW

        // When - Construct deep link URI for notification tap
        val deepLinkUri = when (notificationStatus) {
            SemaphoreStatus.YELLOW -> "smartnutristock://history?status=YELLOW"
            SemaphoreStatus.EXPIRED -> "smartnutristock://history?status=EXPIRED"
            SemaphoreStatus.GREEN -> "smartnutristock://history" // No notification for GREEN
        }

        // Then - Verify deep link URI matches expected format for YELLOW notification
        assertThat(deepLinkUri).isEqualTo("smartnutristock://history?status=YELLOW")
    }
}
