package com.decathlon.smartnutristock.presentation.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.decathlon.smartnutristock.domain.usecase.SemaphoreCounters

/**
 * Dashboard Screen with Semaphore Counters.
 *
 * Features:
 * - Displays 3 semaphore counters (green, yellow, red)
 * - Shows total products count
 * - Shows loading spinner while fetching
 * - Shows error message if fetch fails
 * - Uses theme colors from CalculateStatusUseCase:
 *   🔴 Red: #FF4444 (expired, days ≤ 0)
 *   🟡 Yellow: #FFC107 (warning, 1-7 days)
 *   🟢 Green: #4CAF50 (safe, 8+ days)
 *
 * Performance:
 * - LazyColumn for performance
 * - CircularProgressIndicator for loading state
 * - Auto-refresh on app resume
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        // Auto-refresh when screen comes into view
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            // Simple top bar with app name
            TopAppBar(
                title = {
                    Text(
                        text = "Smart Nutri-Stock",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is DashboardUiState.Loading -> {
                    // Show loading spinner
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is DashboardUiState.Success -> {
                    // Show counters
                    DashboardContent(counters = (uiState as DashboardUiState.Success).counters)
                }

                is DashboardUiState.Error -> {
                    // Show error message
                    ErrorScreen(message = (uiState as DashboardUiState.Error).message)
                }
            }
        }
    }
}

/**
 * Dashboard content with semaphore counters.
 * Optimized for thumb zone on Samsung XCover7.
 */
@Composable
private fun DashboardContent(counters: SemaphoreCounters) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Semáforo de Inventario",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Semaphore Counters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Green Counter
            SemaphoreCounter(
                label = "Productos Seguros",
                count = counters.green,
                color = Color(0xFF4CAF50), // Green from CalculateStatusUseCase
                emoji = "🟢"
            )

            // Yellow Counter
            SemaphoreCounter(
                label = "Por Vencer",
                count = counters.yellow,
                color = Color(0xFFFFC107), // Yellow from CalculateStatusUseCase
                emoji = "🟡"
            )

            // Red Counter
            SemaphoreCounter(
                label = "Expirados",
                count = counters.red,
                color = Color(0xFFFF4444), // Red from CalculateStatusUseCase
                emoji = "🔴"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Total Products Count
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total de Productos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${counters.total}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Individual semaphore counter component.
 *
 * Features:
 * - Circular background with theme color
 * - Count number in large font
 * - Optimized for thumb zone (56dp minimum)
 */
@Composable
private fun SemaphoreCounter(
    label: String,
    count: Int,
    color: Color,
    emoji: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .height(120.dp) // Thumb zone optimized
    ) {
        // Background circle
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color = color, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Count number
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Emoji label
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium
        )

        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 4.dp)
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
