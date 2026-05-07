package com.decathlon.smartnutristock.presentation.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.decathlon.smartnutristock.data.worker.SyncScheduler
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import com.decathlon.smartnutristock.domain.model.Batch
import com.decathlon.smartnutristock.presentation.permission.NotificationPermissionHandler
import com.decathlon.smartnutristock.presentation.permission.NotificationPermissionHandler.RationaleDialogState
import com.decathlon.smartnutristock.presentation.permission.NotificationPermissionHandler.RationaleDialogState.NotShowing
import com.decathlon.smartnutristock.presentation.permission.NotificationPermissionHandler.RationaleDialogState.Showing
import com.decathlon.smartnutristock.presentation.permission.NotificationRationaleDialog
import com.decathlon.smartnutristock.presentation.ui.components.NutriCard
import com.decathlon.smartnutristock.presentation.ui.components.PremiumButton
import com.decathlon.smartnutristock.presentation.ui.components.EmptyState
import com.decathlon.smartnutristock.presentation.ui.components.ShimmerCard
import com.decathlon.smartnutristock.presentation.ui.theme.statusTeal
import com.decathlon.smartnutristock.presentation.ui.theme.statusAmber
import com.decathlon.smartnutristock.presentation.ui.theme.statusDeepRed
import com.decathlon.smartnutristock.R

/**
 * Dashboard Screen with Premium UI Design.
 *
 * Features:
 * - Displays 3 summary cards (GREEN/AMBER/RED counts) with premium styling
 * - Shows product list with NutriCard components
 * - Shows empty state with motivational message when no products
 * - Shows shimmer loading while fetching data
 * - Quick Scan button with PremiumButton in bottom thumb zone
 * - Ver Historial button for navigation
 * - Total products count
 * - Uses theme colors via MaterialTheme.colorScheme.statusTeal/Amber/DeepRed
 *
 * SSOT: All status logic comes from CalculateStatusUseCase (domain layer).
 * UI renders state, never calculates status.
 *
 * Performance:
 * - LazyColumn with keys (batchId) for smooth scrolling
 * - ShimmerLoading placeholders during sync
 * - Auto-refresh on app resume
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Check camera permission
    val cameraPermissionGranted = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, navigate to scanner
            navController.navigate("scanner")
        }
    }

    // Check notification permission (Android 13+)
    val notificationPermissionRequired = remember {
        NotificationPermissionHandler.isPermissionRequired()
    }

    val notificationPermissionGranted = remember {
        NotificationPermissionHandler.checkPermission(context)
    }

    // Notification permission launcher (only used on Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted - worker will run automatically
            // Note: Worker is already initialized in SmartNutriStockApp.onCreate()
            // No action needed here
        }
        // If denied, user can request again or be directed to settings
    }

    // Rationale dialog state
    var rationaleDialogState by remember { mutableStateOf<RationaleDialogState>(NotShowing) }

    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect UI state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val logoutEvent by viewModel.logoutEvent.collectAsState()

    // Handle logout event - navigate to login and clear backstack
    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            navController.navigate("login") {
                popUpTo("login") { inclusive = true }
            }
            viewModel.clearLogoutEvent()
        }
    }



    LaunchedEffect(Unit) {
        // Auto-refresh when screen comes into view
        viewModel.refresh()

        // Handle notification permission request (Android 13+ only)
        if (notificationPermissionRequired && !notificationPermissionGranted) {
            // First time: show rationale dialog before requesting
            rationaleDialogState = Showing(
                onConfirm = {
                    rationaleDialogState = NotShowing
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                },
                onDismiss = {
                    rationaleDialogState = NotShowing
                }
            )
        }
    }

    // Show snackbar if permission was permanently denied
    // This runs after permission check, when user has denied with "Don't ask again"
    LaunchedEffect(notificationPermissionGranted) {
        if (notificationPermissionRequired &&
            !notificationPermissionGranted &&
            rationaleDialogState == NotShowing) {
            // Check if this is a permanent denial (user checked "Don't ask again")
            val activity = context as? android.app.Activity
            if (activity != null && NotificationPermissionHandler.isPermanentlyDenied(activity)) {
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.notification_permission_denied),
                    actionLabel = context.getString(R.string.notification_permission_settings),
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // Open app settings
                    val settingsIntent = NotificationPermissionHandler.createSettingsIntent(context)
                    context.startActivity(settingsIntent)
                }
            }
        }
    }

    // Show rationale dialog if needed
    if (rationaleDialogState is Showing) {
        val showingState = rationaleDialogState as Showing
        NotificationRationaleDialog(
            onConfirm = showingState.onConfirm,
            onDismiss = showingState.onDismiss
        )
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                    // Show counters and product list
                    DashboardContent(
                        counters = (uiState as DashboardUiState.Success).counters,
                        batches = (uiState as DashboardUiState.Success).batches,
                        navController = navController,
                        cameraPermissionGranted = cameraPermissionGranted,
                        cameraPermissionLauncher = cameraPermissionLauncher,
                        context = context,
                        userEmail = userEmail,
                        onLogout = { viewModel.logout() }
                    )
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
 * Dashboard header with two-section layout.
 *
 * Structure:
 * - Top Row: Logo + "Smart Nutri-Stock" + Logout IconButton
 * - Greeting Column: 3 lines with Material3 typography
 *
 * @param userEmail Nullable email for greeting. Extracts username with `?.substringBefore("@")`
 * @param onLogout Callback invoked directly from logout IconButton
 */
