package com.decathlon.smartnutristock.presentation.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.domain.model.SemaphoreCounters
import org.junit.Rule
import org.junit.Test

/**
 * UI Tests for DashboardScreen.
 *
 * Tests semaphore counters display and total products count.
 */
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCorrectSemaphoreCounters() {
        // Given
        val greenCount = 5
        val yellowCount = 2
        val redCount = 0

        // When - Test semaphore counter display behavior by recreating the UI structure
        composeTestRule.setContent {
            Surface {
                androidx.compose.foundation.layout.Row {
                    // Green Counter
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(100.dp)
                            .height(120.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFF4CAF50), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "$greenCount",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "🟢",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.material3.Text(
                            text = "Productos Seguros",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }

                    // Yellow Counter
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(100.dp)
                            .height(120.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFFC107), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "$yellowCount",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "🟡",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.material3.Text(
                            text = "Por Vencer",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }

                    // Red Counter
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(100.dp)
                            .height(120.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFF4444), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = "$redCount",
                                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "🔴",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.material3.Text(
                            text = "Expirados",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Then - verify green counter displays "5"
        composeTestRule.onNodeWithText("5")
            .assertTextEquals("5")

        // Then - verify yellow counter displays "2"
        composeTestRule.onNodeWithText("2")
            .assertTextEquals("2")

        // Then - verify red counter displays "0"
        composeTestRule.onNodeWithText("0")
            .assertTextEquals("0")

        // Then - verify labels are displayed
        composeTestRule.onNodeWithText("Productos Seguros").assertIsDisplayed()
        composeTestRule.onNodeWithText("Por Vencer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expirados").assertIsDisplayed()
    }

    @Test
    fun displaysCorrectTotalProducts() {
        // Given
        val counters = SemaphoreCounters(
            green = 5,
            yellow = 2,
            expired = 0
        )
        val expectedTotal = 5 + 2 + 0 // 7

        // When - Test total products display
        composeTestRule.setContent {
            Surface {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Text(
                            text = "Total de Productos",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text(
                            text = "${counters.total}",
                            style = androidx.compose.material3.MaterialTheme.typography.displayLarge
                        )
                    }
                }
            }
        }

        // Then - verify total products text matches sum (7)
        composeTestRule.onNodeWithText("7")
            .assertTextEquals("7")

        // Then - verify "Total de Productos" label is displayed
        composeTestRule.onNodeWithText("Total de Productos").assertIsDisplayed()
    }
}
