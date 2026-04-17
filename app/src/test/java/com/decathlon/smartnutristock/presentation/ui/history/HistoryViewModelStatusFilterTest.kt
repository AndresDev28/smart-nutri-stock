package com.decathlon.smartnutristock.presentation.ui.history

import androidx.lifecycle.SavedStateHandle
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.WorkflowAction
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
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HistoryViewModel status filter initialization from SavedStateHandle.
 *
 * Tests verify that the status filter is correctly initialized from the "status" nav argument
 * (deep link parameter) and that batches are filtered appropriately.
 */
@ExperimentalCoroutinesApi
class HistoryViewModelStatusFilterTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HistoryViewModel
    private lateinit var mockGetAllBatchesUseCase: GetAllBatchesUseCase
    private lateinit var mockSoftDeleteBatchUseCase: SoftDeleteBatchUseCase
    private lateinit var mockRestoreBatchUseCase: RestoreBatchUseCase
    private lateinit var mockUpdateBatchUseCase: UpdateBatchUseCase
    private lateinit var mockUpdateProductNameUseCase: UpdateProductNameUseCase
    private lateinit var mockUpdateBatchActionUseCase: UpdateBatchActionUseCase
    private lateinit var mockExportInventoryUseCase: ExportInventoryUseCase

    private val testBatches =
        listOf(
            Batch(
                id = "batch-1",
                ean = "8435408475366",
                quantity = 10,
                expiryDate = Instant.now(),
                status = SemaphoreStatus.YELLOW,
                name = "Protein A",
                packSize = 500,
                actionTaken = WorkflowAction.PENDING
            ),
            Batch(
                id = "batch-2",
                ean = "8435408475367",
                quantity = 20,
                expiryDate = Instant.now(),
                status = SemaphoreStatus.EXPIRED,
                name = "Protein B",
                packSize = 500,
                actionTaken = WorkflowAction.PENDING
            ),
            Batch(
                id = "batch-3",
                ean = "8435408475368",
                quantity = 30,
                expiryDate = Instant.now(),
                status = SemaphoreStatus.GREEN,
                name = "Protein C",
                packSize = 500,
                actionTaken = WorkflowAction.PENDING
            )
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockGetAllBatchesUseCase = mockk()
        mockSoftDeleteBatchUseCase = mockk()
        mockRestoreBatchUseCase = mockk()
        mockUpdateBatchUseCase = mockk()
        mockUpdateProductNameUseCase = mockk()
        mockUpdateBatchActionUseCase = mockk()
        mockExportInventoryUseCase = mockk(relaxed = true)

        coEvery { mockGetAllBatchesUseCase() } returns flowOf(testBatches)
        coEvery { mockSoftDeleteBatchUseCase(any(), any()) } returns 1
        coEvery { mockRestoreBatchUseCase(any()) } returns 1
        coEvery { mockUpdateBatchUseCase(any()) } returns 1
        coEvery { mockUpdateProductNameUseCase(any(), any()) } returns 1
        coEvery { mockUpdateBatchActionUseCase(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state with YELLOW status nav arg should filter to YELLOW batches only`() = runTest {
        // Given - SavedStateHandle with "YELLOW" status
        val savedStateHandle = SavedStateHandle(mapOf("status" to "YELLOW"))

        // When - ViewModel is created with SavedStateHandle
        viewModel =
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

        // Wait for coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - status filter should be YELLOW
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.YELLOW)

        // And - only YELLOW batches should be shown
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

        val batches = (uiState as HistoryUiState.Success).batches
        assertThat(batches).hasSize(1)
        assertThat(batches[0].id).isEqualTo("batch-1")
        assertThat(batches[0].status).isEqualTo(SemaphoreStatus.YELLOW)
    }

    @Test
    fun `initial state with EXPIRED status nav arg should filter to EXPIRED batches only`() =
        runTest {
            // Given - SavedStateHandle with "EXPIRED" status
            val savedStateHandle = SavedStateHandle(mapOf("status" to "EXPIRED"))

            // When - ViewModel is created with SavedStateHandle
            viewModel =
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

            // Wait for coroutines to complete
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - status filter should be EXPIRED
            assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.EXPIRED)

            // And - only EXPIRED batches should be shown
            val uiState = viewModel.uiState.value
            assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

            val batches = (uiState as HistoryUiState.Success).batches
            assertThat(batches).hasSize(1)
            assertThat(batches[0].id).isEqualTo("batch-2")
            assertThat(batches[0].status).isEqualTo(SemaphoreStatus.EXPIRED)
        }

    @Test
    fun `initial state with null status nav arg should default to ALL filter`() = runTest {
        // Given - SavedStateHandle with null status
        val savedStateHandle = SavedStateHandle(mapOf("status" to null))

        // When - ViewModel is created with SavedStateHandle
        viewModel =
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

        // Wait for coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - status filter should be ALL
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.ALL)

        // And - all batches should be shown
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

        val batches = (uiState as HistoryUiState.Success).batches
        assertThat(batches).hasSize(3)
    }

    @Test
    fun `initial state with missing status nav arg should default to ALL filter`() = runTest {
        // Given - SavedStateHandle without status parameter
        val savedStateHandle = SavedStateHandle()

        // When - ViewModel is created with SavedStateHandle
        viewModel =
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

        // Wait for coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - status filter should be ALL
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.ALL)

        // And - all batches should be shown
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

        val batches = (uiState as HistoryUiState.Success).batches
        assertThat(batches).hasSize(3)
    }

    @Test
    fun `initial state with invalid status nav arg should default to ALL filter (graceful degradation)`() =
        runTest {
            // Given - SavedStateHandle with invalid status
            val savedStateHandle = SavedStateHandle(mapOf("status" to "FOOBAR"))

            // When - ViewModel is created with SavedStateHandle
            viewModel =
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

            // Wait for coroutines to complete
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - status filter should be ALL (graceful degradation)
            assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.ALL)

            // And - all batches should be shown
            val uiState = viewModel.uiState.value
            assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

            val batches = (uiState as HistoryUiState.Success).batches
            assertThat(batches).hasSize(3)
        }

    @Test
    fun `setStatusFilter should change filter and reload batches`() = runTest {
        // Given - SavedStateHandle with null status (default ALL)
        val savedStateHandle = SavedStateHandle()
        viewModel =
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

        testDispatcher.scheduler.advanceUntilIdle()
        var batches = (viewModel.uiState.value as HistoryUiState.Success).batches
        assertThat(batches).hasSize(3) // All batches

        // When - Set status filter to YELLOW
        viewModel.setStatusFilter(SemaphoreStatusFilter.YELLOW)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - only YELLOW batches should be shown
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.YELLOW)
        batches = (viewModel.uiState.value as HistoryUiState.Success).batches
        assertThat(batches).hasSize(1)
        assertThat(batches[0].status).isEqualTo(SemaphoreStatus.YELLOW)

        // When - Set status filter to EXPIRED
        viewModel.setStatusFilter(SemaphoreStatusFilter.EXPIRED)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - only EXPIRED batches should be shown
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.EXPIRED)
        batches = (viewModel.uiState.value as HistoryUiState.Success).batches
        assertThat(batches).hasSize(1)
        assertThat(batches[0].status).isEqualTo(SemaphoreStatus.EXPIRED)

        // When - Set status filter back to ALL
        viewModel.setStatusFilter(SemaphoreStatusFilter.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - all batches should be shown again
        assertThat(viewModel.statusFilter.value).isEqualTo(SemaphoreStatusFilter.ALL)
        batches = (viewModel.uiState.value as HistoryUiState.Success).batches
        assertThat(batches).hasSize(3)
    }
}
