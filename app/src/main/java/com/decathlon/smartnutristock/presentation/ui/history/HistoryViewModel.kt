package com.decathlon.smartnutristock.presentation.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
import com.decathlon.smartnutristock.domain.usecase.GetAllProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 *
 * Responsibilities:
 * - Observes all products from GetAllProductsUseCase
 * - Exposes reactive state via StateFlow
 * - Handles loading and error states
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<HistoryUiState>(
        value = HistoryUiState.Loading
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Load products on initialization
        loadProducts()
    }

    /**
     * Load all products from UseCase.
     */
    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState.Loading

            try {
                getAllProductsUseCase().collect { products ->
                    _uiState.value = HistoryUiState.Success(products)
                }
            } catch (e: Exception) {
                _uiState.value = HistoryUiState.Error(e.message ?: "Error al cargar productos")
            }
        }
    }

    /**
     * Reload products (user-triggered refresh).
     */
    fun refresh() {
        loadProducts()
    }
}

/**
 * UI State for History screen.
 * Sealed class enables exhaustive `when()` expressions in Compose UI.
 */
sealed class HistoryUiState {
    data object Loading : HistoryUiState()
    data class Success(val products: List<ProductCatalogEntity>) : HistoryUiState()
    data class Error(val message: String) : HistoryUiState()
}
