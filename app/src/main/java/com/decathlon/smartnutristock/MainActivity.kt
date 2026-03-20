package com.decathlon.smartnutristock

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


/**
 * Entry point of the application.
 * Sets up navigation with Jetpack Navigation Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
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
                        composable(route = "history") {
                            HistoryScreen()
                        }
                    }
                }
            }
        }
    }
}
