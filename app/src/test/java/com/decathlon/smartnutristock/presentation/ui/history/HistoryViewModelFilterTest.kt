package com.decathlon.smartnutristock.presentation.ui.history

import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.SemaphoreStatus
import com.decathlon.smartnutristock.domain.model.WorkflowAction
import com.decathlon.smartnutristock.domain.usecase.GetAllBatchesUseCase
import com.decathlon.smartnutristock.domain.usecase.RestoreBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.SoftDeleteBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchActionUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateBatchUseCase
import com.decathlon.smartnutristock.domain.usecase.UpdateProductNameUseCase
import io.mockk.coEvery
import io.mockk.mockk
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
import com.google.common.truth.Truth.assertThat
import java.time.Instant

/**
 * Unit tests for HistoryViewModel filter logic.
 *
 * Verifies that batches are correctly filtered by action state.
 */
@ExperimentalCoroutinesApi
class HistoryViewModelFilterTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: HistoryViewModel
    private lateinit var mockGetAllBatchesUseCase: GetAllBatchesUseCase
    private lateinit var mockSoftDeleteBatchUseCase: SoftDeleteBatchUseCase
    private lateinit var mockRestoreBatchUseCase: RestoreBatchUseCase
    private lateinit var mockUpdateBatchUseCase: UpdateBatchUseCase
    private lateinit var mockUpdateProductNameUseCase: UpdateProductNameUseCase
    private lateinit var mockUpdateBatchActionUseCase: UpdateBatchActionUseCase

    private val testBatches = listOf(
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
            actionTaken = WorkflowAction.DISCOUNTED
        ),
        Batch(
            id = "batch-3",
            ean = "8435408475368",
            quantity = 30,
            expiryDate = Instant.now(),
            status = SemaphoreStatus.YELLOW,
            name = "Protein C",
            packSize = 500,
            actionTaken = WorkflowAction.REMOVED
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

        coEvery { mockGetAllBatchesUseCase() } returns flowOf(testBatches)
        coEvery { mockSoftDeleteBatchUseCase(any(), any()) } returns 1
        coEvery { mockRestoreBatchUseCase(any()) } returns 1
        coEvery { mockUpdateBatchUseCase(any()) } returns 1
        coEvery { mockUpdateProductNameUseCase(any(), any()) } returns 1
        coEvery { mockUpdateBatchActionUseCase(any(), any()) } returns Unit

        viewModel = HistoryViewModel(
            mockGetAllBatchesUseCase,
            mockSoftDeleteBatchUseCase,
            mockRestoreBatchUseCase,
            mockUpdateBatchUseCase,
            mockUpdateProductNameUseCase,
            mockUpdateBatchActionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state with ALL filter should show all batches`() = runTest {
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

        val batches = (uiState as HistoryUiState.Success).batches
        assertThat(batches).hasSize(3)
    }

    @Test
    fun `setActionFilter to PENDING should show only PENDING batches`() = runTest {
        viewModel.setActionFilter(ActionFilter.PENDING)

        // Wait for coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

        val batches = (uiState as HistoryUiState.Success).batches
        assertThat(batches).hasSize(1)
        assertThat(batches[0].id).isEqualTo("batch-1")
        assertThat(batches[0].actionTaken).isEqualTo(WorkflowAction.PENDING)
    }

    @Test
    fun `setActionFilter to WITH_ACTION should show only DISCOUNTED and REMOVED batches`() = runTest {
        viewModel.setActionFilter(ActionFilter.WITH_ACTION)

        // Wait for coroutines to complete
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(HistoryUiState.Success::class.java)

        val batches = (uiState as HistoryUiState.Success).batches
        assertThat(batches).hasSize(2)
        assertThat(batches.map { it.actionTaken }).containsExactly(WorkflowAction.DISCOUNTED, WorkflowAction.REMOVED)
    }

    @Test
    fun `setActionFilter to ALL should show all batches after changing filter`() = runTest {
        // First filter to PENDING
        viewModel.setActionFilter(ActionFilter.PENDING)
        testDispatcher.scheduler.advanceUntilIdle()
        var batches = (viewModel.uiState.value as HistoryUiState.Success).batches
        assertThat(batches).hasSize(1)

        // Then filter to ALL
        viewModel.setActionFilter(ActionFilter.ALL)
        testDispatcher.scheduler.advanceUntilIdle()
        batches = (viewModel.uiState.value as HistoryUiState.Success).batches
        assertThat(batches).hasSize(3)
    }
}
