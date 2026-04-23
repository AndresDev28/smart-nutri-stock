package com.decathlon.smartnutristock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.decathlon.smartnutristock.presentation.ui.theme.SmartNutriStockTheme
import com.decathlon.smartnutristock.presentation.ui.scanner.ScannerScreen
import com.decathlon.smartnutristock.presentation.ui.dashboard.DashboardScreen
import com.decathlon.smartnutristock.presentation.ui.history.HistoryScreen
import com.decathlon.smartnutristock.presentation.auth.LoginScreen
import com.decathlon.smartnutristock.presentation.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.hilt.navigation.compose.hiltViewModel
import com.decathlon.smartnutristock.data.worker.SyncScheduler
import com.decathlon.smartnutristock.domain.model.AuthState


/**
 * Entry point of the application.
 * Sets up navigation with Jetpack Navigation Compose.
 * Handles deep links for notification taps.
 * Implements Auth Guard to protect routes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Reference to navController for deep link handling
    private var navControllerRef: androidx.navigation.NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SyncScheduler.scheduleSync(this)

        setContent {
            val navController = rememberNavController().also {
                navControllerRef = it
            }

            // Inject MainViewModel to observe auth state
            val mainViewModel: MainViewModel = hiltViewModel()

            // Collect auth state
            val authState by mainViewModel.authState.collectAsState()

            SmartNutriStockTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Smart Nutri-Stock",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "login", // Auth Guard: start at login
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        // Login route (no auth guard required)
                        composable(route = "login") {
                            // If user is already authenticated, navigate to home
                            LaunchedEffect(authState) {
                                if (authState is AuthState.Authenticated) {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            }

                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Home route (auth guard: require authentication)
                        composable(route = "home") {
                            // Auth Guard: Check auth state
                            when (authState) {
                                is AuthState.Authenticated -> {
                                    DashboardScreen(navController = navController)
                                }
                                is AuthState.NotAuthenticated,
                                is AuthState.Error -> {
                                    // Redirect to login if not authenticated
                                    LaunchedEffect(Unit) {
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                }
                                AuthState.Loading -> {
                                    // Show loading while checking auth state
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        // Scanner route (auth guard: require authentication)
                        composable(route = "scanner") {
                            // Auth Guard: Check auth state
                            when (authState) {
                                is AuthState.Authenticated -> {
                                    ScannerScreen()
                                }
                                is AuthState.NotAuthenticated,
                                is AuthState.Error -> {
                                    // Redirect to login if not authenticated
                                    LaunchedEffect(Unit) {
                                        navController.navigate("login") {
                                            popUpTo("scanner") { inclusive = true }
                                        }
                                    }
                                }
                                AuthState.Loading -> {
                                    // Show loading while checking auth state
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        // Dashboard route (auth guard: require authentication)
                        composable(route = "dashboard") {
                            // Auth Guard: Check auth state
                            when (authState) {
                                is AuthState.Authenticated -> {
                                    DashboardScreen(navController = navController)
                                }
                                is AuthState.NotAuthenticated,
                                is AuthState.Error -> {
                                    // Redirect to login if not authenticated
                                    LaunchedEffect(Unit) {
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                    }
                                }
                                AuthState.Loading -> {
                                    // Show loading while checking auth state
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        // History route (auth guard: require authentication)
                        composable(
                            route = "history?status={status}",
                            arguments = listOf(
                                navArgument("status") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            ),
                            deepLinks = listOf(
                                navDeepLink {
                                    uriPattern = "smartnutristock://history?status={status}"
                                }
                            )
                        ) {
                            // Auth Guard: Check auth state
                            when (authState) {
                                is AuthState.Authenticated -> {
                                    HistoryScreen()
                                }
                                is AuthState.NotAuthenticated,
                                is AuthState.Error -> {
                                    // Redirect to login if not authenticated
                                    LaunchedEffect(Unit) {
                                        navController.navigate("login") {
                                            popUpTo("history") { inclusive = true }
                                        }
                                    }
                                }
                                AuthState.Loading -> {
                                    // Show loading while checking auth state
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle deep link intents when the app is already open in background.
     * This is called when the user taps a notification while the app is running.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            navControllerRef?.handleDeepLink(it)
        }
    }
}