@Composable
private fun DashboardHeader(
    userEmail: String?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Top Row: Logo, Title, and Logout Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Smart Nutri-Stock Logo",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Smart Nutri-Stock",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Cerrar Sesión",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Greeting Column
        Column {
            Text(
                text = if (userEmail != null) "Hola, ${userEmail.substringBefore("@")}!" else "Hola!",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Semáforo de Inventario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Resumen actual de tu inventario",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dashboard content with premium UI design.
 * Optimized for thumb zone on Samsung XCover7.
 */
@Composable
private fun DashboardContent(
    counters: SemaphoreCounters,
    batches: List<Batch>,
    navController: NavController,
    cameraPermissionGranted: Boolean,
    cameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context,
    userEmail: String?,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Dashboard Header with two-section layout
        DashboardHeader(
            userEmail = userEmail,
            onLogout = onLogout
        )

        Spacer(modifier = Modifier.height(24.dp))

        // T3.3: Summary Cards Row (Premium counters with elevation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Max)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Green Counter
            SummaryCard(
                count = counters.green,
                label = "Óptimo",
                statusColor = statusTeal(),
                subtitle = "Productos en buen estado",
                icon = Icons.Default.Check,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            // Yellow Counter
            SummaryCard(
                count = counters.yellow,
                label = "Caducar",
                statusColor = statusAmber(),
                subtitle = "Caducan pronto",
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )

            // Red Counter
            SummaryCard(
                count = counters.expired,
                label = "Caducados",
                statusColor = statusDeepRed(),
                subtitle = "Requieren atención",
                icon = Icons.Default.Error,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // T3.5: Product List with NutriCards
        if (batches.isEmpty()) {
            // T3.6: Empty State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    message = "Listo para la recepción de hoy",
                    subtitle = "Escanea tu primer producto para comenzar"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // LazyColumn with NutriCards
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = batches,
                    key = { it.id }  // Critical: use batchId for performance
                ) { batch ->
                    NutriCard(
                        productName = batch.name ?: "Producto desconocido",
                        ean = batch.ean,
                        quantity = batch.quantity,
                        expiryDate = formatExpiryDate(batch.expiryDate),
                        status = batch.status
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Products Count
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total de Productos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${counters.total}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // T3.4: Quick Scan Epicenter Button (PremiumButton in bottom zone)
        PremiumButton(
            text = "Escanear Producto",
            icon = Icons.Default.QrCodeScanner,
            onClick = {
                if (cameraPermissionGranted) {
                    navController.navigate("scanner")
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "Ver Historial" button (OutlinedButton - secondary action)
        OutlinedButton(
            onClick = { navController.navigate("history") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ver Historial",
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * SummaryCard - Premium counter card with icon, count, title, and optional subtitle.
 *
 * Features:
 * - 40dp circular icon container with 10% alpha background (when icon provided)
 * - Count in Bold 28sp using onSurface color
 * - Title in bodyMedium using status color
 * - Optional subtitle in bodySmall with onSurfaceVariant color
 * - Uses theme colors (no hardcoded colors)
 * - 16dp corner radius for premium feel
 * - Fills parent height for uniform card sizing
 *
 * Backward Compatibility:
 * - Works correctly when called without icon and subtitle parameters
 */
@Composable
private fun SummaryCard(
    count: Int,
    label: String,
    statusColor: androidx.compose.ui.graphics.Color,
    subtitle: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon block (top) - only when icon is provided
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Middle block: Count + Title
            Text(
                text = count.toString(),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = statusColor
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor
            )

            Spacer(modifier = Modifier.weight(1f))

            // Subtitle block (bottom) - only when subtitle is provided
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Format expiry date from Instant to display string.
 */
private fun formatExpiryDate(expiryDate: java.time.Instant): String {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return expiryDate.atZone(java.time.ZoneId.systemDefault()).format(formatter)
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
