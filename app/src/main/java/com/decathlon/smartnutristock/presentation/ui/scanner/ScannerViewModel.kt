package com.decathlon.smartnutristock.presentation.ui.scanner

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.data.repository.RegisterResult
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure
import com.decathlon.smartnutristock.data.repository.RegisterResult.Success
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.domain.model.UpsertBatchResult
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import com.decathlon.smartnutristock.domain.usecase.RegisterProductUseCase
import com.decathlon.smartnutristock.domain.usecase.UpsertStockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

/**
 * UI state for scanner screen.
 */
sealed class ScannerUiState {
    object Scanning : ScannerUiState()
    data class ProductFound(val product: ProductCatalogEntity) : ScannerUiState()
    data class WaitingForBatchData(
        val ean: String,
        val productName: String,
        val step: BatchInputStep
    ) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
}

/**
 * Step in batch input flow.
 */
sealed class BatchInputStep {
    object SelectExpiryDate : BatchInputStep()
    data class EnterQuantity(val expiryDate: Instant) : BatchInputStep()
    object Confirming : BatchInputStep()
}

/**
 * ViewModel for barcode scanning and product batch management.
 *
 * States:
 * - Scanning: Camera active, processing frames
 * - ProductFound: Product exists in catalog
 * - WaitingForBatchData: User entering batch details (expiry date, quantity)
 * - Error: Error occurred
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val registerProductUseCase: RegisterProductUseCase,
    private val upsertStockUseCase: UpsertStockUseCase,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : ViewModel() {

    // Camera state
    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    // EAN detection state
    private val _currentEan = MutableStateFlow<String?>(null)
    val currentEan: StateFlow<String?> = _currentEan.asStateFlow()

    // Bottom Sheet state
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Product details (for auto-sum stock when product found)
    private val _foundProduct = MutableStateFlow<ProductCatalogEntity?>(null)
    val foundProduct: StateFlow<ProductCatalogEntity?> = _foundProduct.asStateFlow()

    // Batch input state
    private val _batchInputState = MutableStateFlow<BatchInputStep?>(null)
    val batchInputState: StateFlow<BatchInputStep?> = _batchInputState.asStateFlow()

    // Selected expiry date for batch
    private val _expiryDate = MutableStateFlow<Instant?>(null)
    val expiryDate: StateFlow<Instant?> = _expiryDate.asStateFlow()

    // Entered quantity for batch
    private val _quantity = MutableStateFlow<Int?>(null)
    val quantity: StateFlow<Int?> = _quantity.asStateFlow()

    // Current product info for batch input
    private val _currentProductInfo = MutableStateFlow<ProductCatalogEntity?>(null)
    val currentProductInfo: StateFlow<ProductCatalogEntity?> = _currentProductInfo.asStateFlow()

    /**
     * Start camera and begin scanning.
     */
    fun startCamera() {
        _isCameraActive.value = true
        _errorMessage.value = null
    }

    /**
     * Stop camera and pause scanning.
     */
    fun stopCamera() {
        _isCameraActive.value = false
    }

    /**
     * Handle EAN detection from barcode scanner.
     *
     * @param ean 13-digit EAN code
     */
    fun onEanDetected(ean: String) {
        // Debounce: Only process if not already loading and EAN is new
        if (_isLoading.value || _currentEan.value == ean) {
            return
        }

        // Haptic feedback is now triggered in UI layer via LocalHapticFeedback

        viewModelScope.launch {
            _currentEan.value = ean
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Search product in repository
                val product = productRepository.findByEan(ean)

                if (product != null) {
                    // Product found - start batch input flow
                    _foundProduct.value = product
                    _currentProductInfo.value = product
                    _isBottomSheetVisible.value = false
                    _isLoading.value = false
                    _batchInputState.value = BatchInputStep.SelectExpiryDate
                } else {
                    // Product not found - show Bottom Sheet for registration
                    _foundProduct.value = null
                    _isBottomSheetVisible.value = true
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al buscar producto: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Register new product from Bottom Sheet.
     *
     * @param productName Product name (3-100 chars)
     * @param packSize Pack size in grams (positive)
     */
    fun onRegisterProduct(productName: String, packSize: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val ean = _currentEan.value ?: return@launch

                val result = registerProductUseCase(ean, productName, packSize, 1L)

                when (result) {
                    is Success -> {
                        // Product registered successfully
                        _isBottomSheetVisible.value = false
                        _foundProduct.value = result.product
                        _currentProductInfo.value = result.product
                        _isLoading.value = false
                        // _currentEan.value = null  // Removed to enable chain flow to batch input
                        _batchInputState.value = BatchInputStep.SelectExpiryDate
                        _successMessage.value = "Producto registrado exitosamente"
                    }
                    is Failure -> {
                        _errorMessage.value = when (result) {
                            is com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidEan -> result.message
                            is com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidName -> result.message
                            is com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.InvalidPackSize -> result.message
                            is com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.DuplicateEan -> result.message
                            is com.decathlon.smartnutristock.data.repository.RegisterResult.Failure.DatabaseError -> result.message
                            else -> "Error al registrar producto"
                        }
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Dismiss Bottom Sheet.
     */
    fun dismissBottomSheet() {
        _isBottomSheetVisible.value = false
        _errorMessage.value = null
        _currentEan.value = null
    }

    /**
     * Handle expiry date selection from DatePicker.
     *
     * @param instant Selected expiry date
     */
    fun onExpiryDateSelected(instant: Instant) {
        _expiryDate.value = instant
        _batchInputState.value = BatchInputStep.EnterQuantity(instant)
    }

    /**
     * Handle quantity entry from dialog.
     *
     * @param quantity Entered quantity (must be positive)
     */
    fun onQuantityEntered(quantity: Int) {
        if (quantity <= 0) {
            _errorMessage.value = "La cantidad debe ser mayor a 0"
            return
        }
        _quantity.value = quantity
        _batchInputState.value = BatchInputStep.Confirming
    }

    /**
     * Confirm and create the batch.
     */
    fun onConfirmBatch() {
        val ean = _currentEan.value ?: return
        val expiryDate = _expiryDate.value ?: return
        val quantity = _quantity.value ?: return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Calculate status from expiry date
                val clock = Clock.systemUTC()
                val status = calculateStatusUseCase(expiryDate, clock)

                // Build Batch object
                val batch = Batch(
                    id = UUID.randomUUID().toString(),
                    ean = ean,
                    quantity = quantity,
                    expiryDate = expiryDate,
                    status = status
                )

                // Call UpsertStockUseCase
                when (val result = upsertStockUseCase.upsert(batch)) {
                    is UpsertBatchResult.Success -> {
                        _successMessage.value = "Lote agregado correctamente"
                        resetBatchInputState()
                    }
                    is UpsertBatchResult.Deleted -> {
                        _errorMessage.value = "Lote eliminado (cantidad = 0)"
                        resetBatchInputState()
                    }
                    is UpsertBatchResult.Error -> {
                        _errorMessage.value = "Error al guardar lote: ${result.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error inesperado: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cancel batch input and reset to scanning state.
     */
    fun onCancelBatchInput() {
        resetBatchInputState()
    }

    /**
     * Reset batch input state and return to scanning.
     */
    private fun resetBatchInputState() {
        _batchInputState.value = null
        _expiryDate.value = null
        _quantity.value = null
        _currentProductInfo.value = null
        _currentEan.value = null
        _foundProduct.value = null
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message.
     */
    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Reset to idle state.
     */
    fun reset() {
        _currentEan.value = null
        _foundProduct.value = null
        _isBottomSheetVisible.value = false
        _errorMessage.value = null
        _successMessage.value = null
        _isLoading.value = false
        resetBatchInputState()
    }
}
