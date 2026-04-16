package com.decathlon.smartnutristock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.decathlon.smartnutristock.presentation.ui.theme.SmartNutriStockTheme
import com.decathlon.smartnutristock.presentation.ui.scanner.ScannerScreen
import com.decathlon.smartnutristock.presentation.ui.dashboard.DashboardScreen
import com.decathlon.smartnutristock.presentation.ui.history.HistoryScreen
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink


/**
 * Entry point of the application.
 * Sets up navigation with Jetpack Navigation Compose.
 * Handles deep links for notification taps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Reference to navController for deep link handling
    private var navControllerRef: androidx.navigation.NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController().also {
                navControllerRef = it
            }
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
                        startDestination = "home",
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                            .imePadding()
                    ) {
                        composable(route = "home") {
                            DashboardScreen(navController = navController)
                        }
                        composable(route = "scanner") {
                            ScannerScreen()
                        }
                        composable(route = "dashboard") {
                            DashboardScreen(navController = navController)
                        }
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
                            HistoryScreen()
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
