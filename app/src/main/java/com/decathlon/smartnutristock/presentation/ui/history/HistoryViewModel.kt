package com.decathlon.smartnutristock.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase
import com.decathlon.smartnutristock.domain.usecase.GetAllProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 *
 * Responsibilities:
 * - Loads all products from GetAllProductsUseCase ONCE
 * - Calculates status for each product using CalculateStatusUseCase
 * - Exposes reactive state via StateFlow
 * - Handles loading and error states
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val calculateStatusUseCase: CalculateStatusUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<HistoryUiState>(
        value = HistoryUiState.Loading
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Load products ONCE on initialization
        loadProductsOnce()
    }

    /**
     * Load all products ONCE (not a flow collector).
     * This prevents the infinite recomposition loop.
     */
    private fun loadProductsOnce() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            try {
                // Use first() from flow to get products once, not collect
                val products = getAllProductsUseCase().first()
                
                // Calculate status for each product
                val productsWithStatus = products.map { product ->
                    ProductWithStatus(
                        product = product,
                        status = calculateStatusUseCase(product.daysUntilExpiry)
                    )
                }
                
                _uiState.value = HistoryUiState.Success(productsWithStatus)
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.message ?: "Error al cargar productos")
            }
        }
    }

    /**
     * Reload products (user-triggered refresh).
     */
    fun refresh() {
        loadProductsOnce()
    }
}

/**
 * Product with calculated status.
 * Prevents recalculating status on every recomposition.
 */
data class ProductWithStatus(
    val product: ProductCatalogEntity,
    val status: com.decathlon.smartnutristock.domain.usecase.SemaphoreStatus
)

/**
 * UI State for History screen.
 * Sealed class enables exhaustive `when()` expressions in Compose UI.
 */
sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Success(val products: List<ProductWithStatus>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
