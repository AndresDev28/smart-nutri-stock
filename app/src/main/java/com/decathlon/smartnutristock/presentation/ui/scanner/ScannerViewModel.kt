package com.decathlon.smartnutristock.presentation.ui.scanner

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.data.repository.ProductRepository
import com.decathlon.smartnutristock.data.repository.RegisterResult
import com.decathlon.smartnutristock.data.repository.RegisterResult.Failure
import com.decathlon.smartnutristock.data.repository.RegisterResult.Success
import com.decathlon.smartnutristock.domain.usecase.RegisterProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for barcode scanning and product registration.
 *
 * States:
 * - Idle: Camera not started
 * - Scanning: Camera active, processing frames
 * - Processing: EAN detected, searching repository
 * - ProductFound: Product exists in catalog
 * - ProductNotFound: Product not found, show Bottom Sheet
 * - Registering: User registering new product
 * - Error: Error occurred
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val application: Application,  // Add Application context
    private val productRepository: ProductRepository,
    private val registerProductUseCase: RegisterProductUseCase
) : ViewModel() {

    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

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

        vibrate()  // Haptic feedback

        viewModelScope.launch {
            _currentEan.value = ean
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Search product in repository
                val product = productRepository.findByEan(ean)

                if (product != null) {
                    // Product found - auto-sum stock
                    _foundProduct.value = product
                    _isBottomSheetVisible.value = false
                    _isLoading.value = false
                    _successMessage.value = "Producto encontrado: ${product.name}"

                    // TODO: Integrate UpsertStockUseCase here
                    // For MVP, we'll just show the product was found
                } else {
                    // Product not found - show Bottom Sheet
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
                        _isLoading.value = false
                        _currentEan.value = null
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
    }

    /**
     * Trigger haptic feedback when barcode is detected.
     * Short vibration (50ms) for tactile feedback.
     */
    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ (Android 12): Use VibratorManager
            val vibratorManager = vibrator as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            // API < 31: Use deprecated Vibrator
            @Suppress("DEPRECATION")
            (vibrator as Vibrator).vibrate(50)
        }
    }
}
