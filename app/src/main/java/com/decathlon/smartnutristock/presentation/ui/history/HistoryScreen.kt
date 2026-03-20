package com.decathlon.smartnutristock.presentation.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.decathlon.smartnutristock.domain.usecase.CalculateStatusUseCase

/**
 * History screen for viewing all products in the catalog.
 *
 * Features:
 * - Displays all products from ProductCatalogEntity
 * - Shows product name, pack size, expiry status
 * - Loading state with spinner
 * - Error state with message
 * - Empty state when no products
 * - Pull-to-refresh support (manual refresh)
 *
 * Performance:
 * - LazyColumn for performance
 * - Optimized for thumb zone (56dp minimum item height)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Historial de Productos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is HistoryUiState.Loading -> {
                    // Show loading spinner
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is HistoryUiState.Success -> {
                    val products = (uiState as HistoryUiState.Success).products

                    if (products.isEmpty()) {
                        // Show empty state
                        EmptyState()
                    } else {
                        // Show product list
                        ProductList(
                            products = products,
                            onRefresh = { viewModel.refresh() }
                        )
                    }
                }

                is HistoryUiState.Error -> {
                    // Show error message
                    ErrorScreen(message = (uiState as HistoryUiState.Error).message)
                }
            }
        }
    }
}

/**
 * Product list with LazyColumn.
 * Optimized for thumb zone on Samsung XCover7.
 */
@Composable
private fun ProductList(
    products: List<com.decathlon.smartnutristock.data.entity.ProductCatalogEntity>,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) {
        // Load products when screen comes into view
        onRefresh()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = products,
            key = { it.ean }
        ) { product ->
            ProductCard(product = product)
        }

        // Add bottom padding for scrolling
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Individual product card component.
 * Thumb zone optimized (minimum 56dp height).
 */
@Composable
private fun ProductCard(
    product: com.decathlon.smartnutristock.data.entity.ProductCatalogEntity
) {
    val status = CalculateStatusUseCase().invoke(product.daysUntilExpiry)

    val (statusColor, statusText) = when (status) {
        is com.decathlon.smartnutristock.domain.usecase.SemaphoreStatus.Safe -> {
            Pair(Color(0xFF4CAF50), "Seguro (${status.daysUntil} días)")
        }
        is com.decathlon.smartnutristock.domain.usecase.SemaphoreStatus.Warning -> {
            Pair(Color(0xFFFFC107), "Por vencer (${status.daysUntil} días)")
        }
        is com.decathlon.smartnutristock.domain.usecase.SemaphoreStatus.Expired -> {
            val text = if (status.daysUntil < 0) "Expirado (${status.daysUntil} días)" else "Expira hoy"
            Pair(Color(0xFFFF4444), text)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "EAN: ${product.ean}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Pack: ${product.packSize}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Status indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Empty state component.
 */
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📦",
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No hay productos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Escanea un producto para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/**
 * Error screen component.
 */
@Composable
private fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌ Error",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
